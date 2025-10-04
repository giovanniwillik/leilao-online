package com.auction.common;

/**
 * Mensagem enviada pelo cliente ao servidor para iniciar uma sessão.
 * Contém o nome de usuário escolhido e a porta que o cliente usará para comunicação P2P.
 */
public class LoginMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String username;
    private int p2pPort; // Porta que o cliente usará para comunicação P2P

    public LoginMessage(String senderId, String username, int p2pPort) {
        super(MessageType.LOGIN, senderId);
        this.username = username;
        this.p2pPort = p2pPort;
    }

    public String getUsername() { return username; }
    public int getP2pPort() { return p2pPort; }

    @Override
    public String toString() {
        return "LoginMessage{" +
               "username='" + username + '\'' +
               ", p2pPort=" + p2pPort +
               "} " + super.toString();
    }
}