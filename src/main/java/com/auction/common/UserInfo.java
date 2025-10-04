package com.auction.common;

import java.io.Serializable;

public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;      // ID único do usuário (o mesmo UUID usado como senderId).
    private String username;    // Nome de usuário (display name).
    private String ipAddress;   // Endereço IP do cliente.
    private int p2pPort;        // Porta que o cliente está escutando para conexões P2P.

    /**
     * Construtor para criar um objeto UserInfo.
     *
     * @param userId    ID único do usuário.
     * @param username  Nome de usuário.
     * @param ipAddress Endereço IP do cliente.
     * @param p2pPort   Porta P2P do cliente.
     */
    public UserInfo(String userId, String username, String ipAddress, int p2pPort) {
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.p2pPort = p2pPort;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }
    public int getP2pPort() { return p2pPort; }

    @Override
    public String toString() {
        return "UserInfo{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", ipAddress='" + ipAddress + '\'' +
               ", p2pPort=" + p2pPort +
               '}';
    }

    // Método sobrescrito para comparar usuários logicamente, usando o campo userId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return userId.equals(userInfo.userId);
    }

    // Método sobrescrito para ser compatível com o equals(), pegando o hasCode do userId
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}