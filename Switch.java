import java.io.*;
import java.net.*;
import java.util.*;

public class Switch implements Runnable {

  private String name;
  private InetAddress ipAddress;
  private int port;
  private DatagramSocket socket;
  private Map<String, String> forwardingTable = new HashMap<>(); // MAC to port mapping

  @SuppressWarnings("unused")
  private Map<String, String> routingTable = new HashMap<>();

  @SuppressWarnings("unused")
  private Map<String, String> neighbors;

  @SuppressWarnings("unused")
  private ConfigManager configManager;

  public Switch(String name, int port, ConfigManager configManager)
    throws IOException {
    this.name = name;
    this.configManager = configManager;
    this.ipAddress = InetAddress.getByName(configManager.getIpAddress(name));
    this.port = port; // Assuming port is part of the configuration outside the MAC address
    this.socket = new DatagramSocket(this.port, this.ipAddress);
    this.neighbors = configManager.getConnectedDevices(name); // Initialize neighbors based on ConfigManager
  }

  public void run() {
    System.out.println("Switch " + name + " is running.");
    new Thread(this::listenForPackets).start();
    listenForCommands();
  }

  private void listenForCommands() {
    Scanner scanner = new Scanner(System.in); // Scanner for reading from the console
    try {
      while (true) {
        System.out.println("Enter command (show table, exit):");
        String command = scanner.nextLine().trim().toLowerCase(); // Read and normalize the command

        if ("exit".equalsIgnoreCase(command)) {
          System.out.println("Exiting command listener.");
          break; // Exit the loop and thus stop listening for commands
        }

        switch (command) {
          case "show table":
            showTable(); // Show the forwarding table
            break;
          default:
            System.out.println("Unknown command: " + command);
            break;
        }
      }
    } finally {
      scanner.close(); // Close the scanner to free resources
    }
  }

  private void listenForPackets() {
    byte[] buffer = new byte[1024]; // Size can be adjusted based on expected packet size
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    try {
      while (!Thread.currentThread().isInterrupted()) {
        socket.receive(packet);
        handlePacket(packet);
      }
    } catch (IOException e) {
      System.out.println(
        "[" + name + "] Error receiving packet: " + e.getMessage()
      );
    } finally {
      socket.close();
    }
  }

  private void handlePacket(DatagramPacket packet) {
    String receivedData = new String(packet.getData(), 0, packet.getLength())
      .trim();
    String[] parts = receivedData.split("\\|");
    if (parts.length < 3) return; // Basic validation

    String srcMAC = parts[0];
    String destMAC = parts[1];
    String payload = parts[2];

    String sourceIP = packet.getAddress().getHostAddress();
    int sourcePort = packet.getPort();
    forwardingTable.put(srcMAC, sourceIP + ":" + sourcePort);

    forwardOrFlood(srcMAC, destMAC, payload);
  }

  private void forwardOrFlood(String srcMAC, String destMAC, String payload) {
    String destination = forwardingTable.get(destMAC);
    if (destination != null) {
      forwardPacket(destMAC, payload, destination);
    } else {
      flood(srcMAC, payload);
    }
  }

  private void forwardPacket(
    String destMAC,
    String payload,
    String destination
  ) {
    try {
      String[] parts = destination.split(":");
      InetAddress destAddress = InetAddress.getByName(parts[0]);
      int destPort = Integer.parseInt(parts[1]);

      DatagramPacket forwardPacket = new DatagramPacket(
        payload.getBytes(),
        payload.length(),
        destAddress,
        destPort
      );
      socket.send(forwardPacket);
      System.out.println(
        "[" + name + "] Forwarded packet to " + destMAC + " at " + destination
      );
    } catch (IOException e) {
      System.out.println(
        "[" + name + "] Error forwarding packet: " + e.getMessage()
      );
    }
  }

  private void flood(String srcMAC, String payload) {
    forwardingTable.forEach((mac, port) -> {
      if (!mac.equals(srcMAC)) {
        try {
          String[] parts = port.split(":");
          InetAddress address = InetAddress.getByName(parts[0]);
          int portNumber = Integer.parseInt(parts[1]);

          DatagramPacket floodPacket = new DatagramPacket(
            payload.getBytes(),
            payload.length(),
            address,
            portNumber
          );
          socket.send(floodPacket);
          System.out.println("[" + name + "] Flooding packet to " + mac);
        } catch (IOException e) {
          System.out.println(
            "[" + name + "] Error flooding packet: " + e.getMessage()
          );
        }
      }
    });
  }

  private void showTable() {
    System.out.println("Forwarding Table:");
    forwardingTable.forEach((key, value) ->
      System.out.println(key + " -> " + value)
    );
  }

  public void setNeighbors(Map<String, String> neighbors) {
    // Update the neighbors of the switch
    this.neighbors = neighbors;
    System.out.println(
      "Updated neighbors for Switch " + name + ": " + neighbors
    );
  }

  public void setRoutingTable(Map<String, String> routingTable) {
    // Update the routing table of the switch
    this.routingTable = routingTable;
    System.out.println(
      "Updated routing table for Switch " + name + ": " + routingTable
    );
  }
}
