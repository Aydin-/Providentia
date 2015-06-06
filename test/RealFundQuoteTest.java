import static junit.framework.TestCase.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import utils.FundQuote;

public class RealFundQuoteTest {

  @Test
  public void testGetHoldings() throws IOException {
    assertNotNull(FundQuote.getFundHoldings("DNBNorgeIndex"));
  }

  @Test
  public void testGetFundChange() throws IOException {
    assertNotNull(FundQuote.getFundChange(FundQuote.getFundHoldings("DNBNorgeIndex"), null));
  }

  @Test
  public void testApplyCurrencies() throws IOException {
    assertNotNull(FundQuote.getCurrencyHoldings("DNBGlobalIndex"));
  }

}
