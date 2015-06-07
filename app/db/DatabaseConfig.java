package db;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConfig {
  public static Connection getConnection() throws URISyntaxException, SQLException {
    //  String dbUrl = "jdbc:postgres://aqelfpqdvqynvh:d7LgWGDfd4mr4Q0KLLunYSwTeQ@ec2-54-247-79-142.eu-west-1.compute.amazonaws.com:5432/d6v0kj46nh1i0p";

    String url = "jdbc:postgresql://localhost/pro";
    Properties props = new Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");
    //props.setProperty("ssl","false");
    Connection conn = DriverManager.getConnection(url, props);

    return conn;
  }

  public static void createTables() {
    Connection connection = null;
    try {
      connection = getConnection();
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS stock_symbols (symbol VARCHAR , company VARCHAR )");

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public static void insertStockSymbol(String company, String symbol) {
    Connection connection = null;
    try {
      connection = getConnection();
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM stock_symbols");

      if(!rs.next()) {
        stmt.executeUpdate("INSERT INTO stock_symbols (SYMBOL, COMPANY) VALUES ('AAPL', 'APPLE')");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

  }

  public static String getStockSymbol(String company) {
    Connection connection = null;
    try {
      connection = getConnection();
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM stock_symbols");

      rs.next();
      return rs.getString("symbol");

    } catch (SQLException e1) {
      e1.printStackTrace();
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return null;
  }


}
