package com.auction.client;

import com.auction.common.*; // Importa todas as classes de mensagem e utilitários

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente para a aplicação de leilões online.
 * Gerencia a conexão com o servidor, o estado local da aplicação e a
 * comunicação P2P.
 */
public class AuctionClient {
    private String userId; // ID único deste cliente.
    private String username; // Nome de usuário (display name).
    private int p2pPort; // Porta que este cliente usará para aceitar conexões P2P.

    // Conexão com o servidor principal
    private Socket serverConnectionSocket;
    private ObjectInputStream inFromServer;
    private ObjectOutputStream outToServer;

    // Estado local da aplicação (listas de leilões e usuários online)
    private final List<AuctionItem> activeAuctions = Collections.synchronizedList(new ArrayList<>());
    private final List<AuctionItem> discontinuedAuctions = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, UserInfo> activeUsers = Collections.synchronizedMap(new HashMap<>());

    // Componentes para a comunicação P2P
    private ServerSocket p2pServerSocket; // Servidor para aceitar conexões P2P de outros clientes
    private final Map<String, PeerConnectionHandler> activePeerConnections = Collections
            .synchronizedMap(new HashMap<>()); // Conexões P2P diretas estabelecidas

    // Scheduler para tarefas em segundo plano (ex: Keep-Alive)
    private ScheduledExecutorService scheduler;

    // Referência para a UI (interface de usuário) para exibir mensagens.
    private ClientUI ui;

