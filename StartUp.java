import java.io.*;
//import java.net.*;

public class StartUp {

    private static final String CONFIG_FILE_PATH = "config.txt";
    private static ConfigManager configManager = new ConfigManager(CONFIG_FILE_PATH);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp bin StartUp <device_name>");
            System.exit(1);
        }

        String deviceName = args[0];
        startDevice(deviceName);
    }

    public static void startDevice(String deviceName) {
        try {
            String deviceType = configManager.getDeviceType(deviceName);
            switch (deviceType) {
                case "Computer":
                    startComputer(deviceName);
                    break;
                case "Switch":
                    startSwitch(deviceName);
                    break;
                case "Router":
                    startRouter(deviceName);
                    break;
                default:
                    System.err.println("Unknown device: " + deviceName);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(deviceName + " setup error: " + e.getMessage());
        }
    }

    public static void startComputer(String name) {
        try {
            //String ipAddress = configManager.getIpAddress(name);
            //int port = configManager.getPort(name);
            Computer computer = new Computer(name, configManager);
            new Thread(computer).start();
        } catch (Exception e) {
            System.err.println("Computer setup error: " + e.getMessage());
        }
    }

    public static void startSwitch(String name) {
        try {
            int port = configManager.getPort(name);
            Switch mySwitch = new Switch(name, port, configManager);
            new Thread(mySwitch).start();
        } catch (IOException e) {
            System.err.println("Switch setup error: " + e.getMessage());
        }
    }

    public static void startRouter(String name) {
        try {
            Router router = new Router(name, configManager);
            new Thread(router).start();
        } catch (IOException e) {
            System.err.println("Router setup error: " + e.getMessage());
        }
    }
}
