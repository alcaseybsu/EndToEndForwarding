import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ComputerParser {

  public static String findInfo(
    String filePath,
    String startHeader,
    String endHeader,
    String searchString
  ) throws IOException {
    boolean withinHeaders = false;

    StringBuilder result = new StringBuilder();

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;

      while ((line = br.readLine()) != null) {
        if (line.equals(startHeader)) {
          withinHeaders = true;
        } else if (line.equals(endHeader)) {
          withinHeaders = false;
        } else if (withinHeaders && !line.isEmpty()) {
          String[] parts = line.split(",");
          if (parts.length >= 2) {
            if (parts[0].trim().equals(searchString.trim())) {
              result.append(parts[1].trim());
              for (int i = 2; i < parts.length; i++) {
                result.append(",").append(parts[i].trim());
              }
              result.append(System.lineSeparator());
            } else if (parts[1].trim().equals(searchString.trim())) {
              result.append(parts[0].trim());
              for (int i = 2; i < parts.length; i++) {
                result.append(",").append(parts[i].trim());
              }
              result.append(System.lineSeparator());
            }
          } else {
            // Handle invalid lines
            System.out.println("Invalid line: " + line);
          }
        }
      }
    }

    return result.toString();
  }
}
