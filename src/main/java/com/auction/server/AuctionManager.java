package com.auction.server;

import com.auction.common.AuctionItem;
import com.auction.common.AuctionUpdateMessage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerencia a criação, atualização e encerramento de leilões na aplicação.
 * Responsável por manter o estado de todos os AuctionItems ativos e processar lances.
 */
public class AuctionManager {

    // Utiliza ConcurrentHashMap para garantir thread-safety no acesso concorrente aos leilões.
    // A chave é o ID do leilão, o valor é o AuctionItem.
    private final Map<String, AuctionItem> activeAuctions;

    private final Map<String, AuctionItem> discontinuedAuctions;

    // Referência ao servidor principal para poder broadcastar atualizações aos clientes.
    private AuctionServer server;

    /**
     * Construtor para o AuctionManager.
     *
     * @param server A instância do AuctionServer para comunicação de volta aos clientes.
     */
    public AuctionManager(AuctionServer server) {
        this.server = server;
        this.activeAuctions = new ConcurrentHashMap<>();
        addInitialAuctions();
        this.discontinuedAuctions = new ConcurrentHashMap<>();
    }

    /**
     * Adiciona alguns leilões de exemplo ao iniciar o servidor.
     * Pode ser removido ou substituído por carregamento de um arquivo/DB em uma versão final.
     */
    private void addInitialAuctions() {
        // Exemplo: 1 minuto (60 segundos) de duração
        // AuctionItem item1 = new AuctionItem("Quadro Abstrato", "Obra de arte moderna para sua sala.", 50.0, 60, "server", "Admin");
        // AuctionItem item2 = new AuctionItem("Relógio de Bolso Antigo", "Peça rara do século XIX.", 120.0, 90, "server", "Admin");
        // AuctionItem item3 = new AuctionItem("Livro Raro", "Primeira edição de clássico da literatura.", 30.0, 45, "server", "Admin");

        // activeAuctions.put(item1.getId(), item1);
        // activeAuctions.put(item2.getId(), item2);
        // activeAuctions.put(item3.getId(), item3);

        // System.out.println("Leilões iniciais adicionados:");
        // activeAuctions.values().forEach(System.out::println);
    }

    /**
     * Adiciona um novo leilão à lista de leilões ativos.
     *
     * @param item O AuctionItem a ser adicionado.
     */
    public void addAuction(AuctionItem item) {
        activeAuctions.put(item.getId(), item);
        System.out.println("Novo leilão criado: " + item.getName() + " (ID: " + item.getId() + ")");
        // Notifica todos os clientes sobre o novo leilão
        server.broadcast(new AuctionUpdateMessage("server", item, "Novo leilão adicionado!"));
    }

    /**
     * Tenta processar um lance para um leilão específico.
     *
     * @param auctionId O ID do leilão.
     * @param bidderId O ID do cliente que está dando o lance.
     * @param bidAmount O valor do lance.
     * @return true se o lance foi aceito e o leilão atualizado, false caso contrário.
     */
    public boolean placeBid(String auctionId, String bidderId, double bidAmount) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) {
            System.out.println("Tentativa de lance em leilão inexistente: " + auctionId);
            return false; // Leilão não encontrado
        }

        // Recupera o nome de usuário do licitante para exibir nos clientes
        String bidderUsername = server.getActiveUsersInfo().get(bidderId) != null ?
                                server.getActiveUsersInfo().get(bidderId).getUsername() : bidderId;

        // O método placeBid() dentro de AuctionItem já é synchronized e tem validações
        boolean bidAccepted = auction.placeBid(bidderId, bidderUsername, bidAmount);

        if (bidAccepted) {
            System.out.println("Lance aceito para " + auction.getName() + ": " + bidAmount + " por " + bidderUsername);
            // O servidor deve broadcastar a atualização do leilão para todos os clientes
            server.broadcast(new AuctionUpdateMessage("server", auction, "Novo lance para " + auction.getName() + ": " + bidAmount + " por " + bidderUsername));
        } else {
            System.out.println("Lance recusado para " + auction.getName() + ": " + bidAmount + " (lance atual: " + auction.getCurrentBid() + ")");
        }
        return bidAccepted;
    }

    /**
     * Retorna um AuctionItem pelo seu ID.
     *
     * @param auctionId O ID do leilão.
     * @return O AuctionItem correspondente, ou null se não encontrado.
     */
    public AuctionItem getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Retorna uma lista de todos os leilões ativos, ordenada por tempo restante.
     *
     * @return Uma List de AuctionItem.
     */
    public List<AuctionItem> getLiveAuctions() {
        return activeAuctions.values().stream()
                .sorted(Comparator.comparingLong(AuctionItem::getEndTimeMillis)) // Ordena pelo tempo de término
                .collect(Collectors.toList());
    }

    /**
     * Retorna uma lista de todos os leilões descontinuados (encerrados), ordenada por tempo de término.
     *
     * @return Uma List de AuctionItem.
     */
    public List<AuctionItem> getDiscontinuedAuctions() {
        return discontinuedAuctions.values().stream()
                .sorted(Comparator.comparingLong(AuctionItem::getEndTimeMillis)) // Ordena pelo tempo de término
                .collect(Collectors.toList());
    }

    /**
     * Verifica e encerra leilões que atingiram seu tempo final.
     * Este método é projetado para ser executado periodicamente por um scheduler.
     */
    public void checkAuctionEndTimes() {
        // Itera sobre uma cópia dos valores para evitar ConcurrentModificationException
        // se um leilão for removido enquanto estamos iterando.
        for (AuctionItem auction : activeAuctions.values()) {
            if (auction.getStatus() == AuctionItem.Status.ACTIVE && auction.isEnded()) {
                auction.setStatus(AuctionItem.Status.ENDED);
                
                String statusMessage;
                // Verifica se houve algum lance válido (ou seja, se o highestBidderUsername foi definido)
                if (auction.getHighestBidderUsername() != null) {
                    statusMessage = "Vencedor: " + auction.getHighestBidderUsername() +
                                    " com lance de " + String.format("%.2f", auction.getCurrentBid());
                    System.out.println("Leilão ENCERRADO: " + auction.getName() + " (ID: " + auction.getId() + ")");
                    System.out.println(statusMessage);
                } else {
                    // Ninguém deu um lance após o lance inicial
                    statusMessage = "Item não foi vendido (sem lances). Lance inicial: " + String.format("%.2f", auction.getStartBid());
                    System.out.println("Leilão ENCERRADO: " + auction.getName() + " (ID: " + auction.getId() + ")");
                    System.out.println(statusMessage);
                }
                
                // Atualiza o leilão no mapa (não removemos para manter o histórico)
                activeAuctions.put(auction.getId(), auction);

                // Move o leilão para a lista de descontinuados
                discontinuedAuctions.put(auction.getId(), auction);
                activeAuctions.remove(auction.getId());

                // Notifica todos os clientes que o leilão terminou
                server.broadcast(new AuctionUpdateMessage("server", auction, "Leilão encerrado! " + auction.getName() + " (ID: " + auction.getId() + "). " + statusMessage));
            }
        }
        
    }
}