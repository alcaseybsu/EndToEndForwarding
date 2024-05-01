import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class StartUp {

  private static final String CONFIG_FILE_PATH = "config.txt";
  private static ConfigManager configManager = new ConfigManager();

  public static void main(String[] args) {
    @SuppressWarnings("unused")
    ConfigManager configManager = new ConfigManager();
    generateComputersAndSwitches();
  }

  public static void generateComputersAndSwitches() {
    String computerStartHeader = "# Computer IP and port numbers";
    String computerEndHeader = "# End Computer List";
    String switchStartHeader = "# Different port range for switches";
    String switchEndHeader = "# End Switch List";

    try (
      BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE_PATH))
    ) {
      String line;
      boolean withinComputerHeaders = false;
      boolean withinSwitchHeaders = false;

      while ((line = br.readLine()) != null) {
        if (line.equals(computerStartHeader)) {
          withinComputerHeaders = true;
          continue;
        } else if (line.equals(computerEndHeader)) {
          withinComputerHeaders = false;
        } else if (line.equals(switchStartHeader)) {
          withinSwitchHeaders = true;
          continue;
        } else if (line.equals(switchEndHeader)) {
          withinSwitchHeaders = false;
        }

        if (withinComputerHeaders && !line.isEmpty()) {
          String[] parts = line.split(",");
          if (parts.length == 4) { // Adjusted for MAC address
            generateComputer(
              parts[0].trim(),
              parts[1].trim(),
              Integer.parseInt(parts[2].trim())
            );
          } else {
            System.out.println("Invalid line in computer section: " + line);
          }
        } else if (withinSwitchHeaders && !line.isEmpty()) {
          String[] parts = line.split(",");
          if (parts.length == 4) { // Adjusted for MAC address
            generateSwitch(
              parts[0].trim(),
              Integer.parseInt(parts[2].trim()),
              configManager
            );
          } else {
            System.out.println("Invalid line in switch section: " + line);
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error reading configuration file: " + e.getMessage());
    }
  }

  public static void generateComputer(String name, String ipAddress, int port) {
    try {
      ConfigManager configManager = new ConfigManager(); // Ensure this exists and is initialized correctly
      Computer computer = new Computer(name, ipAddress, port, configManager);
      Thread thread = new Thread(computer); // Use the Computer class which implements Runnable
      thread.start(); // Start thread which calls Computer's run method
    } catch (Exception e) {
      System.err.println("Computer setup error: " + e.getMessage());
    }
  }

  public static void generateSwitch(
    String name,
    int port,
    ConfigManager configManager
  ) {
    try {
      Switch mySwitch = new Switch(name, port, configManager);
      new Thread(mySwitch).start();
    } catch (IOException e) {
      System.err.println("Switch setup error: " + e.getMessage());
    }
  }
}