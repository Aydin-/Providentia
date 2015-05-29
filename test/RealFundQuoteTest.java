import java.io.IOException;

import org.junit.Test;

import utils.FundQuote;

public class RealFundQuoteTest {

  @Test
  public void realFundTest() throws IOException {
    FundQuote fundQuote = new FundQuote();

    System.out.println(fundQuote.getFundHoldings("DNBNorgeIndex"));
  }

}