    /**
     * Construtor do AuctionClient.
     * Gera um ID único para o cliente.
     */
    public AuctionClient() {
        this.userId = UUID.randomUUID().toString(); // Garante um ID único para cada cliente
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getP2pPort() {
        return p2pPort;
    }

    public List<AuctionItem> getActiveAuctions() {
        return activeAuctions;
    }

    public List<AuctionItem> getDiscontinuedAuctions() {
        return discontinuedAuctions;
    }

    public List<AuctionItem> getLiveAuctions() {
        return activeAuctions;
    }

    public List<AuctionItem> getAllAuctions() {
        List<AuctionItem> all = new ArrayList<>();
        all.addAll(activeAuctions);
        all.addAll(discontinuedAuctions);
        return all;
    }

    public Map<String, UserInfo> getActiveUsers() {
        return activeUsers;
    }

    public ClientUI getUi() {
        return ui;
    }

    public Map<String, PeerConnectionHandler> getActivePeerConnections() {
        return activePeerConnections;
    };

    private final Map<String, List<DirectMessage>> pendingP2PMessages = Collections.synchronizedMap(new HashMap<>());

    public void setUi(ClientUI ui) {
        this.ui = ui;
    }

    /**
     * Tenta conectar-se ao servidor de leilões.
     *
     * @param serverIp   O endereço IP do servidor.
     * @param serverPort A porta do servidor.
     * @throws IOException Se houver um erro de conexão.
     */
    public void connectToServer(String serverIp, int serverPort) throws IOException {
        // Tenta iniciar o servidor P2P local antes de conectar ao servidor principal
        // A porta P2P será enviada na mensagem de Login.
        startP2PServer();
        if (p2pServerSocket == null) {
            throw new IOException("Não foi possível iniciar o servidor P2P local. Verifique as portas.");
        }

        serverConnectionSocket = new Socket(serverIp, serverPort);
        // A ordem de criação dos ObjectOutputStream e ObjectInputStream é CRUCIAL!
        // Output stream DEVE ser criado antes do input stream para evitar deadlock na
        // conexão inicial.
        outToServer = new ObjectOutputStream(serverConnectionSocket.getOutputStream());
        inFromServer = new ObjectInputStream(serverConnectionSocket.getInputStream());
        ui.displayMessage("Conectado ao servidor de leilões em " + serverIp + ":" + serverPort);

        // Inicia uma thread separada para escutar mensagens do servidor.
        new Thread(new ServerListener(inFromServer, this)).start();
    }

    /**
     * Inicia um ServerSocket para que este cliente possa aceitar conexões P2P de
     * outros clientes.
     * Tenta portas sequenciais a partir de P2P_BASE_PORT.
     */
    private void startP2PServer() {
        AtomicInteger currentP2PPort = new AtomicInteger(Constants.P2P_BASE_PORT);
        while (p2pServerSocket == null) {
            try {
                p2pPort = currentP2PPort.getAndIncrement();
                p2pServerSocket = new ServerSocket(p2pPort);
                ui.displayMessage("Servidor P2P do cliente iniciado na porta " + p2pPort);
                // Inicia uma thread para escutar por conexões P2P de outros clientes.
                new Thread(new PeerListener(p2pServerSocket, this)).start();
            } catch (IOException e) {
                ui.displayError("Porta P2P " + p2pPort + " em uso. Tentando a próxima...");
                if (currentP2PPort.get() > Constants.P2P_BASE_PORT + 100) { // Limite de tentativas para evitar loop
                                                                            // infinito
                    ui.displayError("Não foi possível encontrar uma porta P2P disponível após 100 tentativas.");
                    break;
                }
            }
        }
    }

    /**
     * Envia a mensagem de login para o servidor.
     *
     * @param username O nome de usuário escolhido.
     */
    public void login(String username) {
        this.username = username;
        // Determine the local IP address used for the connection to the server and
        // include it
        String localIp = null;
        try {
            if (serverConnectionSocket != null && serverConnectionSocket.getLocalAddress() != null) {
                localIp = serverConnectionSocket.getLocalAddress().getHostAddress();
            }
        } catch (Exception e) {
            // ignore and send without IP
        }
        sendMessageToServer(new LoginMessage(userId, username, p2pPort, localIp));
        // Agendar o envio de mensagens Keep-Alive para o servidor.
        scheduler.scheduleAtFixedRate(() -> sendMessageToServer(new KeepAliveMessage(userId)),
                Constants.KEEP_ALIVE_INTERVAL_MS,
                Constants.KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Envia uma mensagem genérica para o servidor.
     *
     * @param message A Message a ser enviada.
     */
    public void sendMessageToServer(Message message) {
        try {
            // Reset the stream so that updated objects are fully serialized
            outToServer.reset();
            outToServer.writeObject(message);
            outToServer.flush();
        } catch (SocketException e) {
            ui.displayError("Conexão com o servidor perdida: " + e.getMessage());
            closeConnections();
        } catch (IOException e) {
            ui.displayError("Erro ao enviar mensagem para o servidor: " + e.getMessage());
            closeConnections();
        }
    }

    /**
     * Processa as mensagens recebidas do servidor.
     * Este método é chamado pela ServerListener thread.
     *
     * @param message A Message recebida do servidor.
     * @throws InterruptedException
     */
    public void handleServerMessage(Message message) throws InterruptedException {
        ui.displayMessage("Recebido do servidor: " + message.getType());
        switch (message.getType()) {
            case LOGIN_RESPONSE:
                LoginResponseMessage loginResp = (LoginResponseMessage) message;
                if (loginResp.isSuccess()) {
                    ui.displayMessage(loginResp.getMessage() + " Logado como " + username);
                    // Atualiza listas locais com dados iniciais do servidor
                    activeAuctions.clear();
                    activeAuctions.addAll(loginResp.getInitialAuctions());
                    activeUsers.clear();
                    loginResp.getActiveUsers().forEach(user -> activeUsers.put(user.getUserId(), user));
                    ui.setLoggedIn(true); // Atualiza o estado de login da UI
                    Thread.sleep(2000); // Pausa para o usuário ler a mensagem
                    ui.displayCurrentState();
                    ui.listClients();
                } else {
                    ui.displayError("Falha no login: " + loginResp.getMessage());
                    closeConnections();
                }
                break;
            case AUCTION_LIST_RESPONSE:
                AuctionListResponseMessage auctionListResp = (AuctionListResponseMessage) message;
                activeAuctions.clear();
                activeAuctions.addAll(auctionListResp.getActiveAuctions());
                discontinuedAuctions.clear();
                discontinuedAuctions.addAll(auctionListResp.getDiscontinuedAuctions());
                ui.displayCurrentState();
                break;
            case AUCTION_UPDATE:
                AuctionUpdateMessage auctionUpdate = (AuctionUpdateMessage) message;
                // Atualiza o leilão na lista local ou adiciona se for novo
                updateOrCreateAuctionLocally(auctionUpdate.getUpdatedAuctionItem());
                ui.displayMessage("--- Leilão atualizado: " + auctionUpdate.getUpdatedAuctionItem().getName() +
                        " - " + auctionUpdate.getUpdateDescription() + " ---");
                ui.displayCurrentState();
                break;
            case USER_STATUS_UPDATE:
                UserStatusUpdateMessage userUpdate = (UserStatusUpdateMessage) message;
                if (userUpdate.isOnline()) {
                    activeUsers.put(userUpdate.getUser().getUserId(), userUpdate.getUser());
                    ui.displayMessage("--- Usuário '" + userUpdate.getUser().getUsername() + "' ficou online. ---");
                } else {
                    activeUsers.remove(userUpdate.getUser().getUserId());
                    ui.displayMessage("--- Usuário '" + userUpdate.getUser().getUsername() + "' ficou offline. ---");
                    // Se o usuário P2P se desconectou, fechar a conexão P2P com ele, se houver.
                    PeerConnectionHandler handler = activePeerConnections.remove(userUpdate.getUser().getUserId());
                    if (handler != null) {
                        handler.closeConnection();
                        ui.displayMessage("Conexão P2P com '" + userUpdate.getUser().getUsername() + "' fechada.");
                    }
                }
                ui.listClients();
                break;
            case PEER_INFO_RESPONSE:
                PeerInfoResponseMessage peerInfoResp = (PeerInfoResponseMessage) message;
                if (peerInfoResp.getTargetIp() != null) {
                    ui.displayMessage("Informações do peer '" + peerInfoResp.getTargetUserId() + "': " +
                            peerInfoResp.getTargetIp() + ":" + peerInfoResp.getTargetPort());
                    try {
                        // Tenta conectar diretamente ao peer com as informações recebidas
                        connectToPeer(peerInfoResp.getTargetUserId(), peerInfoResp.getTargetIp(),
                                peerInfoResp.getTargetPort());
                    } catch (IOException e) {
                        ui.displayError("Erro ao tentar conectar diretamente ao peer: " + e.getMessage());
                        pendingP2PMessages.remove(peerInfoResp.getTargetUserId());
                    }
                } else {
                    ui.displayMessage("Peer '" + peerInfoResp.getTargetUserId() + "' não encontrado ou offline.");
                    pendingP2PMessages.remove(peerInfoResp.getTargetUserId());
                }
                break;
            default:
                ui.displayMessage("Tipo de mensagem não tratada do servidor: " + message.getType());
        }
    }

    /**
     * Atualiza um AuctionItem na lista local ou o adiciona se for novo.
     * 
     * @param newItem O AuctionItem para atualizar/adicionar.
     */
    private void updateOrCreateAuctionLocally(AuctionItem newItem) {
        boolean found = false;
        for (int i = 0; i < activeAuctions.size(); i++) {
            if (activeAuctions.get(i).getId().equals(newItem.getId())) {
                activeAuctions.set(i, newItem); // Substitui o item existente
                found = true;
                break;
            }
        }
        if (!found) {
            activeAuctions.add(newItem); // Adiciona como novo item
        }
    }

    // --- Métodos de ação do cliente (chamados pela UI) ---

    public void requestAuctionList() {
        sendMessageToServer(new AuctionListRequestMessage(userId));
    }

    public void placeBid(String auctionId, double amount) {
        AuctionItem item = activeAuctions.stream().filter(a -> a.getId().equals(auctionId)).findFirst().orElse(null);
        if (item == null) {
            ui.displayError("Leilão com ID " + auctionId + " não encontrado.");
            return;
        }
        if (amount <= item.getCurrentBid()) {
            ui.displayError(
                    "Seu lance de " + amount + " deve ser maior que o lance atual de " + item.getCurrentBid() + ".");
            return;
        }
        sendMessageToServer(new PlaceBidMessage(userId, auctionId, amount, username));
    }

    public void createAuction(String name, String description, double startBid, int durationSeconds) {
        if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()
                || startBid <= 0 || durationSeconds <= 0) {
            ui.displayError("Nome, descrição, lance inicial e duração são obrigatórios e devem ser válidos.");
            return;
        }
        sendMessageToServer(new CreateAuctionMessage(userId, name, description, startBid, durationSeconds));
        ui.displayMessage("Solicitação para criar leilão enviada.");
    }

    public void requestPeerInfo(String targetUserId) {
        if (targetUserId.equals(userId)) {
            ui.displayMessage("Você não pode solicitar informações P2P de si mesmo.");
            return;
        }
        if (!activeUsers.containsKey(targetUserId)) {
            ui.displayError("Usuário com ID " + targetUserId + " não está online.");
            return;
        }
        sendMessageToServer(new PeerInfoRequestMessage(userId, targetUserId));
    }

    /**
     * Tenta estabelecer uma conexão P2P com outro cliente.
     * Se já houver uma conexão ativa, não faz nada.
     *
     * @param peerId   ID do cliente peer.
     * @param peerIp   Endereço IP do cliente peer.
     * @param peerPort Porta P2P do cliente peer.
     * @throws IOException Se houver um erro ao conectar.
     */
    public void connectToPeer(String peerId, String peerIp, int peerPort) throws IOException {
        if (activePeerConnections.containsKey(peerId)) {
            ui.displayMessage("Já conectado ao peer " + peerId);
            return;
        }
        if (peerId.equals(userId)) {
            ui.displayMessage("Não é possível estabelecer conexão P2P consigo mesmo.");
            return;
        }

        Socket directPeerSocket = new Socket(peerIp, peerPort);
        // Cria um PeerConnectionHandler para gerenciar esta conexão P2P
        PeerConnectionHandler handler = new PeerConnectionHandler(directPeerSocket, this, peerId);
        addActivePeerConnection(peerId, handler);
        new Thread(handler).start();
        ui.displayMessage("Conectado diretamente ao peer " + peerId + " (IP: " + peerIp + ", Porta P2P: " + peerPort
                + ") para P2P.");
    }

    /**
     * Método chamado quando uma conexão P2P é estabelecida com sucesso.
     * Verifica se há mensagens pendentes para este peer e as envia.
     *
     * @param peerId ID do cliente peer com o qual a conexão foi estabelecida.
     */
    public void addActivePeerConnection(String peerId, PeerConnectionHandler handler) {
        activePeerConnections.put(peerId, handler);
        onPeerConnectionEstablished(peerId); // Chama o método para verificar e enviar mensagens pendentes
    }

    /**
     * Verifica e envia mensagens P2P pendentes para o peer especificado.
     *
     * @param peerId ID do cliente peer.
     */
    private void onPeerConnectionEstablished(String peerId) {
        List<DirectMessage> messagesToSend = pendingP2PMessages.remove(peerId); // Obtém e remove mensagens pendentes
        if (messagesToSend != null && !messagesToSend.isEmpty()) {
            PeerConnectionHandler handler = activePeerConnections.get(peerId);
            if (handler != null) {
                for (DirectMessage message : messagesToSend) {
                    handler.sendMessage(message);
                    ui.displayMessage(
                            "Mensagem P2P pendente enviada para " + activeUsers.get(peerId).getUsername() + ".");
                }
            } else {
                ui.displayError(
                        "Erro interno: Handler P2P não encontrado para peer " + activeUsers.get(peerId).getUsername()
                                + " após conexão estabelecida. Mensagens pendentes não enviadas.");
            }
        }
    }

    /**
     * Envia uma mensagem direta para outro peer via conexão P2P.
     * Se não houver uma conexão P2P ativa, tenta solicitá-la ao servidor.
     *
     * @param targetUserId     ID do cliente de destino.
     * @param content          Conteúdo da mensagem.
     * @param relatedAuctionId ID opcional do leilão relacionado.
     */
    @SuppressWarnings("unused")
    public void sendDirectMessage(String targetUserId, String content, String relatedAuctionId) {
        if (targetUserId.equals(userId)) {
            ui.displayMessage("Você não pode enviar mensagem direta para si mesmo.");
            return;
        }
        PeerConnectionHandler handler = activePeerConnections.get(targetUserId);
        if (handler != null) {
            handler.sendMessage(new DirectMessage(userId, targetUserId, content, relatedAuctionId));
            ui.displayMessage("Mensagem direta enviada para " + activeUsers.get(targetUserId).getUsername() + ".");
        } else {
            // Armazena a mensagem para envio posterior
            DirectMessage pendingMessage = new DirectMessage(userId, targetUserId, content, relatedAuctionId);
            pendingP2PMessages.computeIfAbsent(targetUserId, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(pendingMessage);

            ui.displayMessage("Não há conexão P2P direta com " + targetUserId + ". Tentando estabelecer...");
            UserInfo targetUser = activeUsers.get(targetUserId);
            if (targetUser != null) {
                // Se o usuário estiver online, solicita as informações P2P ao servidor
                requestPeerInfo(targetUserId);
                // O usuário precisará tentar enviar a mensagem novamente depois que a conexão
                // P2P for estabelecida
                ui.displayMessage("Solicitada informação P2P ao servidor. Tente enviar a mensagem novamente em breve.");
            } else {
                ui.displayError("Usuário com ID " + targetUserId + " não está online para comunicação P2P.");
            }
        }
    }

    /**
     * Processa mensagens recebidas via P2P.
     * Este método é chamado pela PeerConnectionHandler thread.
     *
     * @param message A Message P2P recebida.
     */
    public void handlePeerMessage(Message message) {
        ui.displayMessage("Recebido P2P de " + message.getSenderId() + ": " + message.getType());
        if (message.getType() == MessageType.DIRECT_MESSAGE) {
            DirectMessage dm = (DirectMessage) message;
            String senderUsername = activeUsers.containsKey(dm.getSenderId())
                    ? activeUsers.get(dm.getSenderId()).getUsername()
                    : dm.getSenderId();
            ui.displayMessage("[P2P de " + senderUsername + "]: " + dm.getContent());
        }
        // Outros tipos de mensagens P2P podem ser tratados aqui
    }

    /**
     * Fecha todas as conexões (com o servidor e P2P) e desliga o scheduler.
     */
    public void closeConnections() {
        ui.displayMessage("Fechando conexões...");
        if (scheduler != null)
            scheduler.shutdownNow();
        try {
            if (outToServer != null)
                outToServer.close();
            if (inFromServer != null)
                inFromServer.close();
            if (serverConnectionSocket != null)
                serverConnectionSocket.close();
            if (p2pServerSocket != null && !p2pServerSocket.isClosed())
                p2pServerSocket.close();
            // Fecha todas as conexões P2P ativas
            activePeerConnections.values().forEach(PeerConnectionHandler::closeConnection);
            ui.displayMessage("Cliente " + username + " desconectado.");
        } catch (IOException e) {
            ui.displayError("Erro ao fechar conexões do cliente: " + e.getMessage());
        }
    }

    /**
     * Método principal para iniciar a aplicação cliente.
     *
     * @param args Argumentos da linha de comando (espera-se o IP do servidor como
     *             primeiro argumento).
     */
    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        ClientUI ui = new ClientUI(client);
        client.setUi(ui);

        String serverIp = "localhost"; // Valor padrão para testes locais

        // Verifica se um IP do servidor foi fornecido como argumento
        if (args.length > 0) {
            serverIp = args[0];
        } else {
            ui.displayMessage("Nenhum IP do servidor fornecido. Usando 'localhost' como padrão.");
            ui.displayMessage("Uso: java -cp out com.auction.client.AuctionClient <IP_DO_SERVIDOR>");
        }

        try {
            ui.displayMessage("Digite 'exit' para sair a qualquer momento.\n");
            ui.displayMessage("Tentando conectar ao servidor " + serverIp + "...\n");
            client.connectToServer(serverIp, Constants.SERVER_PORT); // Conecta ao servidor com o IP fornecido
            ui.start();
        } catch (IOException e) {
            ui.displayError("Não foi possível conectar ao servidor ou iniciar o servidor P2P: " + e.getMessage());
        } finally {
            client.closeConnections();
        }
    }
}