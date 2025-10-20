package com.auction.client;

import com.auction.common.AuctionItem;
import com.auction.common.UserInfo;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

/**
 * Interface de Usuário para o cliente do sistema de leilões (baseado em
 * console).
 * Permite que o usuário interaja com a aplicação, enviando comandos e
 * visualizando o estado.
 */
public class ClientUI {
    private AuctionClient client;
    private Scanner scanner;
    private boolean loggedIn = false;
    private ScheduledExecutorService uiScheduler; // Para agendar atualizações periódicas da UI
    private List<AuctionItem> recentlyDiscontinuedAuctions = new ArrayList<>();

    /**
     * Construtor para ClientUI.
     *
     * @param client A instância do AuctionClient que esta UI irá controlar.
     */
    public ClientUI(AuctionClient client) {
        this.client = client;
        this.scanner = new Scanner(System.in);
        this.uiScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Inicia o loop principal da interface de usuário, aguardando comandos do
     * usuário.
     */
    public void start() {
        // // Agendar uma atualização periódica da UI para exibir leilões e usuários
        // online
        // uiScheduler.scheduleAtFixedRate(this::refreshUI, 0, 5, TimeUnit.SECONDS);

        displayLoginPrompt();
        String input;
        while (true) {
            System.out.print("> "); // Prompt para o usuário
            input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                client.closeConnections();
                break;
            }
            if (!loggedIn) {
                // Se não estiver logado, só aceita o comando de login
                handleLoginInput(input);
            } else {
                // Se estiver logado, aceita outros comandos
                handleLoggedInInput(input);
            }
        }
        scanner.close();
        uiScheduler.shutdownNow(); // Desliga o scheduler da UI
    }

    /**
     * Exibe o prompt de login para o usuário.
     */
    private void displayLoginPrompt() {
        System.out.println("---------------------------------------------------");
        System.out.println("Bem-vindo ao Sistema de Leilões! \n");
        System.out.println("Para fazer login, digite: login <seu_nome_de_usuario>");
        System.out.println("Exemplo: login JoaoSilva");
        System.out.println("---------------------------------------------------");
        System.out.println("Após o login, você pode usar os seguintes comandos:");
        System.out.println("lsauctions                  - Lista todos os leilões ativos e encerrados.");
        System.out.println("bid <auction_id> <valor>     - Dá um lance em um leilão específico.");
        System.out.println(
                "createauction '<nome>' '<descricao>' <lance_inicial> <duracao_segundos> - Cria um novo leilão.");
        System.out.println("lsonline                    - Lista todos os usuários online.");
        System.out.println("chat <user_id> <mensagem>   - Envia uma mensagem P2P direta para outro usuário.");
        System.out.println("help                        - Exibe a lista de comandos.");
        System.out.println("exit                        - Sai da aplicação.");
        System.out.println("---------------------------------------------------");
    }

    /**
     * Lida com a entrada do usuário antes do login.
     * 
     * @param input A linha de comando do usuário.
     */
    private void handleLoginInput(String input) {
        if (input.toLowerCase().startsWith("login ")) {
            String username = input.substring("login ".length()).trim();
            if (username.isEmpty()) {
                displayError("Nome de usuário não pode ser vazio.");
                return;
            }
            try {
                client.login(username);
                // Assume que o login será bem-sucedido. A resposta real virá do servidor.
                // O estado `loggedIn` será atualizado na handleServerMessage do AuctionClient.
            } catch (Exception e) {
                displayError("Erro ao tentar login: " + e.getMessage());
            }
        } else {
            displayError("Você deve fazer login primeiro. Digite 'login <seu_nome_de_usuario>'.");
        }
    }

