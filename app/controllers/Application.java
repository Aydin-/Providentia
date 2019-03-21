package controllers;

import actors.UserActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.MessageHandler;

public class Application extends Controller {
  public static Result index() {
    return ok(views.html.index.render());
  }

  private static MessageHandler handle = new MessageHandler();


  public static WebSocket<JsonNode> wss() {
    return new WebSocket<JsonNode>() {
      public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {

        final ActorRef userActor = Akka.system().actorOf(Props.create(UserActor.class, out));

        in.onMessage(jsonNode -> handle.onMessage(jsonNode, userActor, out)
        );

        in.onClose(() -> {
            handle.onClose(userActor);
            Akka.system().stop(userActor);
          }
        );
      }
    };
  }

}
