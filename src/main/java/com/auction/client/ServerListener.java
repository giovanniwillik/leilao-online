package com.auction.client;

import com.auction.common.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;

/**
 * Thread de escuta para mensagens que chegam do AuctionServer.
 * Garante que o cliente possa receber notificações e atualizações do servidor
 * sem bloquear a thread principal da aplicação.
 */
public class ServerListener implements Runnable {

    private ObjectInputStream inFromServer; // Stream de entrada do servidor
    private AuctionClient client;           // Referência para a instância do cliente principal

    /**
     * Construtor para o ServerListener.
     *
     * @param inFromServer O ObjectInputStream conectado ao servidor.
     * @param client A instância do AuctionClient que este listener irá servir.
     */
    public ServerListener(ObjectInputStream inFromServer, AuctionClient client) {
        this.inFromServer = inFromServer;
        this.client = client;
    }

    /**
     * O método run() contém o loop principal de escuta.
     * Ele lê objetos (mensagens) do servidor e os passa para o AuctionClient para tratamento.
     */
    @Override
    public void run() {
        try {
            while (true) { // Loop infinito para escutar continuamente
                Message message = (Message) inFromServer.readObject(); // Bloqueia até receber uma mensagem
                client.handleServerMessage(message); // Encaminha a mensagem para o cliente principal
            }
        } catch (EOFException e) {
            // Fim do stream - servidor fechou a conexão de forma limpa.
            client.getUi().displayError("O servidor fechou a conexão de forma limpa.");
        } catch (SocketException e) {
            // Conexão com o servidor foi perdida (e.g., servidor caiu, rede desconectada).
            client.getUi().displayError("Conexão com o servidor perdida: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            // Outros erros de I/O ou desserialização de objeto.
            client.getUi().displayError("Erro ao ler mensagem do servidor: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o estado de interrupção
            e.printStackTrace();
        } finally {
            client.closeConnections(); // Garante que todos os recursos do cliente sejam fechados.
        }
    }
}