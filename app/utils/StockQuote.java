package utils;

import java.io.IOException;

public interface StockQuote {
    public Double newPrice(String symbol) throws IOException;
    public String newPercentage(String symbol);
}
