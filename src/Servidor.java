import java.io.*;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Servidor {
    public static int porta;
    private ServerSocket serverSocket;
    private final List<ClienteSocket> clientes = new LinkedList<>();
    private final List<ClienteSocket> participantesLeilao = new LinkedList<>();

    private enum EstadoLeilao {
        AGUARDANDO_PARTICIPANTES,
        LEILAO_INICIADO
    }

    private EstadoLeilao estadoLeilao = EstadoLeilao.AGUARDANDO_PARTICIPANTES;
    private int lanceAtual = 30000;
    private ClienteSocket vencedor;
    private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private boolean leilaoAgendado = false;

    public static void main(String[] args) throws IOException {
        try {
            System.out.print("Digite a porta do servidor: ");
            Scanner scan = new Scanner(System.in);
            porta = scan.nextInt();
            Servidor servidor = new Servidor();
            servidor.init();
        } catch (IOException exception) {
            System.out.println("Erro ao iniciar o servidor " + exception.getMessage());
        }
    }

    /**
     * Método que inicializa o servidor passando a porta para instanciar o serverSocket
     * @throws IOException
     */
    private void init() throws IOException {
        serverSocket = new ServerSocket(porta);
        System.out.println("Servidor inicializado na porta: " + porta);
        clientConnectionLoop();
    }

    /**
     * O método cria uma concexão de clientSocket para cada conexão de cliente aceita, e inicia uma thread para lidar com a comunicação do cliente
     * Além disso ele inclui o cliente que iniciou uma conexão em uma lista de clientes conectados.
     * @throws IOException
     */
    private void clientConnectionLoop() throws IOException {
        while (true) {
            ClienteSocket clienteSocket = new ClienteSocket(serverSocket.accept()); //aguarda conexão do cliente;
            System.out.println("Cliente " + clienteSocket.getRemoteSocketAddress() + " conectou");
            new Thread(() -> clienteMessageLoop(clienteSocket)).start();
            clientes.add(clienteSocket);
        }
    }

    /**
     * O método clienteMessageLoop trata as mensagens recebidas de um cliente específico, realiza ações com base nessas mensagens (como entrar ou sair do leilão)
     * e mantém o loop de comunicação enquanto o cliente não solicitar saída.
     * @param clienteSocket
     */
    private void clienteMessageLoop(ClienteSocket clienteSocket) {
        String message;
        try {
            while ((message = clienteSocket.getMessage()) != null) {
                if ("sair".equalsIgnoreCase(message)) {
                    System.out.println("Cliente " + clienteSocket.getRemoteSocketAddress() + " desconectou");
                    clientes.remove(clienteSocket);
                    return;
                }

                if(clienteSocket.getNome() == null){
                    clienteSocket.setNome(message);
                    System.out.println("Cliente "+ clienteSocket.getRemoteSocketAddress() + " logado como " + clienteSocket.getNome());

                    String msg = """
                            
                            ---------------------------- LEILÃO DE VEÍCULOS VIRTUAL ------------------------------
                            Ola, %s! Bem vindo ao leilão de veículos virtual, aqui voce encontra o carro que deseja!
                            Selecione um dos veículos anunciados para participar do leilão ou digite "sair" para sair.
                            Fizemos tanto sucesso que temos apenas um último veículo anunciado. 
                            1. CHEVROLET ONIX 10MT LT2 22/23
                            --------------------------------------------------------------------------------------
                            """.formatted(clienteSocket.getNome());
                    sendMessagetoCliente(clienteSocket, msg);

                }
                else {
                    if (vencedor != null && estadoLeilao == EstadoLeilao.AGUARDANDO_PARTICIPANTES) {
                        sendMessagetoCliente(clienteSocket,"O leilão foi finalizado. Digite sair para sair");
                        continue;
                    }

                    if (estadoLeilao == EstadoLeilao.LEILAO_INICIADO && participantesLeilao.contains(clienteSocket)) {
                        if (clienteSocket.isBloqueioLance()) {
                            sendMessagetoCliente(clienteSocket, "Você já deu um lance. Aguarde o próximo lance.");
                            continue;
                        } else {
                            try {
                                int lance = Integer.parseInt(message);
                                if (lance < lanceAtual)
                                    sendMessagetoCliente(clienteSocket, "O lance é menor que R$" + lanceAtual);
                                else if (lance == lanceAtual) {
                                    sendMessagetoCliente(clienteSocket, "O lance é igual ao lance atual");
                                } else {
                                    sendMessageToAllLeilao("O participante " + clienteSocket.getNome() + " deu um lance de R$" + lance);
                                    vencedor = clienteSocket;
                                    lanceAtual = lance;
                                    for (ClienteSocket participante : participantesLeilao) {
                                        participante.setBloqueioLance(false);
                                    }
                                    clienteSocket.setBloqueioLance(true);
                                    reiniciarTemporizador();
                                }
                            } catch (NumberFormatException exception) {
                                sendMessagetoCliente(clienteSocket, "Por favor, digite números válidos");
                            }
                        }
                    }

                    if (message.equals("1")) {
                        if (estadoLeilao == EstadoLeilao.LEILAO_INICIADO) {
                            sendMessagetoCliente(clienteSocket, "O leilão já foi iniciado. Não é possível participar agora."); // O leilão já começou, informe ao cliente e ignore as mensagens
                            continue;
                        }

                        if (participantesLeilao.contains(clienteSocket) && quantidadeDeParticipantes(participantesLeilao) == 1) {
                            sendMessagetoCliente(clienteSocket, "Você ja entrou no leilão. Aguarde a entrada de novos participantes para iniciarmos!");
                            continue;
                        }
                        if (participantesLeilao.contains(clienteSocket) && quantidadeDeParticipantes(participantesLeilao) > 1){
                            sendMessagetoCliente(clienteSocket, "Você ja entrou no leilão. Aguarde que ja iniciaremos!");
                            continue;
                        }

                        participantesLeilao.add(clienteSocket);
                        if (quantidadeDeParticipantes(participantesLeilao) > 1) {
                            sendMessagetoCliente(clienteSocket,"Aguarde que ja iniciaremos o leilão (Leva apenas 1min)");                             //Servidor envia uma mensagem para o cliente especificado no parâmetro
                            sendMessageToAllLeilaoExceptSender(clienteSocket,"Um novo participante entrou no leilão. Aguarde 1min para iniciarmos");  //Servidor envia uma mensagem para todos que estão participando do leilão com exceção daquele que entrou (especificado no parâmetro)
                            agendarInicioDoLeilao(clienteSocket);                                                                                             //Inicia uma contagem de 60s até dar inicio ao leilão
                        } else
                            sendMessagetoCliente(clienteSocket,"Apenas voce entrou no leilão, aguardando a entrada demais participantes");
                    }

                    System.out.println("Mensagem recebida de "+ clienteSocket.getNome() + clienteSocket.getRemoteSocketAddress() + ": " + message);
                }
            }
        } finally {
            clienteSocket.close();
        }
    }

    /**
     * Método que retorna a quantidade de clientes na lista de participantes do leilão
     */
    private int quantidadeDeParticipantes(List<ClienteSocket> participantes) {
        int contador = 0;
        for (ClienteSocket participante : participantes) {
            contador++;
        }
        return contador;
    }

    /**
     * Método usado para encaminhar a mensagem recebida de um cliente para todos os demais conectados ao servidor
     */
    private void sendMessageToAll(ClienteSocket sender, String message) {
        Iterator<ClienteSocket> iterator = clientes.iterator();
        while (iterator.hasNext()) {
            ClienteSocket cliente = iterator.next();
            if(!sender.equals(cliente)) {
                if (!cliente.sendMessage("Cliente " + sender.getRemoteSocketAddress() + ": " + message)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Método usado para enviar mensagem do servidor para todos os cliente participantes do leilão
     * @param message
     */
    private void sendMessageToAllLeilao(String message) {
        Iterator<ClienteSocket> iterator = participantesLeilao.iterator();
        while (iterator.hasNext()) {
            ClienteSocket cliente = iterator.next();
            if (!cliente.sendMessage(message)) {
                iterator.remove();
            }
        }
    }

    /**
     * Método que encaminha a mensagem recebida de um cliente para todos os demais que participam do leilão
     * @param sender
     * @param message
     */
    private void sendMessageToAllLeilaoExceptSender(ClienteSocket sender, String message) {
        Iterator<ClienteSocket> iterator = participantesLeilao.iterator();
        while (iterator.hasNext()) {
            ClienteSocket cliente = iterator.next();
            if(!sender.equals(cliente)) {
                if (!cliente.sendMessage(message)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Método que envia mensagem do servidor para o cliente especificado como parâmetro
     * @param sender
     * @param message
     */
    private void sendMessagetoCliente(ClienteSocket sender, String message) {
        sender.sendMessage(message);
    }

    /**
     * Método agenda o inicio do leilão para 1min, e reinicia caso um novo participante entre no leilão
     * @param clienteSocket
     */
    private void agendarInicioDoLeilao(ClienteSocket clienteSocket) {
        if (leilaoAgendado) {
            timer.shutdownNow();                                      // Cancela o timer
            timer = Executors.newScheduledThreadPool(1);  // Reinicia o timer
            sendMessageToAllLeilaoExceptSender(clienteSocket,"Um novo participante entrou no leilão. Aguarde mais 1min");  //Encaminha mensagem passada de um cliente para todos os demais participantes do leilão
        }

        timer.schedule(this::iniciarLeilao, 60, TimeUnit.SECONDS); // Agendar uma nova tarefa para iniciar o leilão após 60 segundos
        leilaoAgendado = true;
    }

    /**
     * Método que inicia o leilão (Muda o estado para iniciado)
     * Ao iniciar ele inicia uma timer para validar o tempo para dar um lance esgotou
     */
    private void iniciarLeilao() {
        estadoLeilao = EstadoLeilao.LEILAO_INICIADO; // Atualiza o estado para LEILAO_INICIADO
        String msg = """
                        
                        Iniciando o leilão do veículo CHEVROLET ONIX 10MT LT2 22/23!
                        A quantidade de participante é %d.
                        O lance mínimo para o veículo é de R$30.000
                        O tempo de espera para cada lance é de 1min       _____   
                                                                      ___/     \\___
                                                                      ———(O)——(O)———
                        """.formatted(quantidadeDeParticipantes(participantesLeilao));
        sendMessageToAllLeilao(msg);
        timer.shutdownNow();
        timer = Executors.newScheduledThreadPool(1);             // Reinicia o serviço de execução agendada
        timer.schedule(this::verificarVencedor, 60, TimeUnit.SECONDS);    // Agendar uma nova tarefa (verificarVencedor) para ser executada após 60 segundos
    }

    /**
     * Método que verifica quem foi o vencedor do leilão
     */
    private void verificarVencedor() {
        if (vencedor != null) {
            System.out.println("O leilão foi encerrado! O vencedor é: " + vencedor.getNome()); // Anuncia o vencedor
            sendMessageToAllLeilao("O leilão foi encerrado! O vencedor é: " + vencedor.getNome() + ". Digite sair para sair");
        } else {
            System.out.println("O leilão foi encerrado sem lances."); // Nenhum lance foi dado, encerra o leilão sem vencedor
            sendMessageToAllLeilao("O leilão foi encerrado sem lances. Digite 1 para tentar novamente ou digite sair para sair");

        }

        participantesLeilao.clear();
        estadoLeilao = EstadoLeilao.AGUARDANDO_PARTICIPANTES;
        timer.shutdown(); // Limpa o temporizador
    }

    /**
     * Método usado para reinicar o temporizador para checagem de vencedor do leilão
     */
    private void reiniciarTemporizador() {
        timer.shutdownNow();
        timer = Executors.newScheduledThreadPool(1);
        timer.schedule(this::verificarVencedor, 60, TimeUnit.SECONDS);
    }


}
