package db;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.junit.Test;

public class DatabaseTest {
  @Test
  public void testDatabaseConnection() throws IOException {
    try {
      DatabaseConfig.createTables();
      DatabaseConfig.getConnection().prepareStatement("select * from stock_symbols");
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGetStockSymbol() {
    System.out.println(DatabaseConfig.getStockSymbol("Apple"));

  }
}
