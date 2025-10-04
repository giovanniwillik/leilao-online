package com.auction.common;

/**
 * Mensagem P2P enviada diretamente entre clientes.
 * Pode ser uma mensagem de texto geral ou relacionada a um leilão específico.
 */
public class DirectMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String receiverId; // ID do cliente receptor
    private String content;    // Conteúdo da mensagem
    private String relatedAuctionId; // Opcional: ID do leilão ao qual a mensagem se refere

    public DirectMessage(String senderId, String receiverId, String content, String relatedAuctionId) {
        super(MessageType.DIRECT_MESSAGE, senderId);
        this.receiverId = receiverId;
        this.content = content;
        this.relatedAuctionId = relatedAuctionId;
    }

    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public String getRelatedAuctionId() { return relatedAuctionId; }

    @Override
    public String toString() {
        return "DirectMessage{" +
               "receiverId='" + receiverId + '\'' +
               ", content='" + content + '\'' +
               ", relatedAuctionId='" + (relatedAuctionId != null ? relatedAuctionId : "N/A") + '\'' +
               "} " + super.toString();
    }
}