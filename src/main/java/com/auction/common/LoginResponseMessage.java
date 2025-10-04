package com.auction.common;

import java.util.List;

/**
 * Mensagem de resposta do servidor ao cliente após uma tentativa de login.
 * Informa sobre o sucesso/falha do login e fornece dados iniciais ao cliente.
 */
public class LoginResponseMessage extends Message {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private List<AuctionItem> initialAuctions; // Leilões ativos no momento do login
    private List<UserInfo> activeUsers; // Usuários online no momento do login

    public LoginResponseMessage(String senderId, boolean success, String message,
                                List<AuctionItem> initialAuctions, List<UserInfo> activeUsers) {
        super(MessageType.LOGIN_RESPONSE, senderId);
        this.success = success;
        this.message = message;
        this.initialAuctions = initialAuctions;
        this.activeUsers = activeUsers;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<AuctionItem> getInitialAuctions() { return initialAuctions; }
    public List<UserInfo> getActiveUsers() { return activeUsers; }

    @Override
    public String toString() {
        return "LoginResponseMessage{" +
               "success=" + success +
               ", message='" + message + '\'' +
               ", initialAuctions=" + (initialAuctions != null ? initialAuctions.size() : 0) + " items" +
               ", activeUsers=" + (activeUsers != null ? activeUsers.size() : 0) + " users" +
               "} " + super.toString();
    }
}