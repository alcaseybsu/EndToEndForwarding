# Network Simulation Project

## Overview
This project simulates a network with PCs, switches, and routers. The primary focus is to combine the Projects 1 and 2 and create a network that includes switches and routers. The following topology is used for the simulation:

```
PC_A --- Switch_1 --- Router_1 ---- Router_2 ----- Router_3-----Switch_2-----PC_B
```

## Prerequisites
- Java Development Kit (JDK) installed
- Terminal or command prompt access
- The configuration file (`config.txt`) is properly set up

## Compilation
Compile all Java files in the project directory using the following command:

```sh
javac *.java
```

## Running the Network Devices
Open separate terminal windows for each network device and run the `StartUp` class with the appropriate arguments for each device. Use the following commands:

### Terminal 1 (PC_A)
```sh
java StartUp PC_A
```

### Terminal 2 (PC_B)
```sh
java StartUp PC_B
```

### Terminal 3 (Switch_1)
```sh
java StartUp S1
```

### Terminal 4 (Switch_2)
```sh
java StartUp S2
```

### Terminal 5 (Router_1)
```sh
java StartUp R1
```

### Terminal 6 (Router_2)
```sh
java StartUp R2
```

### Terminal 7 (Router_3)
```sh
java StartUp R3
```

## Validation
### Send Message from PC_A to PC_B
1. In the terminal running `PC_A`, enter the destination IP, port, and message when prompted by the `interactWithUser` method.
2. Check the terminal running `PC_B` to confirm it receives the message.

### Send Message from PC_B to PC_A
1. In the terminal running `PC_B`, enter the destination IP, port, and message when prompted by the `interactWithUser` method.
2. Check the terminal running `PC_A` to confirm it receives the message.

### Check Routing Tables
In each router terminal (R1, R2, R3), use the `show table` command to verify that the routing tables are set up correctly.

```sh
Enter command (trace route, show table, exit): show table
```

### Trace Route
Use the `trace route` command in the router terminals to check the route to `PC_B` from `PC_A`.

```sh
Enter command (trace route, show table, exit): trace route
Enter destination IP: 127.0.0.1 (or the appropriate IP for PC_B)
```

## Debugging Tips
- Ensure that each device is correctly configured in the `config.txt` file.
- Verify that all devices are started in the correct order and are running without errors.
- Check the logs and console outputs for any errors or warnings during initialization and message transmission.

By following these instructions, you should be able to validate the complete virtual network and ensure that all components (PCs, switches, and routers) are functioning as expected.
