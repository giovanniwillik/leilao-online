package com.auction.common;

/**
 * Mensagem simples enviada pelo cliente ao servidor para indicar que ainda está ativo.
 * Não precisa de dados adicionais além do tipo e ID do remetente.
 */
public class KeepAliveMessage extends Message {
    private static final long serialVersionUID = 1L;

    public KeepAliveMessage(String senderId) {
        super(MessageType.KEEP_ALIVE, senderId);
    }

    @Override
    public String toString() {
        return "KeepAliveMessage{} " + super.toString();
    }
}