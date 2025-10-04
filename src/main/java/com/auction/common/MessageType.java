package com.auction.common;

/**
 * Define os tipos de mensagens que podem ser trocadas na aplicação de leilão online.
 * Isso ajuda a categorizar e rotear as mensagens de forma apropriada
 * tanto no servidor quanto nos clientes.
 */
public enum MessageType {
    // --- Mensagens Cliente -> Servidor ---
    /**
     * Enviada pelo cliente para autenticação e registro no servidor.
     * Contém informações de login e a porta P2P do cliente.
     */
    LOGIN,

    /**
     * Enviada pelo cliente para indicar que deseja desconectar-se do servidor.
     */
    LOGOUT,

    /**
     * Enviada pelo cliente para solicitar uma lista atualizada de todos os leilões ativos.
     */
    AUCTION_LIST_REQUEST,

    /**
     * Enviada pelo cliente para registrar um lance em um item de leilão específico.
     */
    PLACE_BID,

    /**
     * Enviada pelo cliente para criar um novo leilão no sistema.
     */
    CREATE_AUCTION,

    /**
     * Enviada periodicamente pelo cliente para indicar ao servidor que ele ainda está ativo.
     */
    KEEP_ALIVE,

    /**
     * Enviada pelo cliente para solicitar as informações de IP e porta P2P de outro cliente.
     * Usada como parte do processo de estabelecimento de comunicação P2P.
     */
    PEER_INFO_REQUEST,

    // --- Mensagens Servidor -> Cliente ---
    /**
     * Resposta do servidor a uma tentativa de LOGIN do cliente.
     * Informa se o login foi bem-sucedido e fornece dados iniciais como leilões e usuários online.
     */
    LOGIN_RESPONSE,

    /**
     * Resposta do servidor a uma solicitação AUCTION_LIST_REQUEST.
     * Contém a lista atual de leilões ativos.
     */
    AUCTION_LIST_RESPONSE,

    /**
     * Enviada pelo servidor para notificar os clientes sobre uma atualização em um leilão.
     * Ex: novo lance, leilão finalizado, etc.
     */
    AUCTION_UPDATE,

    /**
     * Enviada pelo servidor para notificar os clientes sobre mudanças no status de usuários.
     * Ex: um usuário ficou online ou offline.
     */
    USER_STATUS_UPDATE,

    /**
     * Resposta do servidor a uma solicitação PEER_INFO_REQUEST.
     * Contém as informações de IP e porta P2P do cliente solicitado, se encontrado.
     */
    PEER_INFO_RESPONSE,

    // --- Mensagens Cliente <-> Cliente (P2P) ---
    /**
     * Enviada diretamente entre clientes para comunicação P2P.
     * Pode ser uma mensagem de texto, negociação, etc.
     */
    DIRECT_MESSAGE
}

