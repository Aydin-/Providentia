package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;

import actors.ProgressBar;
import actors.SkippedStocks;
import actors.StocksActor;
import akka.actor.ActorRef;

public class FundQuote {

  static Logger log = Logger.getGlobal();

  private static final String path = "/public/csv/";

  public static String getFundChange(List<Holding> holdings, ActorRef actor, String fundName) {
    BigDecimal totalWeightedChange = new BigDecimal("0.0");
    BigDecimal totalPercentage = new BigDecimal("0.0");
    String skipped = null;

    for (Holding holding : holdings) {
      try {
        if (holding != null && holding.symbol != null) {
          String changeToday = "" + StockQuote.newPercentageStatic(holding.symbol.trim());
          changeToday = changeToday.replace('%', ' ').trim();

          log.info(holding + " Total: " + totalPercentage);

          BigDecimal weightedChange = (holding.percentage).multiply(new BigDecimal(changeToday)).divide(new BigDecimal("100.0"));
          totalWeightedChange = totalWeightedChange.add(weightedChange);

          totalPercentage = totalPercentage.add(holding.percentage); // percent of fund

          ProgressBar pb = new ProgressBar(totalPercentage.intValue());
          if (actor != null) {
            actor.tell(pb, StocksActor.stocksActor());
          }
        } else {
          if (holding != null) {
            if (skipped == null) {
              skipped = "Skipped: -" + (holding.name);
            } else {
              skipped = "-" + (holding.name);
            }
          }

          if (skipped != null && actor != null) {
            SkippedStocks skippedStocks = new SkippedStocks(skipped);
            actor.tell(skippedStocks, StocksActor.stocksActor());
          }

        }

      } catch (Exception e) {
        if (holding != null)
          log.severe("Exception getting: " + holding.name + " " + holding.percentage);
        e.printStackTrace();
      }
    }
    totalWeightedChange = applyCurrencies(fundName, totalWeightedChange, actor);

    return " fund changed " + totalWeightedChange + "% since markets last opened, this was calculated using " + totalPercentage + "% of holdings in the fund.";
  }

  public static String trimHoldingName(String name) {
    name = name.toLowerCase();
    if (name.startsWith("as "))
      name = name.substring(2);
    if (name.endsWith(" as"))
      name = name.substring(0, name.length() - 3);

    String[] stopWords = {"asa/the", " asa", " as ", " sa ", "inc/the", "co/the", " ltd", " inc", "the ", "-", " co ", "2012"};

    for (String word : stopWords) {
      name = name.replace(word, " ");
    }
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


  public static List<Holding> getFundHoldings(String fund) {
    String csvFile = path + fund + ".csv";
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

  public static Map<String, BigDecimal> getCurrencyHoldings(String fund) {
    String csvFile = path + fund + "_Currency.csv";
    BufferedReader br = null;
    String line;
    Map<String, BigDecimal> retval = new HashMap<>();
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
          String currency = lineValues[0];

          retval.put(currency, percentHolding);
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

  public static BigDecimal applyCurrencies(String fundName, BigDecimal percentChange, ActorRef actor) {
    Map<String, BigDecimal> currencyMap = getCurrencyHoldings(fundName);

    Calendar cal = Calendar.getInstance();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    cal.add(Calendar.DATE, -1);

    BigDecimal usdChange = new BigDecimal("1.0");
    CurrencyPage yesterdayPage = new CurrencyPage(), todayPage = new CurrencyPage();
    try {

      String ratesYesterdayURL = "https://openexchangerates.org/api/historical/" + dateFormat.format(cal.getTime()) +
        ".json?app_id=90abee42f3444757a1cd0fead7febc96";
      String ratesLatestURL = "https://openexchangerates.org/api/latest.json?app_id=90abee42f3444757a1cd0fead7febc96";

      String yesterdayJson = readUrl(ratesYesterdayURL);
      String todayJson = readUrl(ratesLatestURL);

      yesterdayPage = parseCurrencyPage(yesterdayJson);
      todayPage = parseCurrencyPage(todayJson);

      BigDecimal usdToNok = todayPage.rates.get("NOK");
      BigDecimal usdToNokYesterday = yesterdayPage.rates.get("NOK");

      usdChange = usdToNok.divide(usdToNokYesterday, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100.0")).subtract(new BigDecimal("100.0"));

    } catch (Exception e) {
      e.printStackTrace();
    }

    Map<String, BigDecimal> todayMap = new HashMap<String, BigDecimal>();
    Map<String, BigDecimal> yesterdayMap = new HashMap<String, BigDecimal>();
    Map<String, BigDecimal> changeMap = new HashMap<String, BigDecimal>();

    BigDecimal totalWeightedChange = new BigDecimal("0.0");
    BigDecimal totalPercentage = new BigDecimal("0.0");

    for (String currency : currencyMap.keySet()) {
      if (currency.equals("NOK")) {

        totalPercentage = totalPercentage.add(currencyMap.get(currency));
        continue;
      } else if (currency.equals("USD")) {

        totalPercentage = totalPercentage.add(currencyMap.get(currency));
        totalWeightedChange = totalWeightedChange.add(usdChange.divide(new BigDecimal("100.0"), BigDecimal.ROUND_HALF_UP).multiply(currencyMap.get(currency).divide(new BigDecimal("100.0"), BigDecimal.ROUND_HALF_UP)));
        continue;
      }

      ProgressBar pb = new ProgressBar(totalPercentage.intValue());
      if (actor != null)
        actor.tell(pb, StocksActor.stocksActor());

      BigDecimal todayPrice = todayPage.rates.get(currency);
      BigDecimal yesterdayPrice = yesterdayPage.rates.get(currency);
      BigDecimal holdingPercentage = currencyMap.get(currency);

      totalPercentage.add(holdingPercentage);
      todayMap.put(currency, todayPrice);

      if (todayPrice == null)
        throw new NullPointerException("today price null" + currency);
      if (yesterdayPrice == null)
        throw new NullPointerException("yesterday price null");
      if (usdChange == null)
        throw new NullPointerException("usdchange price null");

      BigDecimal changeVsNok = usdChange.add(todayPrice.divide(yesterdayPrice.multiply(new BigDecimal("100.000001")).subtract(new BigDecimal("100.0")), 9, BigDecimal.ROUND_HALF_UP));

      totalWeightedChange = totalWeightedChange.add(changeVsNok.divide(new BigDecimal("100.0"), BigDecimal.ROUND_HALF_UP).multiply(holdingPercentage.divide(new BigDecimal("100.0"), BigDecimal.ROUND_HALF_UP)));
      log.info(currency + "total weighted change: " + totalWeightedChange.toPlainString());
    }
    ProgressBar pb = new ProgressBar(100);
    if (actor != null)
      actor.tell(pb, StocksActor.stocksActor());

    totalWeightedChange.add(new BigDecimal("1.0"));

    return totalWeightedChange.multiply(percentChange);
  }

  static class Resource {
    MyResource resource;

  }

  static class MyResource {

    String id;
    String name;
    String ask;
  }

  public static CurrencyPage parseCurrencyPage(String json) throws Exception {
    Gson gson = new Gson();
    return gson.fromJson(json, CurrencyPage.class);
  }

  public static class CurrencyPage {
    Map<String, BigDecimal> rates;
  }

  public static class Rates {
    String currency;
    BigDecimal rate;
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
