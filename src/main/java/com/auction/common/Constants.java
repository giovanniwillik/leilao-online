package com.auction.common;

public class Constants {

    /**
     * Porta TCP principal em que o servidor de leilões estará escutando.
     * Esta porta é usada para a comunicação cliente-servidor inicial e contínua.
     */
    public static final int SERVER_PORT = 12345;

    /**
     * Porta base para a comunicação Peer-to-Peer (P2P) entre clientes.
     * Cada cliente tentará usar uma porta sequencial a partir desta base
     * para escutar por conexões P2P de outros clientes.
     */
    public static final int P2P_BASE_PORT = 20000;

    /**
     * Intervalo em milissegundos para o envio de mensagens Keep-Alive.
     * Clientes enviarão periodicamente uma mensagem Keep-Alive para o servidor
     * para indicar que ainda estão ativos e conectados.
     */
    public static final long KEEP_ALIVE_INTERVAL_MS = 5000; // 5 segundos

    /**
     * Intervalo em milissegundos para o servidor verificar o término dos leilões.
     * O servidor executará esta verificação periodicamente para finalizar leilões
     * cujo tempo se esgotou e notificar os clientes.
     */
    public static final long AUCTION_END_CHECK_INTERVAL_MS = 1000; // 1 segundo
}