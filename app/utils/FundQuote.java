package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import actors.ProgressBar;
import actors.StocksActor;
import akka.actor.ActorRef;

public class FundQuote implements StockQuote {

  static Logger log = Logger.getGlobal();

  public static String getFundChange(List<Holding> holdings, ActorRef actor) {
    BigDecimal totalWeightedChange = new BigDecimal("0.0");
    BigDecimal totalPercentage = new BigDecimal("0.0");

    StockQuoteImpl rsq = new StockQuoteImpl();
    for (Holding holding : holdings) {
      try {
        if (holding != null && holding.symbol != null) {
          StockQuoteImpl.StockPage stockPage = rsq.parseStock(holding.symbol.trim());

          String changeToday = "" + rsq.newPercentage(holding.symbol.trim());
          changeToday = changeToday.replace('%', ' ').trim();

          log.info(holding + " Total: " + totalPercentage);

          BigDecimal weightedChange = (holding.percentage).multiply(new BigDecimal(changeToday)).divide(new BigDecimal("100.0"));
          totalWeightedChange = totalWeightedChange.add(weightedChange);

          totalPercentage = totalPercentage.add(holding.percentage); // percent of fund
          ProgressBar pb = new ProgressBar(totalPercentage.intValue());

          actor.tell(pb, StocksActor.stocksActor());
        }

      } catch (Exception e) {
        log.severe("Exception getting: " + holding.symbol);
        e.printStackTrace();
      }
    }

    return " fund changed " + totalWeightedChange + "% since markets last opened, this was calculated using " + totalPercentage + "% of holdings in the fund.";
  }

  private static String getURL(String name) {
    String url;

    url = "https://query.yahooapis.com/v1/public/yql?q=";
    url += URLEncoder.encode("select * from yahoo.finance.quotes ");
    url += URLEncoder.encode("where symbol in ('" + name + "')");
    url += "&format=json&diagnostics=true";
    url += "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    return url;
  }

  public static String getStockSymbol(String name) {

    name = name.replace("ASA", "");
    name = name.replace("Asa", "");
    name = name.replace("LTD", "");
    name = name.replace("Ltd", "");
    name = name.replace("Inc", "");
    name = name.replace("INC", "");
    name = name.replace("The", "");
    name = name.trim();
    if (name.length() > 15)
      name = name.substring(0, 15);

    String url = "https://s.yimg.com/aq/autoc?query=" + URLEncoder.encode(name) + "&region=US&lang=en-US&callback=YAHOO.util.UHScriptNodeDataSource" +
      ".callbacks&rnd=7780152812483450";
    String json = null;
    try {
      json = readUrl(url);
    } catch (Exception e) {
      e.printStackTrace();
    }
    int test = json.indexOf("symbol");
    //System.out.println(json);
    if (test != -1)
      return json.substring(test + 9, json.indexOf(',', test + 9)).replace('\"', ' ').trim();
    else
      return "";
  }

  private static String readUrl(String urlString) throws Exception {
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
      e.printStackTrace();
    } finally {
      if (reader != null)
        reader.close();
    }
    return "";
  }


  public List<Holding> getFundHoldings(String fund) {
    String csvFile = "public/csv/" + fund + ".csv";
    BufferedReader br = null;
    String line;
    List<Holding> retval = new ArrayList<>();
    try {

      br = new BufferedReader(new FileReader(csvFile));
      while ((line = br.readLine()) != null) {

        String[] lineValues = line.split(",");

        String percentStr = "0";
        if (lineValues.length > 1) {
          if (lineValues[1] != null) {
            percentStr = lineValues[1].replace('\"', ' ').replace(',', '.');
          }

          BigDecimal percentHolding = new BigDecimal(percentStr.replace("%", "").trim());

          if (lineValues.length >= 5) {
            retval.add(new Holding(lineValues[4], lineValues[0], percentHolding));
          } else {
            retval.add(new Holding(null, lineValues[0], percentHolding));
          }
        }
      }
      return retval;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return retval;
  }

  @Override
  public Double newPrice(String symbol) throws IOException {
    return -1.0;
  }

  @Override
  public String newPercentage(String symbol) {
    return null;
  }


  public static class Holding {
    public String name;
    BigDecimal percentage;
    public String symbol;

    public Holding(String symbol, String name, BigDecimal percentage) {
      this.symbol = symbol;
      this.name = name;
      this.percentage = percentage;
    }

    @Override
    public String toString() {
      return "Holding{" +
        "name='" + name + '\'' +
        ", percentage=" + new DecimalFormat("#0.0000").format(percentage) +
        ", symbol='" + symbol + '\'' +
        '}';
    }
  }

}
