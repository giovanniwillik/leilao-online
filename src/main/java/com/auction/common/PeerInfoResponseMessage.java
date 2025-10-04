package com.auction.common;

/**
 * Mensagem de resposta do servidor contendo as informações P2P do usuário solicitado.
 */
public class PeerInfoResponseMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String targetUserId;
    private String targetIp;
    private int targetPort;

    public PeerInfoResponseMessage(String senderId, String targetUserId, String targetIp, int targetPort) {
        super(MessageType.PEER_INFO_RESPONSE, senderId);
        this.targetUserId = targetUserId;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
    }

    public String getTargetUserId() { return targetUserId; }
    public String getTargetIp() { return targetIp; }
    public int getTargetPort() { return targetPort; }

    @Override
    public String toString() {
        return "PeerInfoResponseMessage{" +
               "targetUserId='" + targetUserId + '\'' +
               ", targetIp='" + targetIp + '\'' +
               ", targetPort=" + targetPort +
               "} " + super.toString();
    }
}