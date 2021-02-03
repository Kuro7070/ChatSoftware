import org.json.simple.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Hauptklasse für die Anwendung des ChatClients
 */

public class Server {

    private ServerSocket serverSocket;
    InputStreamReader fileInputStream = new InputStreamReader(System.in);
    BufferedReader bufferedReader = new BufferedReader(fileInputStream);
    private List<Socket> socketList = new ArrayList<>();
    private final int TIMETOLIVE = 5;
    private String username;
    private RoutingTabelle routingTabelle;
    private ExecutorService pool;
    private String IP;
    private int port;
    private final String TOPORT = "toPort";
    private final String TOIP = "toIP";
    private File logFile;
    private final String MESSAGE = "message";

    public void start(int port, int poolSize, String username) {

        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.out.println("Es gab ein Problem mit der Ip Adresse im InitialenEintrag");
            System.out.println(e);
        }

        try {
            logFile = new File("logFile" + username + ".txt");
            if (logFile.createNewFile()) {
                System.out.println("File created: " + logFile.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        try {
            serverSocket = new ServerSocket(port);
            System.out.println(serverSocket.getInetAddress());
            System.out.println("Server ist online...");
            pool = Executors.newFixedThreadPool(poolSize);
            pool.execute(new Connection(serverSocket, this));
            this.username = username;

            routingTabelle = new RoutingTabelle(port, username, this);
            this.port = port;

            // System.out.println("gehe über zur Nachrichten überprüfung");

            while (true) {

                //Nachrichten eingang
                synchronized (socketList) {
                    for (Socket socketElem : socketList) {
                        try {
                            checkIncommingMessage(pool, socketElem);
                        } catch (IOException e) {
                            System.out.println("Beim prüfen der Nachrichten gabe es einen Fehler Socket: " + socketElem);
                        }
                    }
                }

                //Überprüfe ausgehende Nachricht
                if (bufferedReader.ready()) {

                    send(bufferedReader.readLine(), socketList, true);
                }

            }
        } catch (Exception e) {
            System.out.println(e + " Server ist abgestürzt");
        }
    }


    public RoutingTabelle getRoutingTabelle() {
        return routingTabelle;
    }



    public void writeLogFile(String message) {
        try {
            FileWriter myWriter = new FileWriter("logFile" + username + ".txt", true);
            String temp = "\n".concat(message).concat("\n");
            myWriter.write(temp);
            myWriter.close();

        } catch (IOException e) {
            System.out.println("Es gab einen Fehler beim bearbeiten des LogFiles");
            e.printStackTrace();
        }

    }

    public int getPort() {
        return port;
    }

    public void weiterleitenMessage(JSONObject message) {

        RoutingEintrag eintrag = getRoutingTabelle().getRoutingListe().stream().filter(elem ->
                elem.getPort() == Integer.parseInt(message.get(TOPORT).toString())
                        && elem.getIp().equals(message.get(TOIP).toString()))
                .findAny()
                .orElse(null);


        if (eintrag != null) {
            List<Socket> tempSocketList = new ArrayList<>();
            tempSocketList.add(eintrag.getEingangsSocket());
            send(message.toString(), tempSocketList, true);
        } else {
            System.out.println("Nachricht konnte nicht weitergeleitet werden an: " + eintrag.getUsername());
        }

    }

    public int getTIMETOLIVE() {
        return TIMETOLIVE;
    }


    public void addSocketList(Socket socket) {

        synchronized (socketList) {
            socketList.add(socket);
        }
    }

    public void sendRoutingInformation(Socket socket) {
        //Routing tabelle an neu verbundenen client senden
        List<Socket> tempSocketList = new ArrayList<>();
        tempSocketList.add(socket);

        send(routingTabelle.formatRoutingTableAsMessage(), tempSocketList, false);
    }


    public List<Socket> getSocketList() {
        return socketList;
    }

    public String getIP() {
        return IP;
    }

    //Sendet eine Nachricht
    //Mode = true -> normale Chatnachricht
    //     = false -> Routing Nachricht
    public void send(String message, List<Socket> socket, boolean mode) {

        pool.execute(new Message(socket, message, this, mode));
    }


    //prüft einkommende Nachrichten von Clients
    private void checkIncommingMessage(ExecutorService pool, Socket elem) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(elem.getInputStream()));
        if (in.ready()) {
            //System.out.println("socket enthält nachricht, überpüfe in thread");
            pool.execute(new MessageHandler(elem, this));
        }
    }

    public String getUsername() {
        return username;
    }


    public RoutingEintrag getRoutingEintragByName(String name) {
        return routingTabelle.getRoutingListe().stream().filter(elem -> elem.getUsername().equals(name))
                .findAny()
                .orElse(null);

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start(7777, 10, "Patrick");
    }


}
