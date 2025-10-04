package com.auction.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Thread de escuta para conexões P2P de outros clientes.
 * Cada AuctionClient terá um PeerListener rodando para aceitar conexões diretas.
 */
public class PeerListener implements Runnable {

    private ServerSocket p2pServerSocket; // ServerSocket do cliente para aceitar conexões P2P
    private AuctionClient client;           // Referência para a instância do cliente principal

    /**
     * Construtor para o PeerListener.
     *
     * @param p2pServerSocket O ServerSocket que o cliente abriu para P2P.
     * @param client A instância do AuctionClient que este listener irá servir.
     */
    public PeerListener(ServerSocket p2pServerSocket, AuctionClient client) {
        this.p2pServerSocket = p2pServerSocket;
        this.client = client;
    }

    /**
     * O método run() contém o loop principal de escuta.
     * Ele aguarda por novas conexões P2P de outros clientes e cria um PeerConnectionHandler para cada uma.
     */
    @Override
    public void run() {
        try {
            // Loop para aceitar continuamente novas conexões P2P enquanto o socket estiver aberto.
            while (!p2pServerSocket.isClosed()) {
                Socket peerSocket = p2pServerSocket.accept(); // Bloqueia até uma nova conexão P2P chegar
                client.getUi().displayMessage("Nova conexão P2P recebida de: " + peerSocket.getInetAddress().getHostAddress());

                // Cria um novo PeerConnectionHandler para gerenciar a comunicação com este peer.
                // O peerId é null inicialmente, pois será descoberto na primeira mensagem recebida.
                new Thread(new PeerConnectionHandler(peerSocket, client, null)).start();
            }
        } catch (SocketException e) {
            // Geralmente ocorre quando p2pServerSocket.close() é chamado em outra thread,
            // ou a rede é desativada. É um encerramento esperado.
            if (!p2pServerSocket.isClosed()) {
                client.getUi().displayError("Erro inesperado no servidor P2P do cliente: " + e.getMessage());
            } else {
                client.getUi().displayMessage("Servidor P2P do cliente encerrado.");
            }
        } catch (IOException e) {
            client.getUi().displayError("Erro ao aceitar conexão P2P: " + e.getMessage());
        } finally {
            // Garante que o ServerSocket P2P seja fechado se ainda estiver aberto.
            try {
                if (p2pServerSocket != null && !p2pServerSocket.isClosed()) {
                    p2pServerSocket.close();
                }
            } catch (IOException e) {
                client.getUi().displayError("Erro ao fechar p2pServerSocket: " + e.getMessage());
            }
        }
    }
}
