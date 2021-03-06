package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.FundUpdate;
import actors.StocksActor;
import actors.UserActor;
import actors.UnwatchStock;
import actors.WatchStock;
import akka.actor.ActorRef;
import bl.FundQuote;
import db.DatabaseAO;
import play.libs.Json;
import play.mvc.WebSocket;

public class MessageHandler {

	public static Logger log = Logger.getAnonymousLogger();
	private Map<String, String> symbolMap = new HashMap<String, String>(); // From name to symbol

	public void onMessage(JsonNode jsonNode, ActorRef userActor, WebSocket.Out<JsonNode> out) {

		List<FundQuote.Holding> holdings = CSVReader.getFundHoldings(jsonNode.get("symbol").textValue());

		Double progress = 0.0;
		Double counter = 1.0;

		for (String symbol : symbolMap.values()) { // unwatch previous fund
			StocksActor.stocksActor().tell(new UnwatchStock(StocksActor.getOptionString(symbol)), userActor);
			log.log(Level.INFO, "Unwatching " + symbol);
		}

		String symbol;
		for (FundQuote.Holding holding : holdings) {
			symbol = "";
			if (symbolMap.get(holding.name) != null) { // check cache
				symbol = symbolMap.get(holding.name);
			} else {
				String symbolFromDb = new DatabaseAO().getStockSymbol(holding.name); // check db
				if (symbolFromDb != null) {
					symbol = symbolFromDb;
					symbolMap.put(holding.name, symbol);
				} else if (holding.symbol != null && holding.symbol.length() > 0) { // symbol in CSV
					symbol = holding.symbol;
					symbolMap.put(holding.name, symbol);
					new DatabaseAO().insertStockSymbol(holding.name, symbol);
				} else {
					String possibleSymbol = FundQuote.getStockSymbol(holding.name);
					if (possibleSymbol.length() > 0) {
						symbol = possibleSymbol;
						symbolMap.put(holding.name, symbol);
						new DatabaseAO().insertStockSymbol(holding.name, symbol);
					} else {
						log.log(Level.WARNING, "Still no symbol for " + holding.name);

					}
				}
			}

			if (symbol.length() > 0) {
				WatchStock watchStock = new WatchStock(symbol.trim());
				StocksActor.stocksActor().tell(watchStock, userActor);
				userActor.tell(watchStock, StocksActor.stocksActor());
				holding = new FundQuote.Holding(symbol, holding.name, holding.percentage);
			} else {
				log.warning("Symbol:" + symbol + " rejected.");
			}

			progress = counter / holdings.size() * 100.0;
			counter++;

			ObjectNode progressBarMessage = Json.newObject();
			progressBarMessage.put(UserActor.TYPE, UserActor.TYPE_PROGRESS_BAR);
			progressBarMessage.put(UserActor.TOTAL_PERCENTAGE, progress.intValue());
			progressBarMessage.put(UserActor.PROGRESS_MESSAGE,
					("Getting fund holdings " + ((symbol.length() > 0) ? "[" + symbol + "]" : " ") + " - "));
			out.write(progressBarMessage);
		}

		FundUpdate fundUpdate = new FundUpdate(jsonNode.get("symbol").textValue().replace("DNB", "DNB ")
				+ FundQuote.getFundChange(holdings, userActor, jsonNode.get("symbol").textValue()));

		userActor.tell(fundUpdate, StocksActor.stocksActor());
	}

	public void onClose(ActorRef userActor) {
		StringBuffer sb = new StringBuffer();
		for (String symbol : symbolMap.values()) {
			StocksActor.stocksActor().tell(new UnwatchStock(StocksActor.getOptionString(symbol)), userActor);
			sb = sb.append(symbol + " , ");
		}
		log.log(Level.INFO, "Unwatching " + sb.toString());

	}
}
