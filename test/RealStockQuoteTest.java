import java.io.IOException;

import org.junit.Test;

import utils.RealFundQuote;

public class RealStockQuoteTest {

  @Test
  public void realFundTest() throws IOException {
    RealFundQuote fundQuote = new RealFundQuote();

    System.out.println(RealFundQuote.getFundChange(fundQuote.getFundHoldings("DNBNorgeIndex")));
 //   System.out.println(fundQuote.getFundChange("DNBNorgeIndex"));
  }

}
