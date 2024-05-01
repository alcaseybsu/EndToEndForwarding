import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ElementCounter {

    public static int countOccurrencesBetweenHeaders(String filePath, String startHeader, String endHeader, String keyword) {
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

    public static int generateNumber(String startHeader, String endHeader, String keyword){
        String filePath = "config.txt";
        int result = countOccurrencesBetweenHeaders(filePath, startHeader, endHeader, keyword);
        return result;
    }
}
