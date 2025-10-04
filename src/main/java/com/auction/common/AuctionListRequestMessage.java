package com.auction.common;

/**
 * Mensagem enviada pelo cliente para solicitar uma lista atualizada dos leilões ativos.
 */
public class AuctionListRequestMessage extends Message {
    private static final long serialVersionUID = 1L;

    public AuctionListRequestMessage(String senderId) {
        super(MessageType.AUCTION_LIST_REQUEST, senderId);
    }

    @Override
    public String toString() {
        return "AuctionListRequestMessage{} " + super.toString();
    }
}