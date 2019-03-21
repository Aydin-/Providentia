package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RESTClient {

	static Logger log = Logger.getGlobal();
	private static int BACKOFF_AFTER = 4;

	public static String readUrl(String urlString) throws MalformedURLException {
		return readUrl(urlString, 1);
	}

	public static String readUrl(String urlString, int attempt) throws MalformedURLException {

	    URL url = new URL(urlString);
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			
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
			if (attempt < BACKOFF_AFTER) {
				return readUrl(urlString, attempt + 1);
			} else {
				return "";
			}		
		} 
	}

}
