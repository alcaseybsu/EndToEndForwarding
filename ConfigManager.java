import java.io.*;
import java.util.*;

public class ConfigManager {

  private Map<String, String> ipAddresses = new HashMap<>();
  private Map<String, String> macAddresses = new HashMap<>();
  private Map<String, Integer> ports = new HashMap<>(); // Store ports separately
  private Map<String, String> subnetMasks = new HashMap<>();
  private Map<String, String> arpTable = new HashMap<>();
  private Map<String, List<String>> connections = new HashMap<>();

  public ConfigManager(String configFilePath) {
    loadInitialConfigs(configFilePath);
  }

  public void loadInitialConfigs(String configFilePath) {
    try (
      BufferedReader reader = new BufferedReader(new FileReader(configFilePath))
    ) {
      String line;
      String currentSection = "";

      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("# End")) continue; // Skip empty lines and end tags

        if (line.startsWith("#")) {
          currentSection = line; // Store current section header
          continue;
        }

        switch (currentSection) {
          case "# Computer IP and port numbers":
          case "# Different port range for switches":
          case "# Router Configuration":
            parseDeviceConfig(line);
            break;
          case "# ARP table":
            parseArpTable(line);
            break;
          case "# Node connections":
            parseConnections(line);
            break;
        }
      }
    } catch (IOException e) {
      System.err.println(
        "Failed to read configuration file: " + e.getMessage()
      );
    }
  }

  private void parseDeviceConfig(String line) {
    String[] parts = line.split(",");
    if (parts.length < 4) return;

    String deviceName = parts[0].trim();
    String ipAddress = parts[1].trim();
    int port = Integer.parseInt(parts[2].trim());
    String macAddress = parts[3].trim();

    ipAddresses.put(deviceName, ipAddress);
    ports.put(deviceName, port); // Store port number separately
    macAddresses.put(deviceName, macAddress);
    subnetMasks.put(deviceName, "255.255.255.0"); // Default subnet mask
    arpTable.put(ipAddress, macAddress);
  }

  private void parseArpTable(String line) {
    String[] parts = line.split(",");
    if (parts.length < 2) return;

    String ipAddress = parts[0].trim();
    String macAddress = parts[1].trim();

    arpTable.put(ipAddress, macAddress);
  }

  private void parseConnections(String line) {
    String[] parts = line.split(":");
    if (parts.length < 2) return;

    String deviceName = parts[0].trim();
    String connectedDevice = parts[1].trim();

    connections
      .computeIfAbsent(deviceName, k -> new ArrayList<>())
      .add(connectedDevice);
  }

  public String getIpAddress(String deviceName) {
    return ipAddresses.get(deviceName);
  }

  public String getMacAddress(String deviceName) {
    return macAddresses.get(deviceName);
  }

  public int getPort(String deviceName) {
    return ports.get(deviceName); // Retrieve port number from the map
  }

  public String getSubnetMask(String deviceName) {
    return subnetMasks.get(deviceName);
  }

  public Map<String, String> getARPTable() {
    return arpTable;
  }

  public String resolveArp(String ipAddress) {
    return arpTable.get(ipAddress);
  }

  public void updateIpAddress(String deviceName, String newIp) {
    ipAddresses.put(deviceName, newIp);
  }

  public List<String> getConnectedDevices(String name) {
    return connections.getOrDefault(name, Collections.emptyList());
  }

  public String getDeviceType(String deviceName) {
    if (ipAddresses.containsKey(deviceName)) return "Computer";
    if (connections.containsKey(deviceName)) return "Switch";
    return "Router";
  }
}
