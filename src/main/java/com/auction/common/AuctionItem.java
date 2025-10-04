package com.auction.common;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Representa um item que está em leilão na aplicação.
 * Contém todos os detalhes do item, seu estado atual e informações sobre o leilão.
 */
public class AuctionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        ACTIVE,     // O leilão está ativo e aceitando lances.
        ENDED,      // O leilão foi encerrado.
        CANCELLED   // O leilão foi cancelado antes do término normal.
    }

    private String id;                      // ID único do leilão (gerado automaticamente).
    private String name;                    // Nome do item leiloado.
    private String description;             // Descrição detalhada do item.
    private double currentBid;              // O valor do lance mais alto atual.
    private String highestBidderId;         // ID do cliente que deu o lance mais alto.
    private String highestBidderUsername;   // Nome de usuário do cliente que deu o lance mais alto.
    private long endTimeMillis;             // Carimbo de data/hora em milissegundos para o fim do leilão.
    private String sellerId;                // ID do cliente que criou/vendeu o item.
    private String sellerUsername;          // Nome de usuário do vendedor.
    private Status status;                  // Status atual do leilão.

    /**
     * Construtor para criar um novo AuctionItem.
     *
     * @param name           Nome do item.
     * @param description    Descrição do item.
     * @param startBid       Lance inicial para o item.
     * @param durationSeconds Duração do leilão em segundos a partir da criação.
     * @param sellerId       ID do vendedor.
     * @param sellerUsername Nome de usuário do vendedor.
     */
    public AuctionItem(String name, String description, double startBid, int durationSeconds,
                       String sellerId, String sellerUsername) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.currentBid = startBid;
        this.highestBidderId = null;
        this.highestBidderUsername = null;
        this.endTimeMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(durationSeconds);
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        this.status = Status.ACTIVE;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCurrentBid() { return currentBid; }
    public String getHighestBidderId() { return highestBidderId; }
    public String getHighestBidderUsername() { return highestBidderUsername; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public String getSellerId() { return sellerId; }
    public String getSellerUsername() { return sellerUsername; }
    public Status getStatus() { return status; }

    private void setCurrentBid(double currentBid) { this.currentBid = currentBid; }
    private void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }
    private void setHighestBidderUsername(String highestBidderUsername) { this.highestBidderUsername = highestBidderUsername; }
    public void setStatus(Status status) { this.status = status; }

    /**
     * Tenta registrar um novo lance para este item.
     *
     * @param bidderId         ID do cliente que está dando o lance.
     * @param bidderUsername   Nome de usuário do cliente que está dando o lance.
     * @param bidAmount        O valor do lance proposto.
     * @return true se o lance for aceito (maior que o lance atual), false caso contrário.
     */
    public synchronized boolean placeBid(String bidderId, String bidderUsername, double bidAmount) {
        if (status != Status.ACTIVE) {
            System.out.println("Leilão " + id + " não está ativo.");
            return false;
        }
        if (isEnded()) {
            System.out.println("Leilão " + id + " já terminou.");
            this.status = Status.ENDED; // Atualiza o status caso não tenha sido feito pelo scheduler
            return false;
        }
        if (bidAmount > currentBid) {
            setCurrentBid(bidAmount);
            setHighestBidderId(bidderId);
            setHighestBidderUsername(bidderUsername);
            return true;
        }
        return false;
    }

    /**
     * Verifica se o leilão já terminou com base no tempo.
     *
     * @return true se o tempo final já foi atingido, false caso contrário.
     */
    public boolean isEnded() {
        return System.currentTimeMillis() >= endTimeMillis;
    }

    /**
     * Retorna o tempo restante para o fim do leilão em milissegundos.
     *
     * @return Tempo restante em milissegundos, 0 ou negativo se já terminou.
     */
    public long getRemainingTime() {
        return Math.max(0, endTimeMillis - System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "AuctionItem{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", currentBid=" + currentBid +
               ", highestBidder='" + (highestBidderUsername != null ? highestBidderUsername : "N/A") + '\'' +
               ", endTime=" + (getRemainingTime() > 0 ? TimeUnit.MILLISECONDS.toSeconds(getRemainingTime()) + "s restantes" : "ENCERRADO") +
               ", status=" + status +
               '}';
    }
}