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
import actors.SkippedStocks;
import actors.StocksActor;
import akka.actor.ActorRef;

public class FundQuote implements StockQuote {

  static Logger log = Logger.getGlobal();

  public static String getFundChange(List<Holding> holdings, ActorRef actor) {
    BigDecimal totalWeightedChange = new BigDecimal("0.0");
    BigDecimal totalPercentage = new BigDecimal("0.0");
    String skipped = null;

    for (Holding holding : holdings) {
      try {
        if (holding != null && holding.symbol != null) {
          String changeToday = "" + StockQuoteImpl.newPercentageStatic(holding.symbol.trim());
          changeToday = changeToday.replace('%', ' ').trim();

          log.info(holding + " Total: " + totalPercentage);

          BigDecimal weightedChange = (holding.percentage).multiply(new BigDecimal(changeToday)).divide(new BigDecimal("100.0"));
          totalWeightedChange = totalWeightedChange.add(weightedChange);

          totalPercentage = totalPercentage.add(holding.percentage); // percent of fund
          ProgressBar pb = new ProgressBar(totalPercentage.intValue());
          actor.tell(pb, StocksActor.stocksActor());
        } else {
          if (holding != null) {
            if(skipped == null){
              skipped = "Skipped: -" + (holding.name);
            } else {
              skipped = "-" + (holding.name);
            }
          }

          if(skipped!=null){
            SkippedStocks skippedStocks = new SkippedStocks(skipped);
            actor.tell(skippedStocks, StocksActor.stocksActor());
          }

        }

      } catch (Exception e) {
        if (holding != null)
          log.severe("Exception getting: " + holding.name+ " "+holding.percentage);
        e.printStackTrace();
      }
    }

    return " fund changed " + totalWeightedChange + "% since markets last opened, this was calculated using " + totalPercentage + "% of holdings in the fund.";
  }

  public static String trimHoldingName(String name) {
    name = name.toLowerCase();
    if (name.startsWith("as "))
      name = name.substring(2);
    if (name.endsWith(" as"))
      name = name.substring(0, name.length() - 3);

    name = name.replace("asa/the", "");
    name = name.replace(" asa", "");
    name = name.replace(" as ", " ");
    name = name.replace(" ltd", "");
    name = name.replace(" inc", "");
    name = name.replace("the ", " ");
    name = name.replace("-", " ");
    name = name.replace("_", " ");
    name = name.replace(" co ", " ");
    name = name.replace("2012", " ");
    name = name.trim();
    return name;
  }

  public static String getStockSymbol(String name) {
    name = trimHoldingName(name);
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
    if (json == null)
      return "";

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
