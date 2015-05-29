package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import actors.FundUpdate;
import actors.StocksActor;
import actors.UnwatchStock;
import actors.UserActor;
import actors.WatchStock;
import akka.actor.ActorRef;
import akka.actor.Props;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.Option;
import utils.RealFundQuote;


/**
 * The main web controller that handles returning the index page, setting up a WebSocket, and watching a stock.
 */
public class Application extends Controller {

  public static Result index() {
    return ok(views.html.index.render());
  }

  private static Map<String, String> symbolMap = new HashMap<String, String>(); //From name to symbol

  public static WebSocket<JsonNode> ws() {
    return new WebSocket<JsonNode>() {
      public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
        // create a new UserActor and give it the default stocks to watch
        final ActorRef userActor = Akka.system().actorOf(Props.create(UserActor.class, out));
        // send all WebSocket message to the UserActor
        in.onMessage(new F.Callback<JsonNode>() {
                       @Override
                       public void invoke(JsonNode jsonNode) throws Throwable {
                         RealFundQuote rfq = new RealFundQuote();

                         List<RealFundQuote.Holding> holdings = rfq.getFundHoldings(jsonNode.get("symbol").textValue());

                         for (RealFundQuote.Holding holding : holdings) {
                           String symbol = "";
                           if (symbolMap.get(holding.name) != null) {
                             symbol = symbolMap.get(holding.name);

                           } else {
                             if (holding.symbol != null && holding.symbol.length() > 1) { //symbol in CSV
                               symbol = holding.symbol;

                               symbolMap.put(holding.name, symbol);
                             } else {
                               String possibleSymbol = RealFundQuote.getStockSymbol(holding.name);
                               if (possibleSymbol.length() > 1) {
                                 symbol = possibleSymbol;

                                 symbolMap.put(holding.name, symbol);
                               } else {
                                 System.out.println("Still no symbol for " + holding.name);
                               }
                             }

                           }

                           if (symbol.length() > 1) {
                             System.out.println("Watching symbol: " + symbol);
                             WatchStock watchStock = new WatchStock(symbol.trim());
                             StocksActor.stocksActor().tell(watchStock, userActor);
                             userActor.tell(watchStock, StocksActor.stocksActor());
                             holding.symbol = symbol;

                           }
                         }

                         FundUpdate fundUpdate = new FundUpdate(jsonNode.get("symbol").textValue()  + RealFundQuote.getFundChange(holdings, userActor));
                         userActor.tell(fundUpdate, StocksActor.stocksActor());
                       }
                     }

        );

        in.onClose(new F.Callback0()

                   {
                     @Override
                     public void invoke() throws Throwable {
                       final Option<String> none = Option.empty();
                       //   StocksActor.stocksActor().tell(new UnwatchStock(none), userActor);
                       Akka.system().stop(userActor);
                     }
                   }

        );
      }
    }

      ;
  }
}