package com.example.quickstart.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.quickstart.commands.Greet;
import com.example.quickstart.commands.Greeted;

// #greeter
public class Greeter extends AbstractBehavior<Greet> {

  public static Behavior<Greet> create() {
    return Behaviors.setup(Greeter::new);
  }

  private Greeter(ActorContext<Greet> context) {
    super(context);
  }

  @Override
  public Receive<Greet> createReceive() {
    return newReceiveBuilder()
        .onMessage(Greet.class, this::onGreet)
        .build();
  }


  private Behavior<Greet> onGreet(Greet greetCommand) {
    //handle the command
    handle(greetCommand);
    //send acknowledgement
    acknowledge(greetCommand);

    return this;
  }

  private void handle(Greet greetCommand) {
    getContext().getLog().info("Hello {}!", greetCommand.whom());
  }

  private void acknowledge(Greet greetCommand) {
    final Greeted acknowledgement = new Greeted(greetCommand.whom(), getContext().getSelf());
    greetCommand.replyTo().tell(acknowledgement);
  }
}
// #greeter

