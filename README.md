# 🚀 Sistema de Leilão Online Distribuído

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)
![TCP](https://img.shields.io/badge/TCP-007ACC?style=flat)
![Distribuído](https://img.shields.io/badge/Distribu%C3%ADdo-4CAF50?style=flat)
![Threads](https://img.shields.io/badge/Threads-F44336?style=flat)

---

## 📑 Sumário

- [🚀 Sistema de Leilão Online Distribuído](#-sistema-de-leilão-online-distribuído)
  - [📑 Sumário](#-sumário)
  - [Visão Geral do Projeto](#visão-geral-do-projeto)
  - [✨ Funcionalidades Principais](#-funcionalidades-principais)
  - [🏗️ Arquitetura](#️-arquitetura)
    - [Cliente-Servidor](#cliente-servidor)
    - [Peer-to-Peer (P2P)](#peer-to-peer-p2p)
  - [💬 Tipos de Mensagens](#-tipos-de-mensagens)
  - [⚙️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)
  - [🚀 Como Executar o Projeto](#-como-executar-o-projeto)
    - [Pré-requisitos](#pré-requisitos)
    - [Estrutura de Pastas](#estrutura-de-pastas)
  - [⚙️ Compilação e Execução](#️-compilação-e-execução)
    - [🧩 1. Usando um IDE (IntelliJ IDEA, Eclipse, etc.)](#-1-usando-um-ide-intellij-idea-eclipse-etc)
    - [💻 2. Via Linha de Comando](#-2-via-linha-de-comando)
      - [🔧 Compilar o Projeto](#-compilar-o-projeto)
      - [🚀 Executar o Servidor](#-executar-o-servidor)
    - [👥 Executar Múltiplos Clientes](#-executar-múltiplos-clientes)
    - [💬 Comandos Disponíveis no Cliente](#-comandos-disponíveis-no-cliente)

---

## Visão Geral do Projeto

Este projeto implementa um sistema de leilão online distribuído, focando na comunicação em rede entre múltiplos clientes e um servidor central.  
A aplicação permite que usuários entrem, visualizem leilões ativos, deem lances, criem novos leilões e até mesmo se comuniquem diretamente entre si (P2P), com o servidor gerenciando o estado global e a descoberta de peers.

O objetivo principal é demonstrar conceitos de **programação distribuída**, comunicação via **sockets**, gerenciamento de **threads** e design de **protocolos de mensagens** para aplicações em rede.

---

## ✨ Funcionalidades Principais

- **Login e Gerenciamento de Usuários:** Clientes podem se conectar ao servidor usando um nome de usuário.  
- **Listagem de Leilões:** Visualização de leilões ativos com detalhes de nome, lance atual e tempo restante.  
- **Lances em Leilões:** Clientes podem dar lances em tempo real, validados pelo servidor.  
- **Criação de Leilões:** Clientes logados podem criar novos leilões, definindo nome, descrição, lance inicial e duração.  
- **Notificações em Tempo Real:** Atualizações sobre novos lances e status de usuários.  
- **Comunicação Peer-to-Peer (P2P):** Mensagens diretas entre clientes, com servidor atuando apenas como diretório.  
- **Keep-Alive:** Detecta clientes inativos ou desconexões inesperadas.

---

## 🏗️ Arquitetura

### Cliente-Servidor

- **Servidor Central (AuctionServer):** Gerencia estado global, autenticação e descoberta de peers.  
- **Clientes (AuctionClient):** Conectam-se para visualizar leilões, dar lances, criar leilões e receber notificações.  
- Comunicação via **TCP** para garantir confiabilidade.

### Peer-to-Peer (P2P)

- Após descoberta, clientes estabelecem conexões TCP diretas para mensagens privadas.  
- Reduz a carga do servidor para interações cliente-cliente.

---

## 💬 Tipos de Mensagens

- `LoginMessage` / `LoginResponseMessage`  
- `KeepAliveMessage`  
- `AuctionListRequestMessage` / `AuctionListResponseMessage`  
- `PlaceBidMessage`  
- `CreateAuctionMessage`  
- `AuctionUpdateMessage`  
- `UserStatusUpdateMessage`  
- `PeerInfoRequestMessage` / `PeerInfoResponseMessage`  
- `DirectMessage`  

> Todas baseadas em objetos serializáveis `Message` definidos pelo enum `MessageType`.

---

## ⚙️ Tecnologias Utilizadas

- **Java**  
- **Sockets (java.net.\*)**  
- **Threads (java.lang.Thread, java.util.concurrent.\*)**  
- **Serialização de Objetos (java.io.Serializable)**  

---

## 🚀 Como Executar o Projeto

### Pré-requisitos

- JDK (Java Development Kit) 8 ou superior

### Estrutura de Pastas

```text  
.
├── src/
│└── main/
│└── java/
│└── com/
│└── auction/
│    ├── common/
│    │   ├── AuctionItem.java               // Representa um item de leilão com seu estado e lances.
│    │   ├── AuctionListRequestMessage.java // Mensagem para solicitar a lista de leilões ao servidor.
│    │   ├── AuctionListResponseMessage.java// Mensagem de resposta do servidor com a lista de leilões.
│    │   ├── AuctionUpdateMessage.java      // Mensagem para notificar atualizações de um leilão.
│    │   ├── Constants.java                 // Contém constantes globais para a aplicação (portas, tempos).
│    │   ├── CreateAuctionMessage.java      // Mensagem para solicitar a criação de um novo leilão.
│    │   ├── DirectMessage.java             // Mensagem para comunicação direta entre clientes (P2P).
│    │   ├── KeepAliveMessage.java          // Mensagem para manter a conexão ativa e evitar timeouts.
│    │   ├── LoginMessage.java              // Mensagem para autenticar um cliente no servidor.
│    │   ├── LoginResponseMessage.java      // Mensagem de resposta do servidor sobre o status do login.
│    │   ├── Message.java                   // Classe base para todas as mensagens trocadas no sistema.
│    │   ├── MessageType.java               // Enum que define os tipos de mensagens possíveis.
│    │   ├── PeerInfoRequestMessage.java    // Mensagem para solicitar informações P2P de outro cliente.
│    │   ├── PeerInfoResponseMessage.java   // Mensagem de resposta do servidor com informações P2P.
│    │   ├── PlaceBidMessage.java           // Mensagem para submeter um lance a um leilão.
│    │   ├── UserInfo.java                  // Armazena informações de um usuário (ID, nome, IP, porta P2P).
│    │   └── UserStatusUpdateMessage.java   // Mensagem para notificar mudança de status de um usuário.
│    ├── client/
│    │   ├── AuctionClient.java             // Lógica principal do cliente, gerencia conexões e estado local.
│    │   ├── ClientUI.java                  // Interface de usuário do cliente (baseada em console).
│    │   ├── PeerConnectionHandler.java     // Gerencia uma única conexão P2P com outro cliente.
│    │   ├── PeerListener.java              // Escuta por novas conexões P2P de outros clientes.
│    │   └── ServerListener.java            // Escuta mensagens do servidor principal.
│    └── server/
│        ├── AuctionServer.java             // Lógica principal do servidor, aceita clientes e gerencia leilões.
│        ├── AuctionManager.java            // Gerencia a criação, atualização e encerramento de leilões.
│        └── ClientHandler.java             // Ger"encia a comunicação com um único cliente conectado ao servidor.
└── out/                                                   // (Este diretório será criado após a compilação)

```

## ⚙️ Compilação e Execução

Certifique-se de que todos os arquivos `.java` estão organizados conforme a estrutura de pastas mostrada anteriormente.  
Você pode compilar e executar o projeto de duas maneiras:

---

### 🧩 1. Usando um IDE (IntelliJ IDEA, Eclipse, etc.)

Abra o projeto no IDE e execute o **build** normalmente.  
Depois, basta rodar a classe principal:

- **Servidor:** `AuctionServer`  
- **Cliente:** `AuctionClient`

💡 *Essa é a forma mais prática para desenvolvimento e testes rápidos.*

---

### 💻 2. Via Linha de Comando

No diretório raiz do projeto, execute os seguintes comandos:

#### 🔧 Compilar o Projeto

```bash
javac -d out src/main/java/com/auction/common/*.java \
             src/main/java/com/auction/server/*.java \
             src/main/java/com/auction/client/*.java
```

#### 🚀 Executar o Servidor

```bash
java -cp out com.auction.server.AuctionServer

💡 Você também pode iniciar o servidor diretamente pelo seu IDE.
```

---

### 👥 Executar Múltiplos Clientes

Abra quantos terminais quiser (ou consoles no IDE) e execute em cada um:

```bash
java -cp out com.auction.client.AuctionClient

Cada cliente solicitará um **nome de usuário** para login.
```

---

### 💬 Comandos Disponíveis no Cliente

| Comando         | Descrição                                   |
|-----------------|---------------------------------------------|
| `lsauctions`    | Lista todos os leilões ativos               |
| `createauction` | Cria um novo leilão                         |
| `bid`           | Realiza um lance em um item                 |
| `lsonline`      | Mostra os usuários online                   |
| `chat`          | Envia mensagens entre usuários              |
| `help`          | Mostra todos os comandos disponíveis        |
| `exit`          | Encerra a aplicação                         |
