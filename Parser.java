import java.io.*;
import java.util.*;

public class Parser {

    // Parse the config file to extract details for a given node
    public static Map<String, Map<String, String>> parseConfig(File configFile, String nodeName) {
        Map<String, Map<String, String>> configDetails = new HashMap<>();

        try (Scanner scanner = new Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(nodeName)) {
                    // Process the line to extract details
                    Map<String, String> neighbors = new HashMap<>();
                    Map<String, String> routingTable = new HashMap<>();
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        String[] keyValue = part.split(":");
                        if (keyValue.length == 2) {
                            if (keyValue[0].startsWith("Neighbor")) {
                                neighbors.put(keyValue[0].trim(), keyValue[1].trim());
                            } else if (keyValue[0].startsWith("Routing")) {
                                routingTable.put(keyValue[0].trim(), keyValue[1].trim());
                            }
                        }
                    }
                    configDetails.put("Neighbors", neighbors);
                    configDetails.put("RoutingTable", routingTable);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Config file not found: " + e.getMessage());
        }

        return configDetails;
    }

    // Find neighbors of a device from the config file
    public static List<String> findNeighbor(String deviceName, String filePath) {
        List<String> neighbors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(deviceName)) {
                    int index = line.indexOf(deviceName) + deviceName.length() + 1;
                    String neighbor = line.substring(index).trim();
                    neighbors.add(neighbor);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
        }
        return neighbors;
    }

    public static void main(String[] args) {
        // Parse config file and find switch
        String filePath = "config.txt";

        // Parse the configuration file to get the neighbors and routing table
        Map<String, Map<String, String>> configDetails = parseConfig(new File(filePath), "Switch");
        Map<String, String> neighbors = configDetails.get("Neighbors");
        Map<String, String> routingTable = configDetails.get("RoutingTable");

        // Create an instance of Switch with neighbors and routing table
        try {
            Switch switchInstance = new Switch("Switch1", 4000, new ConfigManager(filePath));
            switchInstance.setNeighbors(neighbors);
            switchInstance.setRoutingTable(routingTable);
        } catch (IOException e) {
            System.err.println("Error creating switch instance: " + e.getMessage());
        }
    }
}
