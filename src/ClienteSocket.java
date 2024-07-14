import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class ClienteSocket {

    private final Socket socket;             //Conexão de socket com um cliente
    private final BufferedReader in;         //Usada para ler dados recebidos do cliente
    private final PrintWriter out;           //Usado para enviar dados para o cliente
    private String nome;                     //Usada para armazenar o nome / username informado pelo cliente
    private boolean bloqueioLance = false;   //Usado para bloquear ou desbloquear o lance de um cliente durante o leilão

    public ClienteSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));   //Inicializa a variável in criando um BufferedReader que lê dados do InputStream associado ao socket. É usado para ler mensagens do cliente.
        this.out = new PrintWriter(socket.getOutputStream(), true);  //Cria buffer que enviara as mensagens do cliente para o servidor através de um canal (OutputStream)

    }

    public String getMessage() {
        try {
            return in.readLine();
        } catch (IOException exception) {
            return null;
        }
    }

    public boolean sendMessage(String message) {
        out.println(message);
        return !out.checkError();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException exception) {
            System.out.println("Erro ao fechar socket: " + exception.getMessage());
        }
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public boolean isBloqueioLance() {
        return bloqueioLance;
    }

    public void setBloqueioLance(boolean bloqueioLance) {
        this.bloqueioLance = bloqueioLance;
    }
}
