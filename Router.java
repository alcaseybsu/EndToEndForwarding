import java.io.*;
import java.net.*;
import java.util.*;

public class Router implements Runnable {

    private String name;
    private InetAddress ipAddress;
    private int port;
    private DatagramSocket socket;
    private TreeMap<String, String> routingTable; // Subnet to next-hop IP:Port, sorted by prefix length
    private Map<String, String> arpTable = new HashMap<>();
    @SuppressWarnings("unused")
    private ConfigManager configManager;
    private Scanner scanner = new Scanner(System.in);

    public Router(String name, ConfigManager configManager) throws UnknownHostException, SocketException {
        this.name = name;
        this.configManager = configManager;
        this.ipAddress = InetAddress.getByName(configManager.getIpAddress(name));
        this.port = Integer.parseInt(configManager.getMacAddress(name).split(":")[1]);
        this.socket = new DatagramSocket(port, ipAddress);
        this.routingTable = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(n -> n));
        this.arpTable.putAll(configManager.getARPTable(name)); // Assuming ConfigManager provides ARP table
        this.routingTable.putAll(configManager.getRoutingTable(name));
    }

    public void run() {
        System.out.println("Router " + name + " is running.");
        new Thread(this::listenForPackets).start();
        listenForCommands();
    }

    private void listenForPackets() {
        byte[] buffer = new byte[2048]; // Adjust buffer size based on expected packet size
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                socket.receive(packet); // Receive a packet
                handlePacket(packet); // Process the received packet
            }
        } catch (IOException e) {
            System.err.println("Error receiving packets: " + e.getMessage());
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void listenForCommands() {
        while (true) {
            System.out.println("Enter command (trace route, show table, exit):");
            String command = scanner.nextLine();
            if ("exit".equalsIgnoreCase(command)) break;

            switch (command) {
                case "trace route":
                    System.out.println("Enter destination IP:");
                    String destIP = scanner.nextLine();
                    String nextHop = findNextHop(destIP);
                    System.out.println("Next hop for " + destIP + " is " + nextHop);
                    break;
                case "show table":
                    showTables();
                    break;
                default:
                    System.out.println("Unknown command");
                    break;
            }
        }
    }

    private String findNextHop(String destIP) {
        for (Map.Entry<String, String> entry : routingTable.entrySet()) {
            if (destIP.startsWith(entry.getKey())) { // Simulating prefix match
                return entry.getValue();
            }
        }
        return null;
    }

    private void showTables() {
        System.out.println("Routing Table:");
        routingTable.forEach((key, value) -> System.out.println(key + " -> " + value));
        System.out.println("ARP Table:");
        arpTable.forEach((key, value) -> System.out.println(key + " -> " + value));
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        String receivedData = new String(packet.getData(), 0, packet.getLength()).trim();
        String[] parts = receivedData.split("\\|");
        if (parts.length < 3) return;

        String destIP = parts[1];
        String payload = parts[2];
        routePacket(destIP, payload);
    }

    private void routePacket(String destIP, String payload) throws IOException {
        String nextHop = findNextHop(destIP);
        if (nextHop != null) {
            String[] parts = nextHop.split(":");
            InetAddress nextHopIp = InetAddress.getByName(parts[0]);
            int nextHopPort = Integer.parseInt(parts[1]);
            DatagramPacket forwardPacket = new DatagramPacket(
                payload.getBytes(),
                payload.length(),
                nextHopIp,
                nextHopPort
            );
            socket.send(forwardPacket);
            System.out.println("[" + name + "] Packet routed to " + destIP + " via " + nextHop);
        } else {
            System.out.println("[" + name + "] No route found for " + destIP);
        }
    }

    public static void main(String[] args) {
        try {
            // Example instantiation and running of the Router
            ConfigManager configManager = new ConfigManager("config.txt"); // Assumed ConfigManager handles config file
            Router router = new Router("R1", configManager);
            router.run();
        } catch (Exception e) {
            System.err.println("Router setup error: " + e.getMessage());
        }
    }
}
