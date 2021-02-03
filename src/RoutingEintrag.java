import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse für Routing-Einträge wie im Protokoll besprochen
 * Enthält eine Nachrichten-Anzeige und eine Vergleichs-Methode
 *
 */
public class RoutingEintrag {

    private String ip;
    private int port;
    private String username;
    private  int hopCount;
    private Socket eingangsSocket;
    private List<String> chat = new ArrayList<>();



    public RoutingEintrag(String ip, int port, String username, int hopCount, Socket eingangsSocket){
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.hopCount = hopCount;
        this.eingangsSocket = eingangsSocket;
    }



    public void showChat() {
        System.out.println("\n----------------------------------------");
        System.out.println("CHAT mit User: " + username + "\n");
        for (String elem : chat) {
            System.out.println(elem);
        }
        System.out.println("\n----------------------------------------\n");
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof RoutingEintrag)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        RoutingEintrag c = (RoutingEintrag) o;

        // Compare the data members and return accordingly
        return c.getIp().equals(getIp()) && c.getPort() == getPort();
    }

    public void addChat(String elem) {
        chat.add(elem);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public Socket getEingangsSocket() {
        return eingangsSocket;
    }

    public void setEingangsSocket(Socket eingangsSocket) {
        this.eingangsSocket = eingangsSocket;
    }
}
