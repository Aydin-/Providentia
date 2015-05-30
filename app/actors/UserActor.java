package actors;

import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Play;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.List;

/**
 * The broker between the WebSocket and the StockActor(s).  The UserActor holds the connection and sends serialized
 * JSON data to the client.
 */

public class UserActor extends UntypedActor {

  private final WebSocket.Out<JsonNode> out;
  private int counter = 1;

  public UserActor(WebSocket.Out<JsonNode> out) {
    this.out = out;

    // watch the default stocks
    List<String> defaultStocks = Play.application().configuration().getStringList("default.stocks");

    for (String stockSymbol : defaultStocks) {
      StocksActor.stocksActor().tell(new WatchStock(stockSymbol), getSelf());
    }
  }

  public void addSymbol(String symbol) {
    StocksActor.stocksActor().tell(new WatchStock(symbol), getSelf());
  }

  public void onReceive(Object message) {
    System.out.println("got message" +message);
    if (message instanceof StockUpdate) {
      // push the stock to the client
      StockUpdate stockUpdate = (StockUpdate) message;
      ObjectNode stockUpdateMessage = Json.newObject();
      stockUpdateMessage.put("type", "stockupdate");
      stockUpdateMessage.put("symbol", stockUpdate.symbol());
      stockUpdateMessage.put("price", stockUpdate.price().doubleValue());
      stockUpdateMessage.put("percentage", stockUpdate.percentage());
      out.write(stockUpdateMessage);
    } else if (message instanceof FundUpdate) {
      // push the stock to the client
      FundUpdate fundUpdate = (FundUpdate) message;
      ObjectNode fundUpdateMessage = Json.newObject();
      fundUpdateMessage.put("type", "fundupdate");
      fundUpdateMessage.put("percentage", fundUpdate.percentage());

      out.write(fundUpdateMessage);
    } else if (message instanceof StockHistory) {
      // push the history to the client
      StockHistory stockHistory = (StockHistory) message;

      ObjectNode stockUpdateMessage = Json.newObject();
      stockUpdateMessage.put("type", "stockhistory");
      stockUpdateMessage.put("symbol", stockHistory.symbol());
      stockUpdateMessage.put("percentage", stockHistory.percentage());

      ArrayNode historyJson = stockUpdateMessage.putArray("history");
      for (Object price : stockHistory.history()) {
        historyJson.add(((Number) price).doubleValue());
      }

      out.write(stockUpdateMessage);
    } else if (message instanceof ProgressBar) {
      ProgressBar progressBar = (ProgressBar) message;
      ObjectNode progressBarMessage = Json.newObject();
      progressBarMessage.put("type", "progressbar");
      progressBarMessage.put("totalPercentage", progressBar.percentChange());
      progressBarMessage.put("progressMessage", "Estimating value change..");
      out.write(progressBarMessage);

    }
  }
}
