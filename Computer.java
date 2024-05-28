import java.io.*;
import java.net.*;
import java.util.*;

public class Computer implements Runnable {

    private String name;
    private InetAddress address;
    private int port;
    private InetAddress networkMask;
    private Map<String, String> arpTable;
    private DatagramSocket socket;
    private Set<String> receivedMessageIds = new HashSet<>(); // Set to store received message IDs

    // Constructor uses ConfigManager to set properties
    public Computer(String name, ConfigManager configManager) throws UnknownHostException, SocketException {
        this.name = name;
        this.address = InetAddress.getByName(configManager.getIpAddress(name));
        this.port = configManager.getPort(name);
        this.networkMask = InetAddress.getByName(configManager.getSubnetMask(name));
        this.arpTable = new HashMap<>(configManager.getARPTable());
        this.socket = new DatagramSocket(this.port, this.address);
    }

    public void run() {
        listenForMessages();
        interactWithUser();
    }

    private void interactWithUser() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nEnter the destination IP or 'exit' to quit: ");
                String destIp = scanner.nextLine();
                if ("exit".equalsIgnoreCase(destIp)) break;

                System.out.print("\nEnter the destination port: ");
                int destPort = Integer.parseInt(scanner.nextLine());

                System.out.print("\nEnter the message: ");
                String message = scanner.nextLine();
                String messageId = UUID.randomUUID().toString(); // Generate unique message ID
                sendMessage(destIp, destPort, message, "original", messageId);
            }
        } catch (IOException e) {
            System.err.println("\nError sending message: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    handleReceivedMessage(received, packet.getAddress(), packet.getPort());
                }
            } catch (IOException e) {
                System.err.println("\nError receiving messages: " + e.getMessage());
            }
        }).start();
    }

    private void handleReceivedMessage(String message, InetAddress sourceAddress, int sourcePort) {
        System.out.println("\n" + name + " received: " + message);

        // Extract the message ID
        String[] parts = message.split("\\|");
        if (parts.length < 2) return;
        String messageId = parts[0];
        String messageContent = parts[1];

        // Check if the message has already been received
        if (receivedMessageIds.contains(messageId)) return;

        // Add the message ID to the received set
        receivedMessageIds.add(messageId);

        // Check if the message is an original and needs a reply
        if (messageContent.contains("original")) {
            try {
                sendMessage(sourceAddress.getHostAddress(), sourcePort, "Received your message: " + messageContent, "reply", messageId);
            } catch (IOException e) {
                System.err.println("\nError sending reply: " + e.getMessage());
            }
        }
    }

    private void sendMessage(String destIp, int destPort, String message, String type, String messageId) throws IOException {
        InetAddress destAddress = InetAddress.getByName(destIp);
        String destMac;
        if (isSameSubnet(destAddress)) {
            destMac = arpTable.getOrDefault(destIp, "FF:FF:FF:FF:FF:FF"); // Use specific MAC or broadcast
        } else {
            destIp = "192.168.1.1"; // Gateway IP should be dynamically retrieved or configured
            destMac = arpTable.getOrDefault(destIp, "FF:FF:FF:FF:FF:FF"); // Gateway MAC or broadcast
        }

        String fullMessage = "\n" + messageId + "|" + (name + " (" + type + ") says to " + destMac + ": " + message);
        byte[] data = fullMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, destPort);
        socket.send(packet);
        System.out.println("\n" + name + " sent (" + type + ") message to " + destIp + ":" + destPort + ": " + message);
    }

    private boolean isSameSubnet(InetAddress destIp) {
        byte[] srcIpBytes = this.address.getAddress();
        byte[] destIpBytes = destIp.getAddress();
        byte[] maskBytes = this.networkMask.getAddress();

        for (int i = 0; i < srcIpBytes.length; i++) {
            if ((srcIpBytes[i] & maskBytes[i]) != (destIpBytes[i] & maskBytes[i])) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("\nUsage: java Computer <Name>");
            return;
        }
        String name = args[0];

        try {
            ConfigManager configManager = new ConfigManager("config.txt"); // Make sure this can handle dynamic inputs if needed
            Computer computer = new Computer(name, configManager);
            new Thread(computer).start();
        } catch (Exception e) {
            System.err.println("\nInitialization error: " + e.getMessage());
        }
    }
}
