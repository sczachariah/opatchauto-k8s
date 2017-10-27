package oracle.fmwplatformqa.opatchautoqa.util;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;

/**
 * Finds an available port on localhost.
 */
public class PortFinder {
    /**
     * If you only need the one port you can use this. No need to instantiate the class
     */

    public final static Integer MIN_PORT_NUMBER = 0;
    public final static Integer MAX_PORT_NUMBER = 65535;

    public static int findFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        try {
            return socket.getLocalPort();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private static HashSet<Integer> used = new HashSet<Integer>();

    /**
     * Finds a port that is currently free and is guaranteed to be different from any of the port numbers previously
     * returned by this PortFinder instance.
     */
    public synchronized static int setNextAvailableUniquePort() throws IOException {
        int port;

        do {
            port = findFreePort();
            if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
                throw new IllegalArgumentException("Invalid port numbers: " + port);
            }
        } while (used.contains(port));
        used.add(port);
        return port;
    }
}
