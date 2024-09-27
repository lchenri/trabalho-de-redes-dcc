package org.redes;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class Client {
    private static JPanel messagePanel;
    private static PrintWriter out;
    private static String userName;

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Chat MSN/WLM Simulado");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);

        // Pergunta o nome do usuário
        userName = JOptionPane.showInputDialog(frame, "Digite seu nome de usuário:");

        // Substitui JTextArea por JPanel para exibir as mensagens
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(messagePanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        JTextField textField = new JTextField();
        frame.add(textField, BorderLayout.SOUTH);

        // Ação para enviar mensagem
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = textField.getText();
                // Envia a mensagem junto com o nome do usuário
                out.println(userName + ": " + message);
                addMessage(userName + ": " + message, true); // Adiciona a mensagem do próprio usuário
                textField.setText("");
            }
        });

        frame.setVisible(true);

        Socket socket = new Socket("localhost", 12345);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Envia o nome do usuário para o servidor
        out.println(userName);

        // Escuta e exibe as mensagens
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readLine();
                    if (message != null) {
                        // Verifica se a mensagem é de outro usuário
                        if (!message.startsWith(userName)) {
                            addMessage(message, false);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Método para adicionar mensagem ao painel com alinhamento apropriado
    private static void addMessage(String message, boolean isOwnMessage) {
        JLabel messageLabel = new JLabel(message);
        JPanel messageContainer = new JPanel();
        messageContainer.setLayout(new BorderLayout());
        int padding = 2;
        messageLabel.setBorder(new EmptyBorder(padding, padding,padding, padding));

        if (isOwnMessage) {
            messageContainer.add(messageLabel, BorderLayout.WEST); // Alinha à esquerda para o próprio usuário
            //messageLabel.setBackground(Color.LIGHT_GRAY);
        } else {
            messageContainer.add(messageLabel, BorderLayout.EAST); // Alinha à direita para outros usuários
            //messageLabel.setBackground(Color.CYAN);
        }

        messageLabel.setOpaque(true); // Para a cor de fundo funcionar
        messagePanel.add(messageContainer);
        messagePanel.add(Box.createVerticalStrut(2));
        messagePanel.revalidate(); // Atualiza o painel para exibir a nova mensagem
    }
}
