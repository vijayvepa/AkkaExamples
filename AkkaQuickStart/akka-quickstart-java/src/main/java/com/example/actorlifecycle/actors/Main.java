package com.example.actorlifecycle.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Main extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(Main::new);
  }

  public Main(ActorContext<String> context) {
    super(context);
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("start", this::start)
        .build();
  }

  private Behavior<String> start() {

    final ActorRef<String> first = getContext().spawn(StartStopActor1.create(), "first");
    first.tell("stop");

    return Behaviors.same();

  }
}
