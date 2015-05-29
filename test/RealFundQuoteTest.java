import java.io.IOException;

import org.junit.Test;

import utils.RealFundQuote;

public class RealFundQuoteTest {

  @Test
  public void realFundTest() throws IOException {
    RealFundQuote fundQuote = new RealFundQuote();

    System.out.println(fundQuote.getFundHoldings("DNBNorgeIndex"));
  }

}
