package bl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;

import actors.ProgressBar;
import actors.SkippedStocks;
import actors.StocksActor;
import akka.actor.ActorRef;
import utils.CSVReader;
import utils.RESTClient;

public final class FundQuote {
  static Logger log = Logger.getGlobal();

  public static final BigDecimal HUNDRED = new BigDecimal("100.00");
  public static final BigDecimal ZERO = new BigDecimal("0.0");
  public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private FundQuote() {
  }

  public static String getFundChange(List<Holding> holdings, ActorRef actor, String fundName) {
    BigDecimal totalWeightedChange = new BigDecimal("0.0").setScale(9, BigDecimal.ROUND_HALF_UP);
    BigDecimal totalPercentage = new BigDecimal("0.0").setScale(3, BigDecimal.ROUND_HALF_UP);
    String skipped = null;
    String changeToday = "0.0";
    BigDecimal weightedChange;
    for (Holding holding : holdings) {
      try {
        if (holding != null && holding.symbol != null) {
          changeToday = "" + StockQuote.newPercentageStatic(holding.symbol.trim());
          changeToday = changeToday.replace('%', ' ').trim();

          weightedChange = (holding.percentage).multiply(new BigDecimal(changeToday)).divide(HUNDRED, 9, BigDecimal.ROUND_HALF_UP);

          totalWeightedChange = totalWeightedChange.add(weightedChange);
          totalPercentage = totalPercentage.add(holding.percentage); // percent of fund

          ProgressBar pb = new ProgressBar(totalPercentage.intValue());
          if (actor != null) {
            actor.tell(pb, StocksActor.stocksActor());
          }
        } else {
          if (holding != null) {
            skipped = "☻" + (holding.name) + " (" + holding.percentage.setScale(2, BigDecimal.ROUND_HALF_UP) + "%)";
          }

          if (skipped != null && actor != null) {
            SkippedStocks skippedStocks = new SkippedStocks(skipped);
            actor.tell(skippedStocks, StocksActor.stocksActor());
          }
        }
      } catch (NumberFormatException nfe) {
        log.severe("Number format exception " + nfe.getMessage());
      } catch (Exception e) {
        if (holding != null)
          log.severe("Exception getting: " + holding.name + " - " + holding.percentage + " - " + changeToday);
        e.printStackTrace();
      }
    }

    BigDecimal currencyFactor = getCurrencyFactor(fundName, actor);
    BigDecimal stockChange = totalWeightedChange;
    totalWeightedChange = (HUNDRED.add(stockChange)).multiply((new BigDecimal("1.0").add(currencyFactor))).subtract(HUNDRED);

    BigDecimal fundChangeDisp = totalWeightedChange.setScale(2, BigDecimal.ROUND_HALF_UP);
    String fundChangeDispLabel = "changed";

    if (fundChangeDisp.compareTo(ZERO) > 0) {
      fundChangeDispLabel = "increased";
    } else if (fundChangeDisp.compareTo(ZERO) < 0) {
      fundChangeDispLabel = "decreased";
    }

    return " fund " + fundChangeDispLabel + " " + fundChangeDisp.abs() + "% since markets last opened, " +
      "this was calculated using " + totalPercentage.setScale(2, BigDecimal.ROUND_HALF_UP) + "% of holdings in the fund." +
      " Stocks changed " + stockChange.setScale(2, BigDecimal.ROUND_HALF_UP) + "% and currencies changed " +
      currencyFactor.multiply(HUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
  }

  public static String trimHoldingName(String name) {
    name = name.toLowerCase();
    if (name.startsWith("as "))
      name = name.substring(2);

    String[] stopWords = {"_", "a-shares", "b-shares", "c-shares", "corp/the", "inc/ii", "asa/the", " as ", " sa ", "inc/the", "co/the", " ltd",
      "the ", "-", " co ", "2012", "company", " & co", "/de", "/mn", "plc", " cos ", "'s"};

    String[] endWords = {" ag", " as", " sa", " asa", " inc"};

    for (String word : stopWords) {
      name = name.replace(word, " ");
    }

    for (String endWord : endWords) {
      if (name.endsWith(endWord)) {
        name = name.substring(0, name.length() - 1 - endWord.length());
      }

    }

    name = name.trim();
    return name;
  }

  public static Calendar getPreviousCloseDate() {
    Calendar cal = Calendar.getInstance();

    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

    if ((dayOfWeek > Calendar.MONDAY) && (dayOfWeek <= Calendar.FRIDAY)) {
      cal.add(Calendar.DATE, -1);
    } else {
      if (dayOfWeek == Calendar.SATURDAY) {
        cal.add(Calendar.DATE, -2);
      } else if (dayOfWeek == Calendar.SUNDAY) {
        cal.add(Calendar.DATE, -3);
      } else {
        cal.add(Calendar.DATE, -4);
      }
    }
    return cal;
  }

  public static BigDecimal getCurrencyFactor(String fundName, ActorRef actor) {
    Map<String, BigDecimal> currencyMap = CSVReader.getCurrencyHoldings(fundName);

    BigDecimal usdChange;
    FundQuote.CurrencyPage yesterdayPage, todayPage;
    String ratesYesterdayURL = "https://openexchangerates.org/api/historical/" + dateFormat.format(getPreviousCloseDate().getTime()) +
      ".json?app_id=90abee42f3444757a1cd0fead7febc96";
    String ratesLatestURL = "https://openexchangerates.org/api/latest.json?app_id=90abee42f3444757a1cd0fead7febc96";

    try {
      String yesterdayJson = RESTClient.readUrl(ratesYesterdayURL);
      String todayJson = RESTClient.readUrl(ratesLatestURL);

      yesterdayPage = parseCurrencyPage(yesterdayJson);
      todayPage = parseCurrencyPage(todayJson);

      BigDecimal usdToNok = todayPage.rates.get("NOK");
      BigDecimal usdToNokYesterday = yesterdayPage.rates.get("NOK");

      usdChange = usdToNok.divide(usdToNokYesterday, 9, BigDecimal.ROUND_HALF_UP).multiply(HUNDRED).subtract(HUNDRED);
      usdChange = usdChange.divide(HUNDRED, 9, BigDecimal.ROUND_HALF_UP);

    } catch (Exception e) {
      e.printStackTrace();
      return new BigDecimal("1.0");
    }

    BigDecimal totalWeightedChange = new BigDecimal("0.0");
    BigDecimal totalPercentage = new BigDecimal("0.0");

    for (String currency : currencyMap.keySet()) {

      BigDecimal holdingPercentage = currencyMap.get(currency);
      totalPercentage = totalPercentage.add(holdingPercentage);

      ProgressBar pb = new ProgressBar(totalPercentage.intValue());
      if (actor != null)
        actor.tell(pb, StocksActor.stocksActor());

      if (currency.equals("USD")) {
        log.info("USD change: " + usdChange);
        totalWeightedChange = totalWeightedChange.add(usdChange.multiply(holdingPercentage.divide(HUNDRED, 9, BigDecimal.ROUND_HALF_UP)));
      } else if (!currency.equals("NOK")) {
        BigDecimal todayPrice = todayPage.rates.get(currency);
        BigDecimal yesterdayPrice = yesterdayPage.rates.get(currency);

        BigDecimal change = todayPrice.divide(yesterdayPrice, 9, BigDecimal.ROUND_HALF_UP).multiply(HUNDRED).subtract(HUNDRED);

        change = change.divide(HUNDRED, 9, BigDecimal.ROUND_HALF_UP);
        change = usdChange.add(change);
        log.info(currency + " change vs NOK" + change);

        totalWeightedChange = totalWeightedChange.add(change.multiply(holdingPercentage.divide(HUNDRED, 9, BigDecimal.ROUND_HALF_UP)));
        log.info(currency + " total weighted change: " + totalWeightedChange.toPlainString());
      }
    }
    ProgressBar pb = new ProgressBar(100);
    if (actor != null) {
      actor.tell(pb, StocksActor.stocksActor());
    }

    return totalWeightedChange;
  }

  public static String getStockSymbol(String name) {
    name = trimHoldingName(name);
    if (name.length() > 10)
      name = name.substring(0, 10);

    log.info("Getting stock symbol for " + name);

    String url = "https://s.yimg.com/aq/autoc?query=" + URLEncoder.encode(name) + "&region=US&lang=en-US&callback=YAHOO.util.UHScriptNodeDataSource" +
      ".callbacks&rnd=7780152812483450";
    String json = null;
    try {
      json = RESTClient.readUrl(url);
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

  public static CurrencyPage parseCurrencyPage(String json) throws Exception {
    Gson gson = new Gson();
    return gson.fromJson(json, CurrencyPage.class);
  }

  public static class CurrencyPage {
    Map<String, BigDecimal> rates;
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
