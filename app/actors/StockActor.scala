package actors

import akka.actor.{Actor, ActorRef, Props}
import play.libs.Akka
import utils.{RealStockQuote, StockQuote}

import scala.collection.JavaConverters._
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of stock values.
 */

class StockActor(symbol: String) extends Actor {

  lazy val stockQuote: StockQuote = new RealStockQuote
  
  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  // A random data set which uses stockQuote.newPrice to get each data point
  var stockHistory: Queue[java.lang.Double] = {
    lazy val initialPrices: Stream[java.lang.Double] = stockQuote.newPrice(symbol) #:: initialPrices.map(previous => stockQuote.newPrice(symbol))
    initialPrices.take(5).to[Queue]
  }
  
  // Fetch the latest stock value every 10000ms TODO:
  val stockTick = context.system.scheduler.schedule(Duration.Zero, 2500.millis, self, FetchLatest)

  def receive = {
    case FetchLatest =>
      // add a new stock price to the history and drop the oldest
      val newPrice = stockQuote.newPrice(symbol)
      stockHistory = stockHistory.drop(1) :+ newPrice
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, newPrice, stockQuote.newPercentage(symbol)))
    case WatchStock(_) =>
      // send the stock history to the user
      sender ! StockHistory(symbol, stockHistory.asJava, stockQuote.newPercentage(symbol))
      // add the watcher to the list
      watchers = watchers + sender
    case UnwatchStock(_) =>
      watchers = watchers - sender
      if (watchers.size == 0) {
        stockTick.cancel()
        context.stop(self)
      }
    case FundUpdate(_) =>
      sender ! FundUpdate(symbol)

  }
}

class StocksActor extends Actor {
  def receive = {
    case watchStock @ WatchStock(symbol) =>
      // get or create the StockActor for the symbol and forward this message
      context.child(symbol).getOrElse {
        context.actorOf(Props(new StockActor(symbol)), symbol)
      } forward watchStock
    case unwatchStock @ UnwatchStock(Some(symbol)) =>
      // if there is a StockActor for the symbol forward this message
      context.child(symbol).foreach(_.forward(unwatchStock))
    case unwatchStock @ UnwatchStock(None) =>
      // if no symbol is specified, forward to everyone
      context.children.foreach(_.forward(unwatchStock))
  }
}

object StocksActor {
  lazy val stocksActor: ActorRef = Akka.system.actorOf(Props(classOf[StocksActor]))
}


case object FetchLatest

case class FundUpdate(percentage: String)

case class StockUpdate(symbol: String, price: Number, percentage: String)

case class StockHistory(symbol: String, history: java.util.List[java.lang.Double], percentage: String)

case class ProgressBar(percentChange: Integer);

case class WatchStock(symbol: String)

case class UnwatchStock(symbol: Option[String])