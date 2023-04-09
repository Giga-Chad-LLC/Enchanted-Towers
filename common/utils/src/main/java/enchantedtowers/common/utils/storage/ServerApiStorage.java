package enchantedtowers.common.utils.storage;

public class ServerApiStorage {
    static private ServerApiStorage instance = null;
    static public ServerApiStorage getInstance() {
        if (instance == null) {
            instance = new ServerApiStorage();
        }
        return instance;
    }

    // member fields
    final String serverHost;
    final String clientHost;
    final int port;

    /**
     * Available endpoints:
     * <ol>
     *  <li><b>10.0.2.2:8080</b> - use for emulators</li>
     *  <li><b>localhost:8080</b> - use for android device</li>
     *  <li><b>192.168.0.103:8080</b> - use for Wi-Fi LAN (if server must listen to <b>192.168.0.103:8080</b>)</li>
     * </ol>
    */
    private ServerApiStorage() {
        serverHost = "localhost";
        clientHost = "10.0.2.2"; // emulators
        // port is common for both client and server
        port = 8080;
    }

    public String getServerHost() {
        return serverHost;
    }

    public String getClientHost() {
        return clientHost;
    }

    public int getPort() {
        return port;
    }
}
