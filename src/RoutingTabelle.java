import com.google.gson.Gson;
import org.json.simple.parser.ParseException;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse repräsentiert Routingtabelle
 * und enthält Methoden zum Update
 */
public class RoutingTabelle {

    Pattern pattern = Pattern.compile("\\{[^}]*\\}");


    private List<RoutingEintrag> routingListe;
    private final String NAME = "name";
    private final String IP = "ip";
    private final String PORT = "port";
    private final String HOPCOUNT = "hopCount";
    private Server server;
    private String ip;

    public RoutingTabelle(int port, String username, Server server) {
        routingListe = new ArrayList<>();
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.out.println("Es gab ein Problem mit der Ip Adresse im InitialenEintrag");
            System.out.println(e);
        }
        RoutingEintrag initialerEintrag = new RoutingEintrag(ip, port, username, 0, null);
        synchronized (routingListe) {
            routingListe.add(initialerEintrag);
        }
        this.server = server;
    }


    public void updateRoutingTabelle(String message, Socket eingangsSocket) throws ParseException {

        List<HashMap<String, String>> routingMessage = new ArrayList<>();

        Matcher matcher = pattern.matcher(message);

        boolean updated = false;
        //[{"ip":"123123213","port": "5555","name": "kuro","hopCount":"2"},{"ip":"7777777777777","port": "4444","name": "marcelo","hopCount":"4"}]

        while (matcher.find()) {
            String match = matcher.group(0);            // {"ip":"123123213","port": "5555","name": "kuro","hopCount":"2"}

            match = match.replaceAll("\\{", "");
            match = match.replaceAll("\\}", "");
            match = match.replaceAll("\"", "");

            HashMap<String, String> tempMap = new HashMap<String, String>();


            String[] keyvalues = match.split(",");

            for (String keyvalue : keyvalues) {
                String key = keyvalue.split(":")[0];
                String value = keyvalue.split(":")[1];

                tempMap.put(key, value);
            }
            routingMessage.add(tempMap);


        }


        for (HashMap<String, String> map : routingMessage) {


            RoutingEintrag aktuellerEintrag = routingListe.stream()
                    .filter(eintrag ->
                            eintrag.getPort() == Integer.parseInt(map.get(PORT))
                                    && eintrag.getIp().equals(map.get(IP)))
                    .findAny()
                    .orElse(null);


//Eintrag noch nicht vorhanden
            if (aktuellerEintrag == null) {
                updated = true;
                String neueIP = map.get(IP);
                int neuerPort = Integer.parseInt(map.get(PORT));
                String neuerName = map.get(NAME);
                int neuerHopCount = (Integer.parseInt(map.get(HOPCOUNT)) + 1);


                RoutingEintrag neuerEintrag = new RoutingEintrag(neueIP, neuerPort, neuerName, neuerHopCount, eingangsSocket);

                synchronized (routingListe) {
                    routingListe.add(neuerEintrag);
                }

            } else {

//Eintrag vorhanden, wird geupdatet
                int neuerHopCount = (Integer.parseInt(map.get(HOPCOUNT)) + 1);
                int neuerPort = Integer.parseInt(map.get(PORT));

                if (aktuellerEintrag.getHopCount() > neuerHopCount) {
                    updated = true;
                    aktuellerEintrag.setHopCount(neuerHopCount);
                    aktuellerEintrag.setPort(neuerPort);
                    aktuellerEintrag.setEingangsSocket(eingangsSocket);

                }
            }
        }

        if (updated) {
            //senden der RoutingInfomraitonen an alle Nachbarn
            String formatedMessage = formatRoutingTableAsMessage();
            server.send(formatedMessage, server.getSocketList(), false);

        }


    }

    public List<RoutingEintrag> getRoutingListe() {
        return routingListe;
    }

    public String formatRoutingTableAsMessage() {
        String erg = "[";
        List<String> jsonStrings = new ArrayList<>();

        synchronized (routingListe) {
            for (RoutingEintrag eintrag : routingListe) {
                String temp = "{" +
                        "\"ip\":\"" + eintrag.getIp() + "\"," +
                        "\"port\":\"" + eintrag.getPort() + "\"," +
                        "\"name\":\"" + eintrag.getUsername() + "\"," +
                        "\"hopCount\":\"" + eintrag.getHopCount() + "\"}";

                jsonStrings.add(temp);
            }
        }

        for (String elem : jsonStrings) {
            erg = erg.concat(elem + ",");
        }
        erg = erg.substring(0, erg.length() - 1);
        erg = erg.concat("]");

        return erg;
    }

    public RoutingEintrag getRoutingEintragByUsername(String username){
        return routingListe.stream().filter(elem -> elem.getUsername().equals(username)).findAny().orElse(null);
    }

    public void showAllUsers(){
        synchronized (routingListe){
            for(RoutingEintrag eintrag : routingListe){
                System.out.println(eintrag.getUsername());
            }
        }
    }

    @Override
    public String toString() {
        String routingInfos = "";
        Gson gson = new Gson();

        for (RoutingEintrag eintrag : routingListe) {
            routingInfos = routingInfos.concat(gson.toJson(eintrag)) + "\n";
        }

        return routingInfos;
    }


   /* public void deleteEintragBySocket(Socket socket){

        List<RoutingEintrag> removeListe =  new ArrayList<>();

        synchronized (routingListe){
            for (RoutingEintrag eintrag : routingListe){
                if(eintrag.getEingangsSocket() == socket) {
                    removeListe.add(eintrag);
                }
            }

            for (RoutingEintrag eintragToRemove : removeListe){
                routingListe.remove(eintragToRemove);
            }
        }

    }
*/
}


