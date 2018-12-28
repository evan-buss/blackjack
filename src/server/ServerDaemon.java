package server;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerDaemon implements Runnable {
    private ServerSocket server = null;
    private List<String> args;

    TextField logArea;

    ServerDaemon(List<String> args) {
        this.args = args;
        logArea = Main.getLogArea();
    }

    ServerDaemon() {
        this.args = null;
    }

    public void run() {
        createServer();
    }


    public void createServer() {
        try {
            if (args == null) {
                // No port specified, create ServerSocket and bind to default port 50001
                server = new ServerSocket(50001);
            } else if (args.size() == 1) {
                // Create ServerSocket on specified port
                server = new ServerSocket(Integer.parseInt(args.get(0)));
            } else {
                System.out.println("Invalid input");
            }
        } catch (IOException ex) {
            System.err.println("Could not create a ServerSocket");
            ex.printStackTrace();
            System.exit(-1);
        }

        // Logic to get the computer's network address.
        // Because a computer can have multiple interfaces that are not simply IPs
        // the results need to be filtered
        try {
            int port = server.getLocalPort();

            ArrayList<String> interfaces = new ArrayList<>();
            // Get enumeration of all the computer's network devices addresses
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; ((Enumeration) n).hasMoreElements(); ) {
                NetworkInterface e = n.nextElement();
                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements(); ) {
                    InetAddress addr = a.nextElement();
                    interfaces.add(addr.getHostAddress());
                }
            }

            // Display the parsed addresses
            logArea.setText(logArea.getText() + "Server Addresses: ");
//            System.out.println("Server Addresses: ");
            for (String ip :
                    interfaces) {
                if (Character.isDigit(ip.charAt(0))) {
                    if (ip.charAt(0) != '0') {
//                        System.out.println("\tIP: " + ip + " Port: " + port);
                        logArea.setText(logArea.getText() + "\tIP: " + ip + " Port: " + port);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting server address");
            e.printStackTrace();
        }

        // Server is bound to a port, wait for connections in a loop
        waitForConnection(server);

        System.exit(0);
    }

    /**
     * Waits for incoming connections with the server. Once a client connection
     * is received, it accepts it and then creates a new thread to handle the
     * connection. This allows multiple clients to connect to the server
     * simultaneously
     *
     * @param server server socket that is bound to a port number
     */
    public static void waitForConnection(ServerSocket server) {

        Socket client = null;

        // Runtime hook to handle when a user presses Ctrl+C to close the server daemon
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Closing server socket...");
                    server.close();
                } catch (IOException e) {
                    System.err.println("Error closing server socket");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    System.err.println("Error occurred during shutdown hook");
                    e.printStackTrace();
                }
            }
        });

        // infinite loop waiting for new connections in main thread'
        // Close server daemon with Ctrl-C
        while (true) {
            try {
                // Accept any incoming connections
                client = server.accept();
            } catch (IOException ex) {
                System.err.println("ServerSocket: Error Connecting with Client");
                ex.printStackTrace();
            }

            //    Only execute if client has actually connected (!= null)
            if (client != null) {
                // Create a new thread and pass it the client connection socket
                // connection
                new Thread(new ClientConnection(client)).start();
            }
        }
    }

}
