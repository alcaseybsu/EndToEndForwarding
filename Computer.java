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

  // Constructor that uses ConfigManager to set properties
  public Computer(
    String name,
    String ipAddress,
    int port,
    ConfigManager configManager
  ) throws UnknownHostException, SocketException {
    this.name = name;
    this.address = InetAddress.getByName(ipAddress);
    this.port = port;
    this.networkMask = InetAddress.getByName(configManager.getSubnetMask(name));
    this.arpTable = new HashMap<>(configManager.getARPTable(name));
    this.socket = new DatagramSocket(this.port, this.address);
  }

  public void run() {
    listenForMessages();
    interactWithUser();
  }

  private void interactWithUser() {
    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.print("Enter the destination IP or 'exit' to quit: ");
        String destIp = scanner.nextLine();
        if ("exit".equalsIgnoreCase(destIp)) break;

        System.out.print("Enter the destination port: ");
        int destPort = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter the message: ");
        String message = scanner.nextLine();
        sendMessage(destIp, destPort, message, "original");
      }
    } catch (IOException e) {
      System.err.println("Error sending message: " + e.getMessage());
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
          handleReceivedMessage(
            received,
            packet.getAddress(),
            packet.getPort()
          );
        }
      } catch (IOException e) {
        System.err.println("Error receiving messages: " + e.getMessage());
      }
    })
      .start();
  }

  private void handleReceivedMessage(
    String message,
    InetAddress sourceAddress,
    int sourcePort
  ) {
    System.out.println(name + " received: " + message);
    // Check if the message is an original and needs a reply
    if (message.contains("original")) {
      try {
        sendMessage(
          sourceAddress.getHostAddress(),
          sourcePort,
          "Received your message: " + message,
          "reply"
        );
      } catch (IOException e) {
        System.err.println("Error sending reply: " + e.getMessage());
      }
    }
  }

  private void sendMessage(
    String destIp,
    int destPort,
    String message,
    String type
  ) throws IOException {
    InetAddress destAddress = InetAddress.getByName(destIp);
    String destMac;
    if (isSameSubnet(destAddress)) {
      destMac = arpTable.getOrDefault(destIp, "FF:FF:FF:FF:FF:FF"); // Use specific MAC or broadcast
    } else {
      destIp = "192.168.1.1"; // Gateway IP should be dynamically retrieved or configured
      destMac = arpTable.getOrDefault(destIp, "FF:FF:FF:FF:FF:FF"); // Gateway MAC or broadcast
    }

    byte[] data =
      (name + " (" + type + ") says to " + destMac + ": " + message).getBytes();
    DatagramPacket packet = new DatagramPacket(
      data,
      data.length,
      destAddress,
      destPort // Send to the specified port
    );
    socket.send(packet);
    System.out.println(
      name +
      " sent (" +
      type +
      ") message to " +
      destIp +
      ":" +
      destPort +
      ": " +
      message
    );
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
    if (args.length < 3) {
      System.out.println("Usage: java Computer <Name> <IP Address> <Port>");
      return;
    }
    String name = args[0];
    String ipAddress = args[1];
    int port = Integer.parseInt(args[2]);

    try {
      ConfigManager configManager = new ConfigManager(); // Make sure this can handle dynamic inputs if needed
      Computer computer = new Computer(name, ipAddress, port, configManager);
      new Thread(computer).start();
    } catch (Exception e) {
      System.err.println("Initialization error: " + e.getMessage());
    }
  }
}
