package com.auction.common;

import java.util.List;

/**
 * Mensagem de resposta do servidor com a lista de leilões ativos.
 */
public class AuctionListResponseMessage extends Message {
    private static final long serialVersionUID = 1L;
    private List<AuctionItem> activeAuctions;
    private List<AuctionItem> discontinuedAuctions;

    public AuctionListResponseMessage(String senderId, List<AuctionItem> activeAuctions, List<AuctionItem> discontinuedAuctions) {
        super(MessageType.AUCTION_LIST_RESPONSE, senderId);
        this.activeAuctions = activeAuctions;
        this.discontinuedAuctions = discontinuedAuctions;
    }

    public List<AuctionItem> getActiveAuctions() { return activeAuctions; }
    public List<AuctionItem> getDiscontinuedAuctions() { return discontinuedAuctions; }

    @Override
    public String toString() {
        return "AuctionListResponseMessage{" +
               "activeAuctions=" + (activeAuctions != null ? activeAuctions.size() : 0) + " items" +
               ", discontinuedAuctions=" + (discontinuedAuctions != null ? discontinuedAuctions.size() : 0) + " items" +
               "} " + super.toString();
    }
}