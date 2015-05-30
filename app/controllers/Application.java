package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.FundUpdate;
import actors.StocksActor;
import actors.UnwatchStock;
import actors.UserActor;
import actors.WatchStock;
import akka.actor.ActorRef;
import akka.actor.Props;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.Option;
import utils.FundQuote;


/**
 * The main web controller that handles returning the index page, setting up a WebSocket, and watching a stock.
 */
public class Application extends Controller {
  static Logger log = Logger.getGlobal();

  public static Result index() {
    return ok(views.html.index.render());
  }

  private static Map<String, String> symbolMap = new HashMap<String, String>(); //From name to symbol

  public static WebSocket<JsonNode> wss() {
    return new WebSocket<JsonNode>() {
      public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
        // create a new UserActor and give it the default stocks to watch
        final ActorRef userActor = Akka.system().actorOf(Props.create(UserActor.class, out));
        // send all WebSocket message to the UserActor
        in.onMessage(jsonNode -> {
          FundQuote rfq = new FundQuote();

          List<FundQuote.Holding> holdings = rfq.getFundHoldings(jsonNode.get("symbol").textValue());
          Double progress = 0.0;
          Double counter = 1.0;

          for (String symbol : symbolMap.values()) { //unwatch previous fund
            StocksActor.stocksActor().tell(new UnwatchStock(StocksActor.getOptionString(symbol)), userActor);
            log.log(Level.INFO, "Unwatching " + symbol);
          }

          String symbol;
          for (FundQuote.Holding holding : holdings) {
            symbol = "";
            if (symbolMap.get(holding.name) != null) {
              symbol = symbolMap.get(holding.name);

            } else {
              if (holding.symbol != null && holding.symbol.length() > 1) { //symbol in CSV
                symbol = holding.symbol;

                symbolMap.put(holding.name, symbol);
              } else {
                String possibleSymbol = FundQuote.getStockSymbol(holding.name);
                if (possibleSymbol.length() > 1) {
                  symbol = possibleSymbol;

                  symbolMap.put(holding.name, symbol);
                } else {
                  log.log(Level.WARNING, "Still no symbol for " + holding.name);

                }
              }
            }

            if (symbol.length() > 1) {
              log.info("Watching symbol: " + symbol);
              WatchStock watchStock = new WatchStock(symbol.trim());
              StocksActor.stocksActor().tell(watchStock, userActor);
              userActor.tell(watchStock, StocksActor.stocksActor());
              holding.symbol = symbol;
            }

            progress = counter / holdings.size() * 100.0;
            counter++;

            if (symbol != null) {
              ObjectNode progressBarMessage = Json.newObject();
              progressBarMessage.put("type", "progressbar");
              progressBarMessage.put("totalPercentage", progress.intValue());
              progressBarMessage.put("progressMessage", ("Getting fund holdings [" + symbol + "] - "));
              out.write(progressBarMessage);
            }

          }
          FundUpdate fundUpdate = new FundUpdate(jsonNode.get("symbol").textValue() + FundQuote.getFundChange(holdings, userActor));
          userActor.tell(fundUpdate, StocksActor.stocksActor());
        }

        );

        in.onClose(() -> {
          final Option<String> none = Option.empty();

          for (String symbol : symbolMap.values()) {
            StocksActor.stocksActor().tell(new UnwatchStock(StocksActor.getOptionString(symbol)), userActor);
            log.log(Level.INFO, "Unwatching " + symbol);
          }

          Akka.system().stop(userActor);
        }

        );
      }
    }

      ;
  }
}