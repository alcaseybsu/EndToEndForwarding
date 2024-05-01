import java.io.*;
import java.net.*;
import java.util.*;

public class Router {

  private String name;
  private InetAddress ipAddress;
  private int port;
  private DatagramSocket socket;
  private TreeMap<String, String> routingTable; // Subnet to next-hop IP:Port, sorted by prefix length

  @SuppressWarnings("unused")
  private ConfigManager configManager;

  private Scanner scanner = new Scanner(System.in);

  public Router(String name, ConfigManager configManager)
    throws UnknownHostException, SocketException {
    this.name = name;
    this.configManager = configManager;
    this.ipAddress = InetAddress.getByName(configManager.getIpAddress(name));
    this.port =
      Integer.parseInt(configManager.getMacAddress(name).split(":")[1]);
    this.socket = new DatagramSocket(port, ipAddress);
    this.routingTable =
      new TreeMap<>(
        Comparator.comparingInt(String::length).reversed().thenComparing(n -> n)
      );
    this.routingTable.putAll(configManager.getRoutingTable(name));
    this.arpTable = new HashMap<>(configManager.getARPTable(name)); // Assuming ConfigManager provides ARP table
    initializeArpTable();
  }

  public void run() {
    System.out.println("Router " + name + " is running.");
    listenForCommands();
    Thread listenThread = new Thread(this::listenForPackets);
    listenThread.start();
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

  




  private Map<String, String> arpTable = new HashMap<>();

  private void initializeArpTable() {
    // Example ARP entries initialization
    arpTable.put("192.168.1.10", "00:1A:2B:3C:4D:5E");
    // Add other ARP entries as needed
}

  private void showTables() {
    System.out.println("Routing Table:");
    routingTable.forEach((key, value) ->
      System.out.println(key + " -> " + value)
    );
    System.out.println("ARP Table:");
    arpTable.forEach((key, value) -> System.out.println(key + " -> " + value));
  }

  private void handlePacket(DatagramPacket packet) throws IOException {
    String receivedData = new String(packet.getData(), 0, packet.getLength())
      .trim();
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
      System.out.println(
        "[" + name + "] Packet routed to " + destIP + " via " + nextHop
      );
    } else {
      System.out.println("[" + name + "] No route found for " + destIP);
    }
  }

  public static void main(String[] args) {
    try {
      Map<String, String> routingTable = new HashMap<>();
      routingTable.put("192.168.1.0/24", "192.168.1.1:3000"); // Example routing entries
      Router router = new Router(null, null);
      router.run();
    } catch (Exception e) {
      System.err.println("Router setup error: " + e.getMessage());
    }
  }
}