    /**
     * Lida com a entrada do usuário após o login.
     * 
     * @param input A linha de comando do usuário.
     */
    private void handleLoggedInInput(String input) {
        String[] parts = input.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "lsauctions":
                client.requestAuctionList();
                break;
            case "bid":
                if (parts.length < 2) {
                    displayError("Uso: bid <auction_id> <valor>");
                    return;
                }
                String[] bidArgs = parts[1].split(" ");
                if (bidArgs.length < 2) {
                    displayError("Uso: bid <auction_id> <valor>");
                    return;
                }
                try {
                    String auctionId = bidArgs[0];
                    double bidAmount = parseBidAmount(bidArgs[1]);
                    client.placeBid(auctionId, bidAmount);
                } catch (NumberFormatException e) {
                    displayError("Valor do lance deve ser um número válido: " + e.getMessage());
                }
                break;
            case "createauction":
                // Exemplo: createauction "Nome do Item" "Descricao do item" 10.50 60
                if (parts.length < 2) {
                    displayError("Uso: createauction '<nome>' '<descricao>' <lance_inicial> <duracao_segundos>");
                    displayError("Exemplo: createauction 'Laptop Gaming' 'Novo, potente' 1200.00 300");
                    return;
                }
                String args = parts[1].trim();
                // Regex para parsing de strings entre aspas e números
                java.util.regex.Matcher m = Pattern.compile("'([^']*)'|(\\S+)").matcher(args);
                List<String> parsedArgs = new java.util.ArrayList<>();
                while (m.find()) {
                    if (m.group(1) != null) {
                        parsedArgs.add(m.group(1)); // Texto entre aspas
                    } else {
                        parsedArgs.add(m.group(2)); // Texto sem aspas
                    }
                }

                if (parsedArgs.size() != 4) {
                    displayError(
                            "Argumentos inválidos. Uso: createauction '<nome>' '<descricao>' <lance_inicial> <duracao_segundos>");
                    return;
                }

                try {
                    String itemName = parsedArgs.get(0);
                    String itemDescription = parsedArgs.get(1);
                    double startBid = parseBidAmount(parsedArgs.get(2));
                    int durationSeconds = parseAuctionDuration(parsedArgs.get(3));
                    client.createAuction(itemName, itemDescription, startBid, durationSeconds);
                } catch (NumberFormatException e) {
                    displayError("Lance inicial e duração devem ser números válidos: " + e.getMessage());
                }
                break;
            case "lsonline":
                listClients();
                break;
            case "chat":
                if (parts.length < 2) {
                    displayError("Uso: chat <user_id> <mensagem>");
                    displayError("Exemplo: chat <id_do_usuario> Ola, quero negociar!");
                    return;
                }
                String[] chatArgs = parts[1].split(" ", 2);
                if (chatArgs.length < 2) {
                    displayError("Uso: chat <user_id> <mensagem>");
                    return;
                }
                String targetUserId = chatArgs[0];
                String messageContent = chatArgs[1];
                // Por simplicidade, mensagem direta sem leilão relacionado por enquanto
                client.sendDirectMessage(targetUserId, messageContent, null);
                break;
            case "help":
                displayHelp();
                break;
            default:
                displayError("Comando desconhecido. Digite 'help' para ver os comandos disponíveis.");
        }
    }

    /**
     * Exibe o estado atual da aplicação (leilões ativos e usuários online).
     */
    public synchronized void displayCurrentState() {
        System.out.println("\n--- LEILÕES ATIVOS ---");
        java.util.concurrent.atomic.AtomicBoolean haveActiveAuctions = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        if (client.getActiveAuctions().isEmpty()) {
            System.out.println("Nenhum leilão ativo no momento.");
        } else {
            // Ordenar por tempo restante para melhor visualização
            client.getActiveAuctions().stream()
                    .sorted(Comparator.comparingLong(AuctionItem::getEndTimeMillis))
                    .forEach(item -> {
                        long endTimeMillis = item.getEndTimeMillis();
                        java.time.Instant endInstant = java.time.Instant.ofEpochMilli(endTimeMillis);
                        java.time.ZonedDateTime endDateTime = endInstant.atZone(java.time.ZoneId.systemDefault());
                        String formattedEndTime = endDateTime
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS"));

                        if (System.currentTimeMillis() < endTimeMillis) {
                            haveActiveAuctions.set(true);
                            System.out.printf(
                                    "ID: %s | Item: %-20s | Lance Atual: %.2f (por %s) | Vendedor: %s | Expira em: %s%n",
                                    item.getId(),
                                    item.getName(),
                                    item.getCurrentBid(),
                                    item.getHighestBidderUsername() != null ? item.getHighestBidderUsername() : "N/A",
                                    item.getSellerUsername(),
                                    formattedEndTime);
                        } else {
                            recentlyDiscontinuedAuctions.add(item);
                        }
                    });
            if (!haveActiveAuctions.get()) {
                System.out.println("Nenhum leilão ativo no momento.");
            }
        }

        System.out.println("---------------------------------------------------");
        System.out.println("--- LEILÕES ENCERRADOS ---");
        if (client.getDiscontinuedAuctions().isEmpty() && recentlyDiscontinuedAuctions.isEmpty()) {
            System.out.println("Nenhum leilão encerrado.");
        } else {
            // Exibe leilões que foram recentemente descontinuados
            for (AuctionItem item : recentlyDiscontinuedAuctions) {
                System.out.printf("ID: %s | Item: %-20s | ENCERRADO | Lance Final: %.2f (por %s) | Vendedor: %s%n",
                        item.getId(),
                        item.getName(),
                        item.getCurrentBid(),
                        item.getHighestBidderUsername() != null ? item.getHighestBidderUsername() : "N/A",
                        item.getSellerUsername());
            }
            recentlyDiscontinuedAuctions.clear(); // Limpa a lista após exibir
            client.getDiscontinuedAuctions().stream()
                    .sorted(Comparator.comparingLong(AuctionItem::getEndTimeMillis))
                    .forEach(item -> {
                        System.out.printf(
                                "ID: %s | Item: %-20s | ENCERRADO | Lance Final: %.2f (por %s) | Vendedor: %s%n",
                                item.getId(),
                                item.getName(),
                                item.getCurrentBid(),
                                item.getHighestBidderUsername() != null ? item.getHighestBidderUsername() : "N/A",
                                item.getSellerUsername());
                    });
        }
        System.out.println("---------------------------------------------------");
        System.out.print("> ");
    }

    public synchronized void listClients() {
        System.out.println("\n--- USUÁRIOS ONLINE ---");

        // Filtra todos os usuários menos o próprio
        var otherUsers = client.getActiveUsers().values().stream()
                .filter(user -> !user.getUserId().equals(client.getUserId()))
                .sorted(Comparator.comparing(UserInfo::getUsername))
                .toList(); // Cria uma lista imutável com os outros usuários

        if (otherUsers.isEmpty()) {
            System.out.println("Nenhum outro usuário online.");
        } else {
            otherUsers
                    .forEach(user -> System.out.println("ID: " + user.getUserId() + " | Nome: " + user.getUsername()));
        }

        System.out.println("---------------------------------------------------");
        System.out.print("> "); // Reimprime o prompt
    }

    /**
     * Exibe uma mensagem geral para o usuário.
     * 
     * @param message A mensagem a ser exibida.
     */
    public synchronized void displayMessage(String message) {
        System.out.println("\n[INFO] " + message);
        if (loggedIn) { // Se estiver logado, re-imprime o prompt para não atrapalhar
            System.out.print("> ");
        }
    }

    /**
     * Exibe uma mensagem de erro para o usuário.
     * 
     * @param error A mensagem de erro a ser exibida.
     */
    public synchronized void displayError(String error) {
        System.err.println("\n[ERRO] " + error);
        if (loggedIn) { // Se estiver logado, re-imprime o prompt para não atrapalhar
            System.out.print("> ");
        }
    }

    /**
     * Define o status de login da UI. Chamado pelo AuctionClient após resposta do
     * servidor.
     * 
     * @param loggedIn True se o login foi bem-sucedido, false caso contrário.
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Exibe a lista de comandos disponíveis para o usuário.
     */
    /**
     * Faz o parsing de uma string para um valor de lance em formato double.
     * Tenta converter diferentes formatos de número para o formato adequado.
     * 
     * @param value A string contendo o valor do lance
     * @return O valor do lance como double
     * @throws NumberFormatException se não for possível converter para um número
     *                               válido
     */
    private double parseBidAmount(String value) throws NumberFormatException {
        // Remove espaços e substitui vírgula por ponto (caso o usuário use vírgula)
        String cleanValue = value.trim().replace(",", ".");

        try {
            double amount = Double.parseDouble(cleanValue);
            // Garante que o valor tem exatamente 2 casas decimais
            return Math.round(amount * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    "Formato inválido para valor monetário. Use números com até duas casas decimais.");
        }
    }

    /**
     * Faz o parsing de uma string para um valor de duração em segundos.
     * Tenta converter diferentes formatos de número para o formato adequado.
     * 
     * @param value A string contendo a duração
     * @return A duração em segundos como int
     * @throws NumberFormatException se não for possível converter para um número
     *                               válido
     */
    private int parseAuctionDuration(String value) throws NumberFormatException {
        // Remove espaços e qualquer parte decimal (caso o usuário digite um float)
        String cleanValue = value.trim().split("\\.")[0];

        try {
            int duration = Integer.parseInt(cleanValue);
            if (duration <= 0) {
                throw new NumberFormatException("A duração deve ser maior que zero.");
            }
            return duration;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("A duração deve ser um número inteiro positivo em segundos.");
        }
    }

    private void displayHelp() {
        System.out.println("\n--- COMANDOS DISPONÍVEIS ---");
        System.out.println("lsauctions              - Lista todos os leilões ativos e encerrados.");
        System.out.println("bid <auction_id> <valor> - Dá um lance em um leilão específico.");
        System.out.println(
                "createauction '<nome>' '<descricao>' <lance_inicial> <duracao_segundos> - Cria um novo leilão.");
        System.out.println("lsonline                - Lista todos os usuários online.");
        System.out.println("chat <user_id> <mensagem> - Envia uma mensagem P2P direta para outro usuário.");
        System.out.println("help                    - Exibe esta ajuda.");
        System.out.println("exit                    - Sai da aplicação.");
        System.out.println("---------------------------------------------------");
    }
}