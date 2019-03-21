package db;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.junit.Test;

public class DatabaseTest {

  @Test
  public void testDatabaseConnection() throws IOException {
    try {
      new DatabaseAO().createTables();
      new DatabaseAO().getConnection().prepareStatement("select * from stock_symbols");
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }

  @Test
  public void testGetStockSymbol() {
    System.out.println(new DatabaseAO().getStockSymbol("Apple"));

  }
}
