package shopping.cart.command;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import shopping.cart.ShoppingCartCommand;
import shopping.cart.model.Summary;

public record Checkout(ActorRef<StatusReply<Summary>> replyTo) implements ShoppingCartCommand {
}
