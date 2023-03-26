package com.example.actorlifecycle.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class StartStopActor1 extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(StartStopActor1::new);
  }

  public StartStopActor1(ActorContext<String> context) {
    super(context);
    System.out.println("First started");

    context.spawn(StartStopActor2.create(), "second");

  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("stop", Behaviors::stopped)
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private Behavior<String> onPostStop() {

    System.out.println("first stopped");
    return this;

  }
}
