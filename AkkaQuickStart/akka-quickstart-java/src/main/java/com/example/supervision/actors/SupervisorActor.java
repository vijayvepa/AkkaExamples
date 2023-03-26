package com.example.supervision.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class SupervisorActor extends AbstractBehavior<String> {

  private final ActorRef<String> child;

  public static Behavior<String> create() {
    return Behaviors.setup(SupervisorActor::new);
  }

  public SupervisorActor(ActorContext<String> context) {
    super(context);
    child = context.spawn(
        Behaviors.supervise(WorkerActor.create()).onFailure(SupervisorStrategy.restart()),
        "worker-actor");
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("FailChild", this::onFailChild)
        .build();
  }

  private Behavior<String> onFailChild() {

    child.tell("Fail");

    return this;

  }
}
