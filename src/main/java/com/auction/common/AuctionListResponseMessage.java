package com.auction.common;

import java.util.List;

/**
 * Mensagem de resposta do servidor com a lista de leilões ativos.
 */
public class AuctionListResponseMessage extends Message {
    private static final long serialVersionUID = 1L;
    private List<AuctionItem> auctions;

    public AuctionListResponseMessage(String senderId, List<AuctionItem> auctions) {
        super(MessageType.AUCTION_LIST_RESPONSE, senderId);
        this.auctions = auctions;
    }

    public List<AuctionItem> getAuctions() { return auctions; }

    @Override
    public String toString() {
        return "AuctionListResponseMessage{" +
               "auctions=" + (auctions != null ? auctions.size() : 0) + " items" +
               "} " + super.toString();
    }
}