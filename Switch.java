import java.io.*;
import java.net.*;
import java.util.*;

public class Switch implements Runnable {
    private String name;
    private InetAddress ipAddress;
    private int port;
    private DatagramSocket socket;
    private Map<String, String> forwardingTable = new HashMap<>(); // MAC to port mapping
    private List<String> neighbors; // Connected devices
    @SuppressWarnings("unused")
    private ConfigManager configManager;
    @SuppressWarnings("unused")
    private Map<String, String> routingTable = new HashMap<>();
    private Set<String> receivedMessageIds = new HashSet<>(); // Set to store received message IDs

    public Switch(String name, int port, ConfigManager configManager) throws IOException {
        this.name = name;
        this.configManager = configManager;
        this.ipAddress = InetAddress.getByName(configManager.getIpAddress(name));
        this.port = port;
        this.socket = new DatagramSocket(this.port, this.ipAddress);
        this.neighbors = configManager.getConnectedDevices(name); // Initialize neighbors based on ConfigManager
    }

    public void run() {
        System.out.println("\nSwitch " + name + " is running.");
        new Thread(this::listenForPackets).start();
        listenForCommands();
    }

    private void listenForCommands() {
        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("\nEnter command (show table, show neighbors, exit):");
                String command = scanner.nextLine().trim().toLowerCase();

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("\nExiting command listener.");
                    break;
                }

                switch (command) {
                    case "show table":
                        showTable();
                        break;
                    case "show neighbors":
                        showNeighbors();
                        break;
                    default:
                        System.out.println("\nUnknown command: " + command);
                        break;
                }
            }
        } finally {
            scanner.close();
        }
    }

    private void listenForPackets() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (IOException e) {
            System.out.println("\n[" + name + "] Error receiving packet: " + e.getMessage());
        } finally {
            socket.close();
        }
    }

    private void handlePacket(DatagramPacket packet) {
        String receivedData = new String(packet.getData(), 0, packet.getLength()).trim();
        String[] parts = receivedData.split("\\|");
        if (parts.length < 4) return;

        String messageId = parts[0];
        String srcMAC = parts[1];
        String destMAC = parts[2];
        String payload = parts[3];

        if (receivedMessageIds.contains(messageId)) return;
        receivedMessageIds.add(messageId);

        String sourceIP = packet.getAddress().getHostAddress();
        int sourcePort = packet.getPort();
        forwardingTable.put(srcMAC, sourceIP + ":" + sourcePort);

        forwardOrFlood(messageId, srcMAC, destMAC, payload);
    }

    private void forwardOrFlood(String messageId, String srcMAC, String destMAC, String payload) {
        String destination = forwardingTable.get(destMAC);
        if (destination != null) {
            forwardPacket(messageId, srcMAC, destMAC, payload, destination);
        } else {
            flood(messageId, srcMAC, destMAC, payload);
        }
    }

    private void forwardPacket(String messageId, String srcMAC, String destMAC, String payload, String destination) {
        try {
            String[] parts = destination.split(":");
            InetAddress destAddress = InetAddress.getByName(parts[0]);
            int destPort = Integer.parseInt(parts[1]);

            String fullMessage = messageId + "|" + srcMAC + "|" + destMAC + "|" + payload;
            DatagramPacket forwardPacket = new DatagramPacket(
                fullMessage.getBytes(),
                fullMessage.length(),
                destAddress,
                destPort
            );
            socket.send(forwardPacket);
            System.out.println("\n[" + name + "] Forwarded packet to " + destMAC + " at " + destination);
        } catch (IOException e) {
            System.out.println("\n[" + name + "] Error forwarding packet: " + e.getMessage());
        }
    }

    private void flood(String messageId, String srcMAC, String destMAC, String payload) {
        forwardingTable.forEach((mac, port) -> {
            if (!mac.equals(srcMAC)) {
                try {
                    String[] parts = port.split(":");
                    InetAddress address = InetAddress.getByName(parts[0]);
                    int portNumber = Integer.parseInt(parts[1]);

                    String fullMessage = messageId + "|" + srcMAC + "|" + destMAC + "|" + payload;
                    DatagramPacket floodPacket = new DatagramPacket(
                        fullMessage.getBytes(),
                        fullMessage.length(),
                        address,
                        portNumber
                    );
                    socket.send(floodPacket);
                    System.out.println("\n[" + name + "] Flooding packet to " + mac);
                } catch (IOException e) {
                    System.out.println("\n[" + name + "] Error flooding packet: " + e.getMessage());
                }
            }
        });
    }

    private void showTable() {
        System.out.println("\nForwarding Table:");
        if (forwardingTable.isEmpty()) {
            System.out.println("\nNo entries in forwarding table.");
        } else {
            forwardingTable.forEach((key, value) -> System.out.println(key + " -> " + value));
        }
    }

    private void showNeighbors() {
        System.out.println("\nNeighbors:");
        neighbors.forEach(neighbor -> System.out.println(neighbor));
    }

    public void setNeighbors(Map<String, String> neighbors) {
        this.neighbors = new ArrayList<>(neighbors.values());
        System.out.println("\nUpdated neighbors for Switch " + name + ": " + this.neighbors);
    }

    public void setRoutingTable(Map<String, String> routingTable) {
        this.routingTable = routingTable;
        System.out.println("\nUpdated routing table for Switch " + name + ": " + routingTable);
    }
}
