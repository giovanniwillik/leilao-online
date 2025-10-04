package com.auction.common;

/**
 * Mensagem enviada pelo servidor para notificar os clientes sobre o status de um usuário (online/offline).
 */
public class UserStatusUpdateMessage extends Message {
    private static final long serialVersionUID = 1L;
    private UserInfo user;
    private boolean isOnline; // true se o usuário ficou online, false se ficou offline

    public UserStatusUpdateMessage(String senderId, UserInfo user, boolean isOnline) {
        super(MessageType.USER_STATUS_UPDATE, senderId);
        this.user = user;
        this.isOnline = isOnline;
    }

    public UserInfo getUser() { return user; }
    public boolean isOnline() { return isOnline; }

    @Override
    public String toString() {
        return "UserStatusUpdateMessage{" +
               "user=" + user.getUsername() +
               ", isOnline=" + isOnline +
               "} " + super.toString();
    }
}