package com.auction.server;

import com.auction.common.*; // Importa todas as classes de mensagem e utilitários
import com.auction.common.AuctionItem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servidor principal da aplicação de leilões online.
 * Gerencia a conexão de clientes, o estado dos leilões e a comunicação de mensagens.
 */
public class AuctionServer {
    // Socket principal do servidor que aceita novas conexões de clientes.
    private ServerSocket welcomeSocket;

    // Mapa thread-safe para armazenar ClientHandlers de clientes conectados, indexados por userId.
    private final Map<String, ClientHandler> connectedClients = Collections.synchronizedMap(new HashMap<>());

    // Mapa thread-safe para armazenar informações de UserInfo de clientes ativos, indexados por userId.
    // Usado para obter IP/porta P2P para comunicação direta entre clientes.
    private final Map<String, UserInfo> activeUsersInfo = Collections.synchronizedMap(new HashMap<>());

    // Mapa para rastrear o último tempo de atividade (keep-alive) de cada cliente.
    private final Map<String, Long> lastActivityMap = Collections.synchronizedMap(new HashMap<>());

    // Gerenciador de leilões, responsável pela lógica de negócios dos leilões.
    private AuctionManager auctionManager;

    // Scheduler para executar tarefas em segundo plano (ex: verificar fim de leilões).
    private ScheduledExecutorService scheduler;

    /**
     * Construtor para o AuctionServer.
     * Inicializa o ServerSocket e o AuctionManager.
     *
     * @param port A porta em que o servidor irá escutar as conexões.
     */
    public AuctionServer(int port) {
        try {
            welcomeSocket = new ServerSocket(port);
            auctionManager = new AuctionManager(this); // Passa a referência do próprio servidor ao gerenciador
            // Cria um scheduler com um pool de 2 threads para tarefas agendadas.
            // Uma para checkAuctionEndTimes, outra para checkClientInactivity.
            scheduler = Executors.newScheduledThreadPool(2);
            System.out.println("Servidor de leilão iniciado na porta " + port);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            System.exit(1); // Sai do programa se o servidor não puder iniciar
        }
    }

