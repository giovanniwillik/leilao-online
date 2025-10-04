package com.auction.common;

/**
 * Mensagem enviada pelo cliente para criar um novo leilão.
 * Encapsula os dados do AuctionItem a ser criado.
 */
public class CreateAuctionMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String itemName;
    private String itemDescription;
    private double startBid;
    private int durationSeconds; // Duração do leilão em segundos

    public CreateAuctionMessage(String senderId, String itemName, String itemDescription,
                                double startBid, int durationSeconds) {
        super(MessageType.CREATE_AUCTION, senderId);
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.startBid = startBid;
        this.durationSeconds = durationSeconds;
    }

    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public double getStartBid() { return startBid; }
    public int getDurationSeconds() { return durationSeconds; }

    @Override
    public String toString() {
        return "CreateAuctionMessage{" +
               "itemName='" + itemName + '\'' +
               ", startBid=" + startBid +
               ", durationSeconds=" + durationSeconds +
               "} " + super.toString();
    }
}