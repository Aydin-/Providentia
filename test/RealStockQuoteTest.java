import java.io.IOException;

import org.junit.Test;

import utils.FundQuote;

public class RealStockQuoteTest {

  @Test
  public void realFundTest() throws IOException {
    FundQuote fundQuote = new FundQuote();

    System.out.println(FundQuote.getFundChange(fundQuote.getFundHoldings("DNBNorgeIndex"),null));//   System.out.println(fundQuote.getFundChange("DNBNorgeIndex"));
  }

}
