/**
 * @author Patrick Zilke
 * @author Marcel Jankowski
 *
 * Klasse zum Starten und festlegen der Parameter
 */
public class script {
    public static void main(String[] args) {
        Server server = new Server();
        server.start(6666, 10, "Kuro");
    }
}
