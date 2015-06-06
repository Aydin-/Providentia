import static junit.framework.TestCase.assertNotNull;
import static play.test.Helpers.fakeApplication;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import play.test.FakeApplication;
import utils.FundQuote;

public class RealFundQuoteTest {

  @Before
  public void startApplication() {
    FakeApplication fakeApp = fakeApplication();
  }

  @Test
  public void testGetHoldings() throws IOException {
    assertNotNull(FundQuote.getFundHoldings("AydinTest"));
  }

  @Test
  public void testGetFundChange() throws IOException {
    String testStr = FundQuote.getFundChange(FundQuote.getFundHoldings("AydinTest"), null, "AydinTest");
    assertNotNull(testStr);
    System.out.println("------ " + testStr);
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
