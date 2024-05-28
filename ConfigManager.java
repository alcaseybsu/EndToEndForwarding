import java.io.*;
import java.util.*;

public class ConfigManager {
    private Map<String, String> ipAddresses = new HashMap<>();
    private Map<String, String> macAddresses = new HashMap<>();
    private Map<String, String> subnetMasks = new HashMap<>();
    private Map<String, Map<String, String>> routingTables = new HashMap<>();
    private Map<String, String> arpTables = new HashMap<>();
    private Map<String, List<String>> connections = new HashMap<>();

    public ConfigManager(String configFilePath) {
        loadInitialConfigs(configFilePath);
    }

    public void loadInitialConfigs(String configFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
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
                    case "# Explicit Routing Tables for each Router":
                        parseRoutingTable(line);
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
            System.err.println("Failed to read configuration file: " + e.getMessage());
        }
    }

    private void parseDeviceConfig(String line) {
        String[] parts = line.split(",");
        if (parts.length < 4) return; // Ensure there are enough parts to parse

        String deviceName = parts[0].trim();
        String ipAddress = parts[1].trim();
        // String port = parts[2].trim(); // port might be used elsewhere
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

        Map<String, String> routerTable = routingTables.getOrDefault(routerName, new HashMap<>());
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

    private void parseConnections(String line) {
        String[] parts = line.split(":");
        if (parts.length < 2) return; // Ensure there are enough parts to parse

        String deviceName = parts[0].trim();
        String connectedDevice = parts[1].trim();

        connections.computeIfAbsent(deviceName, k -> new ArrayList<>()).add(connectedDevice);
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

    public List<String> getConnectedDevices(String name) {
        return connections.getOrDefault(name, Collections.emptyList());
    }

    // Method to count occurrences between headers
    public int countOccurrencesBetweenHeaders(String filePath, String startHeader, String endHeader, String keyword) {
        int elementCount = 0;
        boolean withinHeaders = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.equals(startHeader)) {
                    withinHeaders = true;
                } else if (line.equals(endHeader)) {
                    withinHeaders = false;
                } else if (withinHeaders && !line.isEmpty() && line.contains(keyword)) {
                    elementCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return elementCount;
    }
}
