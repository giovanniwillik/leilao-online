package com.auction.common;

/**
 * Mensagem enviada pelo cliente ao servidor para solicitar as informações P2P de outro usuário.
 */
public class PeerInfoRequestMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String targetUserId; // ID do usuário cujas informações P2P são solicitadas

    public PeerInfoRequestMessage(String senderId, String targetUserId) {
        super(MessageType.PEER_INFO_REQUEST, senderId);
        this.targetUserId = targetUserId;
    }

    public String getTargetUserId() { return targetUserId; }

    @Override
    public String toString() {
        return "PeerInfoRequestMessage{" +
               "targetUserId='" + targetUserId + '\'' +
               "} " + super.toString();
    }
}