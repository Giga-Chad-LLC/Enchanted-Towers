package components.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;



public class FileReader {
  private static final Logger logger = Logger.getLogger(FileReader.class.getName());

  public static String readRawFile(URL url) throws IOException {

    try {
      URLConnection connection = url.openConnection();
      InputStream inputStream = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder content = new StringBuilder();

      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line);
      }

      logger.info("Successfully read file content from '" + url + "'");
      return content.toString();
    } catch (IOException e) {
      logger.severe("Failed to read file: url='" + url + "'\n" + "Error: '" + e.getMessage() + "'");
      System.err.println();
       return "";
    }
  }
}
