package com.auction.server;

import com.auction.common.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Lida com a comunicação de um cliente individual no servidor de leilões.
 * Cada cliente conectado terá uma instância de ClientHandler rodando em sua própria thread.
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private AuctionServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String userId;
    private String username;

    /**
     * Construtor para o ClientHandler.
     *
     * @param socket O socket de comunicação com o cliente.
     * @param server A instância do AuctionServer para interagir com a lógica central.
     */
    public ClientHandler(Socket socket, AuctionServer server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            // A ordem de criação dos ObjectInputStream e ObjectOutputStream é CRUCIAL!
            // O ObjectOutputStream DEVE ser criado primeiro no servidor, e também no cliente.
            // Se a ordem for invertida, pode ocorrer um deadlock pois ambos os lados esperariam
            // o cabeçalho do stream do outro para continuar.
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Erro ao criar streams para o cliente " + clientSocket.getInetAddress() + ": " + e.getMessage());
            closeConnection();
        }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Socket getClientSocket() { return clientSocket; } // Permite ao servidor obter IP do cliente

    /**
     * O método run() contém a lógica principal da thread do ClientHandler.
     * Ele lê mensagens do cliente e as encaminha para o AuctionServer para processamento.
     */
    @Override
    public void run() {
        try {
            // A primeira mensagem de um cliente deve ser um LOGIN
            Message firstMessage = (Message) in.readObject();
            if (firstMessage.getType() == MessageType.LOGIN) {
                LoginMessage loginMsg = (LoginMessage) firstMessage;
                // Configura o ID e username do cliente neste handler
                this.setUserId(loginMsg.getSenderId());
                this.setUsername(loginMsg.getUsername());
                server.addClient(userId, this, clientSocket.getInetAddress().getHostAddress(), loginMsg.getP2pPort());
                // Passa a mensagem de login para o servidor lidar, incluindo o registro do cliente
                server.handleMessage(loginMsg, this);
            } else {
                System.out.println("Cliente " + clientSocket.getInetAddress() + " enviou " + firstMessage.getType() + " antes de LOGIN. Fechando conexão.");
                // Envia uma resposta de falha e fecha a conexão
                sendMessage(new LoginResponseMessage("server", false, "Por favor, faça login primeiro.", null, null));
                return; // Encerra a thread sem entrar no loop de leitura
            }

            // Loop principal para ler mensagens do cliente
            while (clientSocket.isConnected()) {
                Message message = (Message) in.readObject();
                // Encaminha a mensagem para o servidor principal processar
                server.handleMessage(message, this);
            }

        } catch (EOFException e) {
            // Cliente fechou a conexão de forma limpa
            System.out.println("Cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + " desconectou.");
        } catch (SocketException e) {
            // Conexão perdida (e.g., cliente desligou, rede caiu)
            System.out.println("Conexão perdida com o cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + ": " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            // Outros erros de I/O ou desserialização
            System.err.println("Erro na comunicação com o cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + ": " + e.getMessage());
        } finally {
            closeConnection(); // Garante que os recursos sejam liberados
        }
    }

    /**
     * Envia uma mensagem para o cliente associado a este handler.
     *
     * @param message A Message a ser enviada.
     */
    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush(); // Garante que a mensagem seja enviada imediatamente
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem para o cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + ": " + e.getMessage());
            closeConnection(); // A conexão pode ter caído, então tenta fechá-la.
        }
    }

    /**
     * Fecha os streams e o socket deste handler, e notifica o servidor.
     */
    public void closeConnection() {
        try {
            if (userId != null) {
                server.removeClient(userId); // Notifica o servidor que este cliente se desconectou
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Conexão com cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + " fechada.");
        } catch (IOException e) {
            System.err.println("Erro ao fechar recursos do cliente " + (userId != null ? userId : clientSocket.getInetAddress()) + ": " + e.getMessage());
        }
    }
}