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
    final int clientRequestTimeout = 5000; // ms

    /**
     * Available endpoints:
     * <ol>
     *  <li><b>10.0.2.2:8080</b> - use for emulators</li>
     *  <li>
     *      <b>localhost:8080</b> - use for android device (use <code>adb reverse tcp:8080 tcp:8080</code> for port mapping)
     *      See: <a href="https://trello.com/c/ZAte5H6T/47-port-mapping-%D0%BC%D0%B5%D0%B6%D0%B4%D1%83-android-%D1%83%D1%81%D1%82%D1%80%D0%BE%D0%B9%D1%81%D1%82%D0%B2%D0%BE%D0%BC-%D0%B8-%D0%BF%D0%BA">Trello card about port-mapping</a>
     *  </li>
     *  <li><b>192.168.0.103:8080</b> - use for Wi-Fi LAN (server must listen to <b>192.168.0.103:8080</b>)</li>
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

    public int getClientRequestTimeout() {
        return clientRequestTimeout;
    }
}
