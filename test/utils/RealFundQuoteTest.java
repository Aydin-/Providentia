package utils;

import static junit.framework.TestCase.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;

public class RealFundQuoteTest {

  @Test
  public void testGetHoldings() throws IOException {
    running(fakeApplication(), () -> assertNotNull(FundQuote.getFundHoldings("AydinTest")));
  }


  @Test
  public void testGetFundChange() throws IOException {
    running(fakeApplication(), () -> {
      String testStr = FundQuote.getFundChange(FundQuote.getFundHoldings("AydinTest"), null, "AydinTest");
      assertNotNull(testStr);
    });
  }

  @Test
  public void testCurrencies() throws IOException {
    assertNotNull(FundQuote.getCurrencyHoldings("AydinTest"));
  }

  @Test
  public void testApplyCurrencies() throws IOException {
    assertNotNull(FundQuote.getCurrencyFactor("AydinTest", null));
  }

}


