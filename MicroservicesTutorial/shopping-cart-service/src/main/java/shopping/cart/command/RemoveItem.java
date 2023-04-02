package shopping.cart.command;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import shopping.cart.model.Summary;

public record RemoveItem(String itemId,  ActorRef<StatusReply<Summary>> replyTo) {
}
