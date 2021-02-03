import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse für die ausgehende Nachrichten
 * Enthält json-Formatter
 */
public class Message implements Runnable {

    private List<Socket> socketList;
    private String message;
    private PrintWriter out;
    private Server server;
    private boolean messageMode;

    public Message(List<Socket> socketList, String message, Server server, boolean mode) {
        this.message = message;
        this.socketList = socketList;
        this.server = server;
        this.messageMode = mode;
    }

    @Override
    public void run() {

        //Normale Message senden
        if (messageMode) {

            String firstChar = message.substring(0, 1);

            //#send:<Username>:<Nachricht>
            if (firstChar.equals("#")) {
                //Eigene Befehlskette

                message = message.substring(1, message.length());
                String[] splittedNachricht = message.split(":");

                if (splittedNachricht[0].equals("send")) {
                    String username = splittedNachricht[1];
                    String nachricht = splittedNachricht[2];


                    RoutingEintrag targetEintrag = server.getRoutingEintragByName(username);
                    if (targetEintrag == null) {
                        System.out.println("Nachricht konnte nicht zugestellt werden, Sie haben keine Verbindung mit der gewünschten Person");
                    } else {
                        Socket targetSocket = targetEintrag.getEingangsSocket();
                        String verpackteNachricht = generateMessageFormat(nachricht, targetEintrag);

                        sendBySocket(targetSocket, verpackteNachricht);
                        targetEintrag.addChat(nachricht);
                    }
                } else if (splittedNachricht[0].equals("connect")) {
                    String ip = splittedNachricht[1];
                    int port = Integer.parseInt(splittedNachricht[2]);


                    Socket newSocket = null;

                    try {
                        newSocket = new Socket(ip, port);
                    } catch (IOException e) {
                        System.out.println("Verbindung mit: " + ip + " konnte nicht hergestellt werden");
                        e.printStackTrace();
                    }
                    System.out.println("Vebrindung mit: " + ip + " hergestellt");

                    if (newSocket != null) {
                        server.addSocketList(newSocket);
                        server.sendRoutingInformation(newSocket);
                    } else {
                        System.out.println("Neuer Socket konnte nicht der Socketliste hinzugefügt werden: " + ip + " - " + newSocket);
                    }
                } else if (splittedNachricht[0].equals("chat")) {
                    String username = splittedNachricht[1];
                    server.getRoutingEintragByName(username).showChat();
                }else if(splittedNachricht[0].equals("showAll")){
                    server.getRoutingTabelle().showAllUsers();
                } else if (splittedNachricht[0].equals("routing")) {
                    System.out.println(server.getRoutingTabelle().toString());
                }/* else if (splittedNachricht[0].equals("disconnect")) {

                        String username = splittedNachricht[1];
                        RoutingEintrag eintrag = server.getRoutingTabelle().getRoutingEintragByUsername(username);
                        if(eintrag != null){
                            server.getRoutingTabelle().deleteEintragBySocket(eintrag.getEingangsSocket());
                        }



                    }*/ else {
                    System.out.println("Befehl ist nicht bekannt");
                }


            } else {
                //Nachricht weiterleiten
                for (Socket s : socketList) {
                    sendBySocket(s, message);
                }
            }
        }
        //Wir senden routingTabelle eine Person
        else {

            for (Socket socket : socketList) {

                sendBySocket(socket, message);
            }


        }

    }


    public String generateMessageFormat(String tempMessage, RoutingEintrag eintrag) {
        String nachricht = "{" +
                "\"answerFlag\":\"" + 0 + "\"," +
                "\"toIP\":\"" + eintrag.getIp() + "\"," +
                "\"toPort\":\"" + eintrag.getPort() + "\"," +
                "\"toName\":\"" + eintrag.getUsername() + "\"," +
                "\"fromIP\":\"" + server.getIP() + "\"," +
                "\"fromPort\":\"" + server.getPort() + "\"," +
                "\"fromName\":\"" + server.getUsername() + "\"," +
                "\"ttl\":\"" + server.getTIMETOLIVE() + "\"," +
                "\"timeStamp\":\"" + Instant.now().getEpochSecond() + "\"," +
                "\"message\":\"" + tempMessage + "\"}";


        return nachricht;
    }


    public void sendBySocket(Socket socket, String nachricht) {

        //prüfe ob client existiert
        if (socket != null) {
            try {
                //sende an client Nachricht
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println(nachricht);

                server.writeLogFile(nachricht);

            } catch (IOException e) {
                System.out.println("Nachricht konnte nicht gesendet werden");
                e.printStackTrace();
            }
        } else {
            System.out.println("Sie haben keine Verbindung mit: ");
        }
    }


}
