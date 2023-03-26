package com.example.supervision.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WorkerActor extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(WorkerActor::new);
  }

  public WorkerActor(ActorContext<String> context) {
    super(context);
    System.out.println("Worker Actor started");
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onMessageEquals("Fail", this::onFail)
        .onSignal(PreRestart.class, signal -> preRestart())
        .onSignal(PostStop.class, signal -> postStop())
        .build();
  }

  private Behavior<String> onFail() {

    System.out.println("Worker fails now");
    throw new RuntimeException("I failed!");

  }

  private Behavior<String> preRestart() {
    System.out.println("Worker will be restarted");
    return this;
  }

  private Behavior<String> postStop() {
    System.out.println("Worker stopped");
    return this;
  }
}
