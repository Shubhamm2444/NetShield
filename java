import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class BasicFirewall {
    private static final Logger logger = Logger.getLogger(BasicFirewall.class.getName());
    private static final Map<String, Integer> connectionCounts = new HashMap<>();
    private static final int MAX_CONNECTIONS = 5;
    private static Set<String> allowedIPs = new HashSet<>();

    public static void main(String[] args) {
        loadConfig();
        int port = 8080; // Change to the port you want to monitor

        // Logging configuration
        setupLogger();

        // Start the firewall server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Basic Firewall is running on port " + port);
            logger.info("Firewall started on port " + port);
            System.out.println("Listening for incoming connections...");

            while (true) {
                // Accept incoming connections
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                logger.info("Connection accepted from: " + clientIP);

                // Implement logic to allow or block connections
                if (isAllowed(clientIP)) {
                    connectionCounts.put(clientIP, connectionCounts.getOrDefault(clientIP, 0) + 1);
                    if (connectionCounts.get(clientIP) > MAX_CONNECTIONS) {
                        System.out.println("Connection rate limit exceeded for: " + clientIP);
                        logger.warning("Connection rate limit exceeded for: " + clientIP);
                        clientSocket.close(); // Close the connection
                    } else {
                        System.out.println("Connection allowed: " + clientIP);
                        logger.info("Connection allowed: " + clientIP);
                        new Thread(new ClientHandler(clientSocket)).start(); // Handle connection
                    }
                } else {
                    System.out.println("Connection blocked: " + clientIP);
                    logger.warning("Connection blocked: " + clientIP);
                    clientSocket.close(); // Close the connection
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            logger.severe("Error: " + e.getMessage());
        }
    }

    private static boolean isAllowed(String ip) {
        return allowedIPs.contains(ip);
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            String allowedIP = properties.getProperty("allowedIP", "127.0.0.1");
            allowedIPs.add(allowedIP);
            logger.info("Loaded allowed IP: " + allowedIP);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            logger.warning("Error loading configuration: " + e.getMessage());
        }
    }

    private static void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("firewall.log", true);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Handle the client connection
            InputStream input = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break; // End of HTTP headers
            }

            // Send a response (for HTTP)
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Hello, World!");

            // Close the socket
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }
}
