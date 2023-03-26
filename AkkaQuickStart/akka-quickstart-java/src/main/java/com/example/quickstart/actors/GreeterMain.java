package com.example.quickstart.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.quickstart.commands.Greet;
import com.example.quickstart.commands.Greeted;
import com.example.quickstart.commands.SayHello;

public class GreeterMain extends AbstractBehavior<SayHello> {

  private final ActorRef<Greet> greeter;

  public static Behavior<SayHello> create() {
    return Behaviors.setup(GreeterMain::new);
  }

  private GreeterMain(ActorContext<SayHello> context) {
    super(context);
    //region #create--child-actors
    greeter = context.spawn(Greeter.create(), "greeter");
    //endregion #create-child-actors
  }

  @Override
  public Receive<SayHello> createReceive() {
    return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
  }

  private Behavior<SayHello> onSayHello(SayHello command) {
    //#create-child-actors
    ActorRef<Greeted> replyTo =
        getContext().spawn(GreeterBot.create(3), "Greeter-Bot-For-" + command.whom());
    greeter.tell(new Greet(command.whom(), replyTo));
    //#create-child-actors
    return this;
  }
}
