package org.redes;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.Base64;

public class Server {
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<Socket, String> userNames = new HashMap<>(); // Mapeia socket para nome de usuário

    public static void main(String[] args) throws Exception {
        System.out.println("Servidor WebSocket iniciado...");
        ServerSocket serverSocket = new ServerSocket(12345);

        while (true) {
            new ClientHandler(serverSocket.accept()).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private InputStream in;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = socket.getInputStream();
                out = new PrintWriter(socket.getOutputStream(), true);

                // Realizar handshake WebSocket
// Realizar handshake WebSocket
                StringBuilder request = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    request.append(line).append("\r\n");
                }
                System.out.println("Requisição recebida:\n" + request.toString());

                // Extrair a chave do WebSocket da requisição
                String webSocketKey = request.toString().split("Sec-WebSocket-Key: ")[1].split("\r\n")[0];
                String webSocketAccept = generateWebSocketAcceptKey(webSocketKey);

                // Enviar resposta de upgrade
                String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + webSocketAccept + "\r\n\r\n";
                out.write(response);
                out.flush();
                System.out.println("Handshake WebSocket concluído.");

                // Recebe e armazena o nome do usuário (via mensagem WebSocket)
                String message = readWebSocketMessage(in);
                userName = message;

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                synchronized (userNames) {
                    userNames.put(socket, userName); // Associa o nome ao socket
                }

                // Anuncia a entrada do novo usuário para todos
                sendToAll("Servidor: " + userName + " entrou no chat!");

                // Lê e distribui as mensagens para todos
                while ((message = readWebSocketMessage(in)) != null) {
                    System.out.println("Recebido de " + userName + ": " + message);
                    sendToAll(message);
                }
            } catch (IOException e) {
                System.out.println("Erro na conexão com o cliente: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                synchronized (userNames) {
                    sendToAll("Servidor: " + userName + " saiu do chat.");
                    userNames.remove(socket);
                }
            }
        }

        // Função para ler as mensagens WebSocket
        private String readWebSocketMessage(InputStream in) throws IOException {
            // Lê o primeiro byte do quadro
            int firstByte = in.read();
            if (firstByte == -1) {
                return null; // Conexão fechada
            }

            // Lê o segundo byte do quadro
            int secondByte = in.read();
            int length = secondByte & 127; // 7 bits menos significativos

            // Determina o comprimento da mensagem
            if (length == 126) {
                length = (in.read() << 8) | in.read(); // Lê 2 bytes adicionais para o comprimento
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | in.read(); // Lê 8 bytes adicionais para o comprimento
                }
            }

            // Lê a chave da máscara
            byte[] maskingKey = new byte[4];
            in.read(maskingKey);

            // Lê os dados da mensagem
            byte[] messageBytes = new byte[length];
            in.read(messageBytes);

            // Desmascara a mensagem
            byte[] decodedBytes = new byte[length];
            for (int i = 0; i < length; i++) {
                decodedBytes[i] = (byte) (messageBytes[i] ^ maskingKey[i % 4]);
            }

            return new String(decodedBytes, StandardCharsets.UTF_8); // Use a codificação UTF-8
        }


        private void sendToAll(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }

        // Gera a chave 'Sec-WebSocket-Accept' para completar o handshake
        private String generateWebSocketAcceptKey(String key) throws Exception {
            String magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            String combined = key + magicString;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(sha1.digest(combined.getBytes("UTF-8")));
        }
    }
}
