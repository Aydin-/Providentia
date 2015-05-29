package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import com.google.gson.Gson;


/**
 * Get a real stock quote
 */
public class RealStockQuote implements StockQuote {

  public StockPage stockPage;


  public Double newPrice(String symbol) throws IOException {

    try {
      stockPage = parseStock(symbol);
      return Double.parseDouble(stockPage.query.results.quote.LastTradePriceOnly);
    } catch (Exception e) {
      System.out.println("---- Cannot get current price for:" + symbol);
      return 0.0;
    }
  }

  @Override
  public String newPercentage(String symbol) {
    try {
      stockPage = parseStock(symbol);
      return (stockPage.query.results.quote.PercentChange);
    } catch (Exception e) {
      System.out.println("---- Cannot get percent change for " + symbol);
      return "";
    }

  }

  private String readUrl(String urlString, int attempt) throws Exception {

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
      System.out.println("Trying again...");
      Thread.sleep(500);
      if (attempt < 4)
        return readUrl(urlString, attempt + 1);
      else
        return "";

    } finally {
      if (reader != null)
        reader.close();
    }
  }

  public StockPage parseStock(String stock) throws Exception {
    //System.out.println("Parsing "+stock);
    String json = readUrl(getURL(stock.trim()), 1);
    Gson gson = new Gson();
    return gson.fromJson(json, StockPage.class);
  }


  private static String getURL(String symbol) {
    String url = "https://query.yahooapis.com/v1/public/yql?q=";
    url += URLEncoder.encode("select * from yahoo.finance.quotes ");
    url += URLEncoder.encode("where symbol in ('" + symbol + "')");
    url += "&format=json&diagnostics=true";
    url += "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    return url;
  }

  static class Resource {
    MyResource resource;

  }

  static class MyResource {

    String classname;
    Fields fields;
  }

  static class Fields {
    String name;
    String price;
    String symbol;
    String ts;
    String type;
    String utctime;
    String volume;

  }

  static class Meta {
    String type;
    int start;
    int count;

  }

  static class MyList {
    Meta meta;
    List<Resource> resources;
  }

  static class Page {
    MyList list;
  }

  static class StockPage {
    Query query;
  }


  static class Query {
    Results results;
  }

  static class Results {
    Quote quote;

  }

  static class Quote {
    String symbol;
    String ask;
    String DaysLow;
    String DaysHigh;
    String YearLow;
    String YearHigh;
    String LastTradePriceOnly;
    String ChangePercentRealtime;
    String DaysValueChange;
    String open;
    String PercentChange;

    @Override
    public String toString() {
      return "Quote{" +
        "symbol='" + symbol + '\'' +
        ", ask='" + ask + '\'' +
        ", DaysLow='" + DaysLow + '\'' +
        ", DaysHigh='" + DaysHigh + '\'' +
        ", YearLow='" + YearLow + '\'' +
        ", YearHigh='" + YearHigh + '\'' +
        ", LastTradePriceOnly='" + LastTradePriceOnly + '\'' +
        ", ChangePercentRealtime='" + ChangePercentRealtime + '\'' +
        ", DaysValueChange='" + DaysValueChange + '\'' +
        '}';
    }
  }

}
