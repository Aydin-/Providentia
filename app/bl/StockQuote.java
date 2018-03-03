package bl;

import com.google.gson.Gson;
import utils.RESTClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get a real stock quote
 */
public final class StockQuote {

  public static Logger log = Logger.getGlobal();
  static HashMap<String, String> percentageCache = new HashMap<>();

  public Double newPrice(String symbol) throws IOException {
    StockPage stockPage;
    try {
      stockPage = parseStock(symbol);
      if (stockPage.query.results.quote.LastTradePriceOnly != null)
        return Double.parseDouble(stockPage.query.results.quote.LastTradePriceOnly);
      else if (stockPage.query.results.quote.ask != null)
        return Double.parseDouble(stockPage.query.results.quote.ask);
      else {
        return 0.0;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Cannot get current price for:" + symbol);
       log.log(Level.WARNING, "Exception: ", e);
      return 0.0;
    }
  }

  public static String newPercentageStatic(String symbol) {
    try {
      if (percentageCache.containsKey(symbol)) {
        return percentageCache.remove(symbol);
      }

      StockPage stockPage;
      stockPage = parseStock(symbol);

      if (stockPage.query.results.quote.ChangePercentRealtime != null) {
        percentageCache.put(symbol, stockPage.query.results.quote.ChangePercentRealtime);
        return stockPage.query.results.quote.ChangePercentRealtime;
      } else if (stockPage.query.results.quote.PercentChange != null) {
        percentageCache.put(symbol, stockPage.query.results.quote.PercentChange);
        return stockPage.query.results.quote.PercentChange;
      } else {
        percentageCache.put(symbol, stockPage.query.results.quote.ChangeinPercent);
        return stockPage.query.results.quote.ChangeinPercent;
      }

    } catch (Exception e) {
       log.log(Level.WARNING, "Exception: ", e);
    }
    return "0.0";
  }


  public static String getURL(String symbol) {
    return "https://query.yahooapis.com/v1/public/yql?q=" + URLEncoder.encode("select * from yahoo.finance.quotes ")
      + URLEncoder.encode("where symbol in ('" + symbol + "')") + "&format=json&diagnostics=true"
      + "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
  }

  public static StockPage parseStock(String stock) throws Exception {
    String json = RESTClient.readUrl(getURL(stock.trim()), 1);
    Gson gson = new Gson();
    return gson.fromJson(json, StockPage.class);
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
    String ChangeinPercent;
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
