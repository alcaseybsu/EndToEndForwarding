import java.io.*;
import java.net.*;
import java.util.*;

public class Router implements Runnable {

  private String name;
  private InetAddress ipAddress;
  private int port;
  private DatagramSocket socket;
  private TreeMap<String, String> routingTable;
  private Map<String, String> arpTable = new HashMap<>();
  @SuppressWarnings("unused")
  private ConfigManager configManager;
  private Scanner scanner = new Scanner(System.in);
  private Set<String> receivedMessageIds = new HashSet<>(); // Set to store received message IDs

  public Router(String name, ConfigManager configManager) throws UnknownHostException, SocketException {
    this.name = name;
    this.configManager = configManager;
    this.ipAddress = InetAddress.getByName(configManager.getIpAddress(name));
    this.port = configManager.getPort(name);
    this.socket = new DatagramSocket(port, ipAddress);
    this.routingTable = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(n -> n));
    this.arpTable.putAll(configManager.getARPTable()); 
  }

  public void run() {
    System.out.println("\nRouter " + name + " is running.");
    new Thread(this::listenForPackets).start();
    listenForCommands();
  }

  private void listenForPackets() {
    byte[] buffer = new byte[2048];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    try {
      while (!Thread.currentThread().isInterrupted()) {
        socket.receive(packet);
        handlePacket(packet);
      }
    } catch (IOException e) {
      System.err.println("\nError receiving packets: " + e.getMessage());
      if (!socket.isClosed()) {
        socket.close();
      }
    }
  }

  private void listenForCommands() {
    while (true) {
      System.out.println("\nEnter command (trace route, show table, exit):");
      String command = scanner.nextLine();
      if ("exit".equalsIgnoreCase(command)) break;

      switch (command) {
        case "trace route":
          System.out.println("\nEnter destination IP:");
          String destIP = scanner.nextLine();
          String nextHop = findNextHop(destIP);
          System.out.println("\nNext hop for " + destIP + " is " + nextHop);
          break;
        case "show table":
          showTables();
          break;
        default:
          System.out.println("\nUnknown command");
          break;
      }
    }
    scanner.close();
    System.out.println("\nRouter " + name + " is shutting down.");
  }

  private String findNextHop(String destIP) {
    for (Map.Entry<String, String> entry : routingTable.entrySet()) {
      if (destIP.startsWith(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private void showTables() {
    System.out.println("\nRouting Table:");
    routingTable.forEach((key, value) -> System.out.println(key + " -> " + value));
    System.out.println("\nARP Table:");
    arpTable.forEach((key, value) -> System.out.println(key + " -> " + value));
  }

  private void handlePacket(DatagramPacket packet) throws IOException {
    String receivedData = new String(packet.getData(), 0, packet.getLength()).trim();
    String[] parts = receivedData.split("\\|");
    if (parts.length < 4) return;

    String messageId = parts[0];
    String destIP = parts[1];
    String payload = parts[2];

    if (receivedMessageIds.contains(messageId)) return;
    receivedMessageIds.add(messageId);

    routePacket(messageId, destIP, payload);
  }

  private void routePacket(String messageId, String destIP, String payload) throws IOException {
    String nextHop = findNextHop(destIP);
    if (nextHop != null) {
      String[] parts = nextHop.split(":");
      InetAddress nextHopIp = InetAddress.getByName(parts[0]);
      int nextHopPort = Integer.parseInt(parts[1]);
      String fullMessage = "\n" + messageId + "|" + destIP + "|" + payload;
      DatagramPacket forwardPacket = new DatagramPacket(fullMessage.getBytes(), fullMessage.length(), nextHopIp, nextHopPort);
      socket.send(forwardPacket);
      System.out.println("\n[" + name + "] Packet routed to " + destIP + " via " + nextHop);
    } else {
      System.out.println("\n[" + name + "] No route found for " + destIP);
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("\nUsage: java Router <Name>");
      return;
    }
    String name = args[0];

    try {
      ConfigManager configManager = new ConfigManager("config.txt");
      Router router = new Router(name, configManager);
      new Thread(router).start();
    } catch (Exception e) {
      System.err.println("\nRouter setup error: " + e.getMessage());
    }
  }
}
