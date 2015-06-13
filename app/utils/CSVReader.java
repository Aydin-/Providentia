package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typesafe.config.ConfigFactory;

import bl.FundQuote;

public class CSVReader {
  private static final String testPath = ConfigFactory.load().getString("testpath");
  private static final String path = ConfigFactory.load().getString("path");

  public static List<FundQuote.Holding> getFundHoldings(String fund) {
    String csvFile = path + fund + ".csv";
    BufferedReader br = null;
    String line;
    List<FundQuote.Holding> retval = new ArrayList<>();
    try {

      br = new BufferedReader(new FileReader(csvFile));
      while ((line = br.readLine()) != null) {

        String[] lineValues = line.split(",");

        String percentStr = "0";
        if (lineValues.length > 1) {
          if (lineValues[1] != null) {
            percentStr = lineValues[1].replace('\"', ' ').replace(',', '.');
          }

          BigDecimal percentHolding = new BigDecimal(percentStr.replace("%", "").trim()).setScale(3, BigDecimal.ROUND_HALF_UP);

          if (lineValues.length >= 5) {
            retval.add(new FundQuote.Holding(lineValues[4], lineValues[0], percentHolding));
          } else {
            retval.add(new FundQuote.Holding(null, lineValues[0], percentHolding));
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
}
