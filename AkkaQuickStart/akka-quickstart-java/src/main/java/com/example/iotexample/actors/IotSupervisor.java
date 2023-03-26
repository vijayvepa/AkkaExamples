package com.example.iotexample.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class IotSupervisor extends AbstractBehavior<String> {

  public static Behavior<String> create() {
    return Behaviors.setup(IotSupervisor::new);

  }

  public IotSupervisor(ActorContext<String> context) {
    super(context);
    context.getLog().info("IoT application started");
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder()
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private Behavior<String> onPostStop() {

    getContext().getLog().info("IoT Application Stopped");
    return this;

  }
}
