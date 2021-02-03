import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse für die ankommenden Nachrichten
 * Enthält json-Formatter
 */
public class MessageHandler implements Runnable {


    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final String ACK = "ACK";
    private final String ANSWERFLAG = "answerFlag";
    private final String FROMNAME = "fromName";
    private final String TOPORT = "toPort";
    private final String TOIP = "toIP";
    private final String TONAME = "toName";
    private final String MESSAGE = "message";
    private final String FROMIP = "fromIP";
    private final String FROMPORT = "fromPort";
    private final String TTL = "ttl";

    private Server server;


    public MessageHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            String read = "";
            if (in.ready()) {
                read = in.readLine();

                if (read.contains(ANSWERFLAG)) {

                    JSONParser parser = new JSONParser();
                    JSONObject message = (JSONObject) parser.parse(read);

                    //Bestätigungs Message
                    if (message.get(ANSWERFLAG).toString().equals("1")) {
                        if(Integer.parseInt(message.get(TOPORT).toString()) == server.getPort() && message.get(TOIP).equals(server.getIP())){
                            System.out.println("Ihre Nachricht an: " + message.get(FROMNAME) + " ist angekommen");
                        }
                        else{
                            server.weiterleitenMessage(message);
                        }
                        server.writeLogFile(message.toString());

                    }
                    //Nachricht versenden
                    else if (message.get(ANSWERFLAG).toString().equals("0")) {

                        //Nachricht erhalten
                        if (message.get(TOIP).equals(server.getIP()) && Integer.parseInt(message.get(TOPORT).toString()) == server.getPort()) {
                            System.out.println("Sie haben eine neue Nachricht von: " + message.get(FROMNAME));

                            //Eintragung der Nachricht ins Log
                            server.writeLogFile(message.toString());


                            //ACKMessage senden

                            for (RoutingEintrag eintrag : server.getRoutingTabelle().getRoutingListe()) {
                                if (eintrag.getIp().equals(message.get(FROMIP)) && eintrag.getPort() == Integer.parseInt(message.get(FROMPORT).toString())) {

                                    //Eintragung der Nachricht in den Chat
                                    eintrag.addChat(message.get(MESSAGE).toString());
                                    String verpackteNachricht = generateMessageFormat(eintrag);

                                    //Eintragung der Nachricht ins Log
                                    server.writeLogFile(verpackteNachricht);
                                    out.println(verpackteNachricht);
                                    break;

                                }
                            }
                            out.flush();

                        } else {
                            //Nachricht weiterleiten
                            if(!(Integer.parseInt(message.get(TTL).toString()) <= 0)){
                                message.put(TTL, (Integer.parseInt(message.get(TTL).toString()) - 1));
                                server.weiterleitenMessage(message);
                                //Eintragung der Nachricht ins Log
                                String weiterleitMessageLogFile = "\n####### Weiterleitung an: " + message.get(TONAME) + " #######\n".concat(message.toString());
                                server.writeLogFile(weiterleitMessageLogFile);
                            }
                            else{
                                //Eintragung der Nachricht ins Log
                                String weiterleitMessageLogFile = "\n####### Nachricht gestorben an: " + message.get(TONAME) + " #######\n".concat(message.toString());
                                server.writeLogFile(weiterleitMessageLogFile);
                            }
                        }
                    }
                    else{
                        System.out.println("Nachricht ohne Protokoll erhalten");
                        System.out.println(message);
                    }

                    //Routing Nachricht
                }
                else if (!read.contains(ANSWERFLAG)) {

                    //Eintragung der Nachricht ins Log
                    String weiterleitMessageLogFile = "\n####### Routing Informationen #######\n".concat(read);
                    server.writeLogFile(weiterleitMessageLogFile);

                    //aktualisiere RoutingTabelle
                    server.getRoutingTabelle().updateRoutingTabelle(read, socket);
                }
                else{
                    System.out.println("Nachricht ohne Answerflag erhalten");
                }

            }

        } catch (IOException | ParseException e) {

            if(e instanceof IOException)
                System.out.println("Es besteht keine Vebrindung keine Socket: " + socket);
            else System.out.println("Problem beim lesen der Nachricht vom Empfänger,  Nachricht nicht im JSON Format");

        }
    }

    public String generateMessageFormat(RoutingEintrag eintrag) {
        return "{" +
                "\"answerFlag\":\"" + 1 + "\"," +
                "\"toIP\":\"" + eintrag.getIp() + "\"," +
                "\"toPort\":\"" + eintrag.getPort() + "\"," +
                "\"toName\":\"" + eintrag.getUsername() + "\"," +
                "\"fromIP\":\"" + server.getIP() + "\"," +
                "\"fromPort\":\"" + server.getPort() + "\"," +
                "\"fromName\":\"" + server.getUsername() + "\"," +
                "\"ttl\":\"" + server.getTIMETOLIVE() + "\"," +
                "\"timeStamp\":\"" + Instant.now().getEpochSecond() + "\"," +
                "\"message\":\"\"}";
    }

    private void stopConnection() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println("Could not close the server/client: ");
            e.printStackTrace();
        }
    }
}
