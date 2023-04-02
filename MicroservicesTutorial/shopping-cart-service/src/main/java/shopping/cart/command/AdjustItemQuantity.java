package shopping.cart.command;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import shopping.cart.CborSerializable;
import shopping.cart.ShoppingCartCommand;
import shopping.cart.model.Summary;

public record AdjustItemQuantity(String itemId, int updatedQuantity, ActorRef<StatusReply<Summary>> replyTo) implements ShoppingCartCommand, CborSerializable {
}
