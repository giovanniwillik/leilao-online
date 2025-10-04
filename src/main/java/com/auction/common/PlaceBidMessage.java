package com.auction.common;

/**
 * Mensagem enviada pelo cliente para fazer um lance em um leilão específico.
 */
public class PlaceBidMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private double bidAmount;
    private String bidderUsername; // Adicionado para facilitar o display no servidor/clientes

    public PlaceBidMessage(String senderId, String auctionId, double bidAmount, String bidderUsername) {
        super(MessageType.PLACE_BID, senderId);
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidderUsername = bidderUsername;
    }

    public String getAuctionId() { return auctionId; }
    public double getBidAmount() { return bidAmount; }
    public String getBidderUsername() { return bidderUsername; }

    @Override
    public String toString() {
        return "PlaceBidMessage{" +
               "auctionId='" + auctionId + '\'' +
               ", bidAmount=" + bidAmount +
               ", bidderUsername='" + bidderUsername + '\'' +
               "} " + super.toString();
    }
}