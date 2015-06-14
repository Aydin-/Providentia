package db;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseAO {
  public static Logger log = Logger.getGlobal();

  private static Connection getHerokuDBConnection() throws URISyntaxException, SQLException {
    URI dbUri = new URI(System.getenv("DATABASE_URL"));

    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

    return DriverManager.getConnection(dbUrl, username, password);
  }


  public static Connection getConnection() throws URISyntaxException, SQLException {
    if(false)
      return getHerokuDBConnection();

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
      log.log(Level.WARNING, "Exception :", e);
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        log.log(Level.WARNING, "Exception :", e);
      }
    }
  }

  public static void insertStockSymbol(String company, String symbol) {
    Connection connection = null;
    try {
      connection = getConnection();

      PreparedStatement ps = connection.prepareStatement("SELECT * FROM stock_symbols WHERE company=?");
      ps.setString(1, company);
      ResultSet rs = ps.executeQuery();

      if (!rs.next()) {
        PreparedStatement psUpdate = connection.prepareStatement("INSERT INTO stock_symbols (SYMBOL, COMPANY) VALUES (?,?)");
        psUpdate.setString(1, symbol);
        psUpdate.setString(2, company);
        psUpdate.executeUpdate();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Exception :", e);
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        log.log(Level.WARNING, "Exception :", e);
      }
    }

  }

  public static String getStockSymbol(String company) {
    Connection connection = null;
    try {
      connection = getConnection();
      PreparedStatement ps = connection.prepareStatement("SELECT * FROM stock_symbols WHERE company=?");
      ps.setString(1, company);
      ResultSet rs = ps.executeQuery();

      if (rs.next())
        return rs.getString("symbol");
      else
        return null;
    } catch (SQLException | URISyntaxException e1) {
      log.log(Level.WARNING, "Exception :", e1);
    } finally {
      if (connection != null) try {
        connection.close();
      } catch (SQLException e) {
        log.log(Level.WARNING, "Exception :", e);
      }
    }
    return null;
  }

}
