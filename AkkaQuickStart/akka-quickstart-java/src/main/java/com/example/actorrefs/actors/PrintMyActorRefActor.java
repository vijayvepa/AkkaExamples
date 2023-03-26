package com.example.actorrefs.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class PrintMyActorRefActor extends AbstractBehavior<String> {

  static Behavior<String> create() {
    return Behaviors.setup(PrintMyActorRefActor::new);
  }

  public PrintMyActorRefActor(ActorContext<String> context) {
    super(context);
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("printIt", this::printIt)
        .build();
  }

  private Behavior<String> printIt() {
    ActorRef<String> secondRef = getContext().spawn(Behaviors.empty(), "second-actor");
    System.out.println("Second: " + secondRef);
    return this;
  }
}
