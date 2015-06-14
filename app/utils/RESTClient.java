package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RESTClient {

  static Logger log = Logger.getGlobal();

  public static String readUrl(String urlString) throws Exception {
    BufferedReader reader = null;
    try {
      URL url = new URL(urlString);
      reader = new BufferedReader(new InputStreamReader(url.openStream()));
      StringBuffer buffer = new StringBuffer();
      int read;
      char[] chars = new char[1024];
      while ((read = reader.read(chars)) != -1)
        buffer.append(chars, 0, read);

      return buffer.toString();
    } catch (Exception e) {
      log.log(Level.WARNING, "Exception reading URL: " + urlString, e);
    } finally {
      if (reader != null)
        reader.close();
    }
    return "";
  }

  public static String readUrl(String urlString, int attempt) {

    BufferedReader reader = null;
    try {
      URL url = new URL(urlString);
      reader = new BufferedReader(new InputStreamReader(url.openStream()));
      StringBuffer buffer = new StringBuffer();
      int read;
      char[] chars = new char[1024];
      while ((read = reader.read(chars)) != -1)
        buffer.append(chars, 0, read);

      return buffer.toString();
    } catch (Exception e) {
      log.warning("Trying again for " + urlString);
      try {
        Thread.sleep(500);
      } catch (InterruptedException e1) {
        log.log(Level.WARNING, "Thread interrupted in readURL " + urlString, e1);
      }
      if (attempt < 4)
        return readUrl(urlString, attempt + 1);
      else
        return "";

    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e) {
          log.log(Level.WARNING, "Exception closing reader after reading URL: " + urlString, e);
        }
    }
  }

}
