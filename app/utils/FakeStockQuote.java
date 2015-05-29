package utils;

/**
 * Placeholder class TODO: remove
 */
public class FakeStockQuote implements StockQuote {

  public Double newPrice(String symbol) {

    return -10.0;
  }

  @Override
  public String newPercentage(String symbol) {
    return null;
  }

}
