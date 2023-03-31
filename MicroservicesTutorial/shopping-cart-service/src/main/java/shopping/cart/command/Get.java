package shopping.cart.command;

import akka.actor.typed.ActorRef;
import shopping.cart.ShoppingCartCommand;
import shopping.cart.model.Summary;

public record Get(ActorRef<Summary> replyTo) implements ShoppingCartCommand {
}
