package shopping.cart.command;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import common.CborSerializable;
import shopping.cart.ShoppingCartCommand;
import shopping.cart.model.Summary;

public record RemoveItem(String itemId,  ActorRef<StatusReply<Summary>> replyTo) implements ShoppingCartCommand, CborSerializable {
}
