import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Cliente implements Runnable {

    private ClienteSocket clientSocket;
    private Scanner scanner;

    public Cliente() {
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) throws IOException {
        try {
            Cliente client = new Cliente();
            client.init();
        } catch (IOException exception) {
            System.out.println("Erro ao iniciar cliente " + exception.getMessage());

        }
    }

    /**
     * Função usada para inicalizar um cliente, estabelecendo uma conexão com o servidor a partir do host e porta informada
     * @throws IOException
     */
    private void init() throws IOException {
        try {
            Scanner scan = new Scanner(System.in);

            System.out.println("Digite o IP do servidor (IP Local: 127.0.0.1) ou localhost:");
            String host = scan.nextLine();

            System.out.println("Digite a porta");
            int porta = scan.nextInt();

            clientSocket = new ClienteSocket(new Socket(host, porta));
            System.out.println("Cliente conectado ao servidor em " + host + ":" + porta);
            username();

            new Thread(this).start();   //Cria uma instancia de Cliente como Thread e roda o método run
            messageLoop();                    //Chama o método que envia mensagens para o servidor
        }  finally {
            clientSocket.close();
        }
    }

    /**
     * Método que solicita o nome/username do cliente
     */
    private void username() {
        System.out.print("Digite seu nome: ");
        final String nome = scanner.nextLine();
        clientSocket.setNome(nome);
        clientSocket.sendMessage(nome);
    }


    /**
     * Método que contem um loop que aguarda a entrada do cliente e envia mensagens para o servidor
     * @throws IOException
     */
    private void messageLoop() throws IOException {
        String message;
        do {
            //System.out.println("Digite uma mensagem (ou sair para finalizar): ");
            message = scanner.nextLine();
            clientSocket.sendMessage(message);
        } while(!message.equalsIgnoreCase("sair"));
    }

    /**
     * O método lida com a recepção de mensagens do servidor de maneira contínua e assíncrona.
     * Ele executa em uma thread separada para que o cliente possa receber mensagens enquanto continua a executar outras operações
     */
    @Override
    public void run() {
        String message;
        while ((message = clientSocket.getMessage()) != null) {
            System.out.println(message);   //MENSAGEM RECEBIDA DO SERVIDOR: MESSAGE
        }
    }
}
