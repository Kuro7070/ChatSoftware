import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse für die Verbindung
 */
public class Connection implements Runnable{

    private ServerSocket serverSocket;
    private Server server;

    public Connection(ServerSocket serversocket, Server server){
        this.serverSocket = serversocket;
        this.server = server;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket neuerSocket = serverSocket.accept();
                server.addSocketList(neuerSocket);
                System.out.println("Neue Verbindung im Netzwerk");
                server.sendRoutingInformation(neuerSocket);

            } catch (IOException e) {
                System.out.println("Socket konnte nicht aktzeptiert werden");
                e.printStackTrace();

            }
        }
    }
}
