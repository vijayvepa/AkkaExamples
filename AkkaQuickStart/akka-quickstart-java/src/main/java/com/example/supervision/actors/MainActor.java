package com.example.supervision.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MainActor extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(MainActor::new);
  }

  public MainActor(ActorContext<String> context) {
    super(context);
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("Start", this::onStart)
        .build();
  }

  private Behavior<String> onStart() {

    final ActorRef<String> supervisorActor = getContext().spawn(SupervisorActor.create(), "SupervisorActor");
    supervisorActor.tell("FailChild");

    return this;

  }
}
