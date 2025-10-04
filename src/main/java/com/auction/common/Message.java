package com.auction.common;

import java.io.Serializable;

public abstract class Message implements Serializable {

    private static final long serialVersionUID = 1L;
    private MessageType type;
    private String senderId;
    private long timestamp;

    /**
     * Construtor para criar uma nova mensagem.
     *
     * @param type     Tipo da mensagem.
     * @param senderId ID do remetente.
     */
    public Message(MessageType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis(); // Define o timestamp no momento da criação
    }

    public MessageType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
               "type=" + type +
               ", senderId='" + senderId + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
