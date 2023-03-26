package com.example.actorlifecycle.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class StartStopActor2 extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(StartStopActor2::new);
  }

  public StartStopActor2(ActorContext<String> context) {
    super(context);
    System.out.println("second started");
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private Behavior<String> onPostStop() {

    System.out.println("Second stopped");
    return this;

  }
}
