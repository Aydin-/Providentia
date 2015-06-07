package utils;

import static junit.framework.TestCase.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.junit.Test;

import db.DatabaseConfig;
import utils.FundQuote;

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
    assertNotNull(FundQuote.applyCurrencies("AydinTest", new BigDecimal("1"), null));
  }

}


