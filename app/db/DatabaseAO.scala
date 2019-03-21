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

import play.api.Play;

class DatabaseAO {
   val log = Logger.getGlobal();

    def getHerokuDBConnection():Connection ={
      val dbUri = new URI(System.getenv("DATABASE_URL"));

      val username = dbUri.getUserInfo().split(":")(0);
      val password = dbUri.getUserInfo().split(":")(1);
      val dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

      return DriverManager.getConnection(dbUrl, username, password);
  }
  
    def getConnection():Connection ={
      if("heroku".equalsIgnoreCase(Play.current.configuration.getString("database").get)) {
        return getHerokuDBConnection();
      }

      val url = "jdbc:postgresql://localhost/pro";
      var props = new Properties();
      props.setProperty("user", "postgres");
      props.setProperty("password", "postgres");
      //props.setProperty("ssl","false");

      return DriverManager.getConnection(url, props);
    }

    def createTables() {
      var connection = getConnection();
        val stmt = connection.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS stock_symbols (symbol VARCHAR , company VARCHAR )");      
    }

   def insertStockSymbol(company:String, symbol:String) = {
     val connection = getConnection()

      val ps = connection.prepareStatement("SELECT * FROM stock_symbols WHERE company=?");
      ps.setString(1, company);
      var rs = ps.executeQuery();

      if (!rs.next()) {
        var psUpdate = connection.prepareStatement("INSERT INTO stock_symbols (SYMBOL, COMPANY) VALUES (?,?)");
        psUpdate.setString(1, symbol);
        psUpdate.setString(2, company);
        psUpdate.executeUpdate();
      }
  }

   def getStockSymbol(company:String):String = {
    var connection = getConnection();

      var ps = connection.prepareStatement("SELECT * FROM stock_symbols WHERE company=?");
      ps.setString(1, company);
      var rs = ps.executeQuery();

      if (rs.next()) {
        return rs.getString("symbol");
      }
    return null;
  }

}
