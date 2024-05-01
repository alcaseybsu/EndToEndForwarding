import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

  private Map<String, String> ipAddresses = new HashMap<>();
  private Map<String, String> macAddresses = new HashMap<>();
  private Map<String, String> subnetMasks = new HashMap<>();
  private Map<String, Map<String, String>> routingTables = new HashMap<>();
  private Map<String, String> arpTables = new HashMap<>();

  public ConfigManager() {
    loadInitialConfigs();
  }

  public void loadInitialConfigs() {
    String configFilePath = "config.txt"; // Ensure this path is correct
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
            parseDeviceConfig(line);
            break;
          case "# Explicit Routing Tables for each Router":
            parseRoutingTable(line);
            break;
          case "# ARP table":
            parseArpTable(line);
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
    if (parts.length < 4) return; // Ensure there are enough parts to parse

    String deviceName = parts[0].trim();
    String ipAddress = parts[1].trim();
    //String port = parts[2].trim(); // port might be used elsewhere
    String macAddress = parts[3].trim();

    ipAddresses.put(deviceName, ipAddress);
    macAddresses.put(deviceName, macAddress);
    subnetMasks.put(deviceName, "255.255.255.0"); // Default subnet mask
  }

  private void parseRoutingTable(String line) {
    String[] parts = line.split(",");
    if (parts.length < 3) return; // Ensure there are enough parts to parse

    String routerName = parts[0].trim();
    String destination = parts[1].trim();
    String nextHopDetails = parts[2].trim(); // Assuming "NextHopIP:Port, NextHopMAC"

    Map<String, String> routerTable = routingTables.getOrDefault(
      routerName,
      new HashMap<>()
    );
    routerTable.put(destination, nextHopDetails);
    routingTables.put(routerName, routerTable);
  }

  private void parseArpTable(String line) {
    String[] parts = line.split(",");
    if (parts.length < 2) return; // Ensure there are enough parts to parse

    String ipAddress = parts[0].trim();
    String macAddress = parts[1].trim();

    arpTables.put(ipAddress, macAddress);
  }

  public String getIpAddress(String deviceName) {
    return ipAddresses.get(deviceName);
  }

  public String getMacAddress(String deviceName) {
    return macAddresses.get(deviceName);
  }

  public String getSubnetMask(String deviceName) {
    return subnetMasks.get(deviceName);
  }

  public Map<String, String> getRoutingTable(String routerName) {
    return routingTables.get(routerName);
  }

  public Map<String, String> getARPTable(String deviceName) {
    return arpTables;
  }

  public String resolveArp(String ipAddress) {
    return arpTables.get(ipAddress);
  }

  // Methods to update configurations dynamically
  public void updateIpAddress(String deviceName, String newIp) {
    ipAddresses.put(deviceName, newIp);
  }

  public Map<String, String> getConnectedDevices(String name) {
    Map<String, String> connectedDevices = new HashMap<>();

    // Check if the given name exists in the routingTables map
    if (routingTables.containsKey(name)) {
      Map<String, String> routingTable = routingTables.get(name);
      // Iterate over the routing table entries
      for (Map.Entry<String, String> entry : routingTable.entrySet()) {
        String destination = entry.getKey();
        String nextHop = entry.getValue();
        // Add the nextHop (connected device) to the connectedDevices map
        connectedDevices.put(destination, nextHop);
      }
    }

    // Check if the given name exists in the arpTables map
    if (arpTables.containsKey(name)) {
      String macAddress = arpTables.get(name);
      // Add the MAC address (connected device) to the connectedDevices map
      connectedDevices.put("Gateway", macAddress); // Assuming it's connected to the gateway
    }

    return connectedDevices;
  }
}
