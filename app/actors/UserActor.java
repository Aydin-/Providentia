package actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.UntypedActor;
import play.libs.Json;
import play.mvc.WebSocket;

public class UserActor extends UntypedActor {

  private final WebSocket.Out<JsonNode> out;
  
  public static final String TYPE = "type";
  public static final String SYMBOL = "symbol";
  public static final String PRICE = "price";
  public static final String PERCENTAGE = "percentage";
  public static final String TOTAL_PERCENTAGE = "totalPercentage";
  public static final String NAME = "name";
  public static final String PROGRESS_MESSAGE = "progressMessage";
		  
  public static final String TYPE_STOCK_UPDATE = "stockupdate";
  public static final String TYPE_FUND_UPDATE = "fundupdate";
  public static final String TYPE_STOCK_HISTORY = "stockhistory";
  public static final String TYPE_PROGRESS_BAR = "progressbar";
  public static final String TYPE_SKIPPED_STOCKS = "skippedstocks";

  public UserActor(WebSocket.Out<JsonNode> out) {
    this.out = out;
  }

  public void onReceive(Object message) {

    if (message instanceof StockUpdate) {
      // push the stock to the client
      StockUpdate stockUpdate = (StockUpdate) message;
      ObjectNode stockUpdateMessage = Json.newObject();
      stockUpdateMessage.put(TYPE, TYPE_STOCK_UPDATE);
      stockUpdateMessage.put(SYMBOL, stockUpdate.symbol());
      stockUpdateMessage.put(PRICE, stockUpdate.price().doubleValue());
      stockUpdateMessage.put(PERCENTAGE, stockUpdate.percentage());
      out.write(stockUpdateMessage);
    } else if (message instanceof FundUpdate) {

      FundUpdate fundUpdate = (FundUpdate) message;
      ObjectNode fundUpdateMessage = Json.newObject();
      fundUpdateMessage.put(TYPE, TYPE_FUND_UPDATE);
      fundUpdateMessage.put(PERCENTAGE, fundUpdate.percentage());

      out.write(fundUpdateMessage);
    } else if (message instanceof StockHistory) {

      StockHistory stockHistory = (StockHistory) message;

      ObjectNode stockUpdateMessage = Json.newObject();
      stockUpdateMessage.put(TYPE, TYPE_STOCK_HISTORY);
      stockUpdateMessage.put(SYMBOL, stockHistory.symbol());
      stockUpdateMessage.put(PERCENTAGE, stockHistory.percentage());

      ArrayNode historyJson = stockUpdateMessage.putArray("history");
      
      for (Object price : stockHistory.history()) {
        historyJson.add(((Number) price).doubleValue());
      }

      out.write(stockUpdateMessage);
    } else if (message instanceof ProgressBar) {
    	
      ProgressBar progressBar = (ProgressBar) message;
      ObjectNode progressBarMessage = Json.newObject();
      progressBarMessage.put(TYPE, TYPE_PROGRESS_BAR);
      progressBarMessage.put(TOTAL_PERCENTAGE, progressBar.percentChange());
      progressBarMessage.put(PROGRESS_MESSAGE, "Estimating value change..");
      out.write(progressBarMessage);

    }else if (message instanceof SkippedStocks) {
      SkippedStocks skippedStocks = (SkippedStocks) message;
      ObjectNode skippedStocksMessage = Json.newObject();
      skippedStocksMessage.put(TYPE, TYPE_SKIPPED_STOCKS);
      skippedStocksMessage.put(NAME, skippedStocks.name());
      out.write(skippedStocksMessage);

    }
  }
}
