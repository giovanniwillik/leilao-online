package com.auction.client;

import com.auction.common.Message;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Lida com uma única conexão P2P com outro cliente.
 * Cada conexão P2P (seja iniciada ou aceita) terá uma instância de PeerConnectionHandler
 * rodando em sua própria thread.
 */
public class PeerConnectionHandler implements Runnable {

    private Socket peerSocket;
    private AuctionClient client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String peerId; // ID do cliente remoto com o qual esta conexão P2P foi estabelecida

    /**
     * Construtor para o PeerConnectionHandler.
     *
     * @param socket O socket da conexão P2P.
     * @param client A instância do AuctionClient principal.
     * @param initialPeerId O ID do peer, se já for conhecido (e.g., ao iniciar uma conexão).
     *                      Pode ser null se a conexão foi aceita, e o ID será descoberto na primeira mensagem.
     */
    public PeerConnectionHandler(Socket socket, AuctionClient client, String initialPeerId) {
        this.peerSocket = socket;
        this.client = client;
        this.peerId = initialPeerId; // Pode ser null
        try {
            // Ordem CRUCIAL: ObjectOutputStream ANTES de ObjectInputStream para evitar deadlocks.
            // Ambos os lados (initiator e accepter) devem seguir a mesma ordem.
            this.out = new ObjectOutputStream(peerSocket.getOutputStream());
            this.in = new ObjectInputStream(peerSocket.getInputStream());
        } catch (IOException e) {
            client.getUi().displayError("Erro ao criar streams P2P para o peer " + (peerId != null ? peerId : peerSocket.getInetAddress()) + ": " + e.getMessage());
            closeConnection();
        }
    }

    /**
     * O método run() contém a lógica principal da thread do PeerConnectionHandler.
     * Ele lê mensagens do peer e as encaminha para o AuctionClient principal para processamento.
     */
    @Override
    public void run() {
        try {
            // Se peerId ainda não foi definido (conexão recebida), a primeira mensagem deve conter o senderId
            if (peerId == null) {
                Message firstMessage = (Message) in.readObject();
                this.peerId = firstMessage.getSenderId(); // Descobre o ID do peer
                client.addActivePeerConnection(this.peerId, this);  // Adiciona ao mapa de conexões ativas
                
                client.getUi().displayMessage("Conexão P2P estabelecida com: " + peerId);
                client.handlePeerMessage(firstMessage); // Processa a primeira mensagem
            }

            // Loop principal para ler mensagens do peer
            while (peerSocket.isConnected()) {
                Message message = (Message) in.readObject();
                // Verifica se o senderId da mensagem corresponde ao peerId esperado
                if (!message.getSenderId().equals(peerId)) {
                    client.getUi().displayError("Recebida mensagem P2P de senderId inesperado ('" + message.getSenderId() + "') de peer '" + peerId + "'. Ignorando.");
                    continue; // Pode-se optar por fechar a conexão, mas por agora, apenas ignora.
                }
                client.handlePeerMessage(message); // Encaminha a mensagem para o cliente principal
            }

        } catch (EOFException e) {
            client.getUi().displayMessage("Peer " + (peerId != null ? peerId : peerSocket.getInetAddress()) + " desconectou (EOF). ");
        } catch (SocketException e) {
            client.getUi().displayMessage("Conexão P2P com " + (peerId != null ? peerId : peerSocket.getInetAddress()) + " perdida: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            client.getUi().displayError("Erro na comunicação P2P com " + (peerId != null ? peerId : peerSocket.getInetAddress()) + ": " + e.getMessage());
        } finally {
            closeConnection(); // Garante que os recursos sejam liberados
            // Remove esta conexão do mapa de conexões ativas no AuctionClient
            if (peerId != null) {
                client.getActivePeerConnections().remove(peerId);
            }
        }
    }

    /**
     * Envia uma mensagem para o peer associado a este handler.
     *
     * @param message A Message a ser enviada.
     */
    public void sendMessage(Message message) {
        try {
            out.reset();
            out.writeObject(message);
            out.flush(); // Garante que a mensagem seja enviada imediatamente
        } catch (IOException e) {
            client.getUi().displayError("Erro ao enviar mensagem P2P para o peer " + (peerId != null ? peerId : peerSocket.getInetAddress()) + ": " + e.getMessage());
            closeConnection(); // A conexão pode ter caído, então tenta fechá-la.
        }
    }

    /**
     * Fecha os streams e o socket desta conexão P2P.
     */
    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (peerSocket != null) peerSocket.close();
            client.getUi().displayMessage("Conexão P2P com peer " + (peerId != null ? peerId : peerSocket.getInetAddress()) + " fechada.");
        } catch (IOException e) {
            client.getUi().displayError("Erro ao fechar recursos P2P do peer " + (peerId != null ? peerId : peerSocket.getInetAddress()) + ": " + e.getMessage());
        }
    }
}