    /**
     * Inicia o loop principal do servidor para aceitar conexões e agendar tarefas.
     */
    public void start() {
        // Agendamento da tarefa para verificar o fim dos leilões periodicamente.
        // O método checkAuctionEndTimes do auctionManager será chamado a cada AUCTION_END_CHECK_INTERVAL_MS.
        scheduler.scheduleAtFixedRate(auctionManager::checkAuctionEndTimes, 0,
                                       Constants.AUCTION_END_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(this::checkClientInactivity, 0,
                                        Constants.CLIENT_INACTIVITY_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        System.out.println("Servidor aguardando conexões de clientes...");
        // Loop infinito para aceitar novas conexões de clientes
        while (true) {
            try {
                Socket clientSocket = welcomeSocket.accept(); // Bloqueia até uma nova conexão chegar
                System.out.println("Novo cliente conectado de: " + clientSocket.getInetAddress().getHostAddress());
                // Cria um novo ClientHandler para esta conexão e o executa em uma nova thread.
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão de cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Adiciona um ClientHandler à lista de clientes conectados após o login.
     * Também armazena as informações do usuário para P2P e notifica outros clientes.
     *
     * @param userId O ID único do cliente.
     * @param handler A instância do ClientHandler para este cliente.
     * @param ipAddress O endereço IP do cliente.
     * @param p2pPort A porta P2P que o cliente está escutando.
     */
    public synchronized void addClient(String userId, ClientHandler handler, String ipAddress, int p2pPort) {
        // Verifica se o userId já existe (e.g., reconexão ou erro)
        if (connectedClients.containsKey(userId)) {
            System.out.println("Cliente " + userId + " já estava conectado. Atualizando handler.");
        }
        connectedClients.put(userId, handler);
        UserInfo userInfo = new UserInfo(userId, handler.getUsername(), ipAddress, p2pPort);
        activeUsersInfo.put(userId, userInfo);
        System.out.println("Cliente '" + handler.getUsername() + "' (ID: " + userId + ") logado. Total online: " + activeUsersInfo.size());

        // Notifica todos os outros clientes sobre o novo usuário online
        broadcast(new UserStatusUpdateMessage("server", userInfo, true));
    }

    /**
     * Remove um ClientHandler da lista de clientes conectados.
     * Chamado quando um cliente se desconecta ou sua conexão é perdida.
     *
     * @param userId O ID único do cliente a ser removido.
     */
    public synchronized void removeClient(String userId) {
        ClientHandler handler = connectedClients.remove(userId);
        UserInfo userInfo = activeUsersInfo.remove(userId);
        if (handler != null && userInfo != null) {
            System.out.println("Cliente '" + userInfo.getUsername() + "' (ID: " + userId + ") desconectado. Total online: " + activeUsersInfo.size());
            // Notifica todos os outros clientes que este usuário ficou offline
            broadcast(new UserStatusUpdateMessage("server", userInfo, false));
        }
    }

    /**
     * Envia uma mensagem para todos os clientes atualmente conectados.
     *
     * @param message A Message a ser broadcastada.
     */
    public void broadcast(Message message) {
        // Para evitar ConcurrentModificationException ao iterar sobre o mapa
        // e potencialmente remover clientes ao mesmo tempo, itera-se sobre uma cópia.
        // Ou, como connectedClients é synchronizedMap, o bloco synchronized ajuda.
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients.values()) {
                // Não envia a mensagem de status de online/offline para o próprio usuário que acabou de logar/deslogar
                if (message instanceof UserStatusUpdateMessage) {
                    UserStatusUpdateMessage statusMsg = (UserStatusUpdateMessage) message;
                    if (statusMsg.getUser().getUserId().equals(handler.getUserId())) {
                        continue; // Não envie para o próprio usuário
                    }
                }
                handler.sendMessage(message);
            }
        }
    }

    /**
     * Envia uma mensagem para um cliente específico pelo seu ID.
     *
     * @param userId O ID do cliente de destino.
     * @param message A Message a ser enviada.
     */
    public void sendMessageToClient(String userId, Message message) {
        ClientHandler handler = connectedClients.get(userId);
        if (handler != null) {
            handler.sendMessage(message);
        } else {
            System.out.println("Cliente " + userId + " não encontrado para enviar mensagem: " + message.getType());
        }
    }

    /**
     * Retorna as informações P2P de um usuário específico.
     * Usado para responder a solicitações PEER_INFO_REQUEST.
     *
     * @param userId O ID do usuário.
     * @return UserInfo do usuário, ou null se não encontrado/online.
     */
    public UserInfo getPeerInfo(String userId) {
        return activeUsersInfo.get(userId);
    }

    /**
     * Retorna o mapa de informações de usuários ativos.
     * Utilizado pelo AuctionManager para obter nomes de usuários.
     *
     * @return Mapa de UserInfo de usuários ativos.
     */
    public Map<String, UserInfo> getActiveUsersInfo() {
        return activeUsersInfo;
    }

    /**
     * Método central para processar mensagens recebidas de ClientHandlers.
     * A lógica de roteamento e tratamento das mensagens acontece aqui.
     *
     * @param message A Message recebida.
     * @param sender O ClientHandler que enviou a mensagem.
     */
    public void handleMessage(Message message, ClientHandler sender) {
        System.out.println("Servidor recebeu de '" + (sender.getUsername() != null ? sender.getUsername() : sender.getUserId()) + "' (" + message.getSenderId() + "): " + message.getType());

        switch (message.getType()) {
            case LOGIN:
                LoginMessage loginMsg = (LoginMessage) message;
                lastActivityMap.put(loginMsg.getSenderId(), System.currentTimeMillis());
                // O addClient já foi chamado no ClientHandler após a primeira mensagem
                // Agora envia a resposta de login para o cliente
                sendMessageToClient(sender.getUserId(), new LoginResponseMessage(
                    "server", true, "Login bem-sucedido!",
                    auctionManager.getLiveAuctions(),
                    new java.util.ArrayList<>(activeUsersInfo.values()) // Lista de usuários online
                ));
                break;
            case LOGOUT:
                // O ClientHandler já remove o cliente via closeConnection, então aqui é mais para log
                System.out.println("Cliente " + sender.getUsername() + " solicitou LOGOUT.");
                lastActivityMap.remove(sender.getUserId());
                sender.closeConnection(); // Irá chamar removeClient
                break;
            case AUCTION_LIST_REQUEST:
                // Responde com a lista de leilões
                sendMessageToClient(sender.getUserId(), new AuctionListResponseMessage(
                    "server", auctionManager.getLiveAuctions(), auctionManager.getDiscontinuedAuctions()
                ));
                break;
            case PLACE_BID:
                PlaceBidMessage bidMsg = (PlaceBidMessage) message;
                // Passa o processamento do lance para o AuctionManager
                boolean bidAccepted = auctionManager.placeBid(bidMsg.getAuctionId(), bidMsg.getSenderId(), bidMsg.getBidAmount());
                // O AuctionManager já faz o broadcast da atualização se o lance for aceito.
                // Se o lance não for aceito, podemos enviar uma mensagem de erro específica de volta ao cliente.
                if (!bidAccepted) {
                    sendMessageToClient(sender.getUserId(), new AuctionUpdateMessage(
                        "server", auctionManager.getAuction(bidMsg.getAuctionId()), "Lance inválido."));
                }
                break;
            case CREATE_AUCTION:
                CreateAuctionMessage createAuctionMsg = (CreateAuctionMessage) message;
                // Cria um novo AuctionItem usando os dados da mensagem
                AuctionItem newAuction = new AuctionItem(
                    createAuctionMsg.getItemName(),
                    createAuctionMsg.getItemDescription(),
                    createAuctionMsg.getStartBid(),
                    createAuctionMsg.getDurationSeconds(),
                    createAuctionMsg.getSenderId(),
                    sender.getUsername() // Pega o username do sender para associar ao vendedor
                );
                auctionManager.addAuction(newAuction);
                // O addAuction já faz o broadcast da criação do leilão.
                break;
            case KEEP_ALIVE:
                // Atualiza o tempo de última atividade do cliente
                lastActivityMap.put(sender.getUserId(), System.currentTimeMillis());
                break;
            case PEER_INFO_REQUEST:
                PeerInfoRequestMessage peerReq = (PeerInfoRequestMessage) message;
                UserInfo peerInfo = getPeerInfo(peerReq.getTargetUserId());
                if (peerInfo != null) {
                    sendMessageToClient(sender.getUserId(), new PeerInfoResponseMessage(
                        "server", peerReq.getTargetUserId(), peerInfo.getIpAddress(), peerInfo.getP2pPort()
                    ));
                } else {
                    sendMessageToClient(sender.getUserId(), new PeerInfoResponseMessage(
                        "server", peerReq.getTargetUserId(), null, 0 // Peer não encontrado ou offline
                    ));
                }
                break;
            case AUCTION_UPDATE: // Mensagens de atualização de leilão são apenas do server -> client
            case USER_STATUS_UPDATE: // Mensagens de status de usuário são apenas do server -> client
            case LOGIN_RESPONSE: // Resposta de login é apenas do server -> client
            case AUCTION_LIST_RESPONSE: // Resposta de lista de leilões é apenas do server -> client
            case PEER_INFO_RESPONSE: // Resposta de info de peer é apenas do server -> client
            case DIRECT_MESSAGE: // Mensagens P2P são diretas entre clientes
                System.err.println("Mensagem de tipo inesperado recebida do cliente: " + message.getType());
                break;
            default:
                System.out.println("Tipo de mensagem não tratado pelo servidor: " + message.getType());
        }
    }

    public void checkClientInactivity() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastActivityMap.entrySet()) {
            String userId = entry.getKey();
            long lastActiveTime = entry.getValue();
            if (currentTime - lastActiveTime > Constants.CLIENT_INACTIVITY_TIMEOUT_MS) {
                System.out.println("Cliente " + userId + " inativo por mais de " + (Constants.CLIENT_INACTIVITY_TIMEOUT_MS / 1000) + " segundos. Desconectando.");
                ClientHandler handler = connectedClients.get(userId);
                if (handler != null) {
                    handler.closeConnection(); // Isso chamará removeClient e limpará lastActivityMap
                } else {
                    // Se o handler não existir, apenas remova do lastActivityMap
                    lastActivityMap.remove(userId);
                }
            }
        }
    }

    /**
     * Método principal para iniciar o servidor.
     *
     * @param args Argumentos da linha de comando (não usados aqui).
     */
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer(Constants.SERVER_PORT);
        server.start();
    }
}
