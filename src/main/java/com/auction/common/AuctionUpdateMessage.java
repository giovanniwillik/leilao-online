package com.auction.common;

/**
 * Mensagem enviada pelo servidor para notificar os clientes sobre uma atualização em um leilão.
 * Pode ser um novo lance, o término do leilão, etc.
 */
public class AuctionUpdateMessage extends Message {
    private static final long serialVersionUID = 1L;
    private AuctionItem updatedAuctionItem;
    private String updateDescription; // Uma breve descrição do que mudou (e.g., "Novo lance", "Leilão encerrado")

    public AuctionUpdateMessage(String senderId, AuctionItem updatedAuctionItem, String updateDescription) {
        super(MessageType.AUCTION_UPDATE, senderId);
        this.updatedAuctionItem = updatedAuctionItem;
        this.updateDescription = updateDescription;
    }

    public AuctionItem getUpdatedAuctionItem() { return updatedAuctionItem; }
    public String getUpdateDescription() { return updateDescription; }

    @Override
    public String toString() {
        return "AuctionUpdateMessage{" +
               "updatedAuctionItem=" + updatedAuctionItem.getId() +
               ", updateDescription='" + updateDescription + '\'' +
               "} " + super.toString();
    }
}
