# Requisitos do Sistema

## Requisitos de Software

- JDK (Java Development Kit) 8 ou superior
  - Necessário para compilar e executar o projeto
  - Testado com OpenJDK e Oracle JDK

## Requisitos de Sistema

### Servidor

- Memória: 256MB RAM mínimo
- Porta TCP: 12345 (configurada em `Constants.java`)
- Sistema Operacional: Qualquer SO com suporte a Java (Windows, Linux, macOS)
- Rede: IP fixo ou configuração de DNS para que os clientes possam conectar

### Cliente

- Memória: 128MB RAM mínimo
- Portas TCP: 13000-13100 (range para comunicação P2P, configurado em `Constants.java`)
- Sistema Operacional: Qualquer SO com suporte a Java
- Terminal: Suporte a UTF-8 para exibição correta dos caracteres

## Requisitos de Rede

### LAN (Rede Local)

- Comunicação TCP habilitada entre as máquinas
- Firewall configurado para permitir as portas necessárias:
  - Porta do servidor (12345)
  - Range de portas P2P (13000-13100)

### WAN (Internet)

Para funcionamento pela Internet, necessário:

- Port forwarding no roteador para:
  - Porta do servidor (12345) na máquina do servidor
  - Range de portas P2P (13000-13100) nas máquinas dos clientes
- IP público fixo ou serviço de DNS dinâmico configurado

## Dependências do Projeto

O projeto utiliza apenas bibliotecas padrão do JDK:

- java.net.* - Para comunicação via sockets
- java.io.* - Para serialização de objetos e I/O
- java.util.concurrent.* - Para gerenciamento de threads e tarefas agendadas
- java.util.* - Para coleções e utilidades gerais

Não são necessárias bibliotecas externas ou gerenciadores de dependências.