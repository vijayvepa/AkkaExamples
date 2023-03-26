package com.example.chat.gabbler;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.chat.room.model.SessionDenied;
import com.example.chat.room.model.SessionGranted;
import com.example.chat.session.command.PostMessage;
import com.example.chat.session.model.MessagePosted;

public class Gabbler extends AbstractBehavior<GabblerCommand> {

  public Gabbler(ActorContext<GabblerCommand> context) {
    super(context);
  }

  public static Behavior<GabblerCommand> create() {
    return Behaviors.setup(Gabbler::new);
  }

  @Override
  public Receive<GabblerCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(SessionDenied.class, this::onSessionDenied)
        .onMessage(SessionGranted.class, this::onSessionGranted)
        .onMessage(MessagePosted.class, this::onMessagePosted)
        .build();
  }

  private Behavior<GabblerCommand> onSessionDenied(SessionDenied m) {

    getContext().getLog().info("Cannot start chat room session: {}", m.reason());

    return Behaviors.stopped();
  }

  private Behavior<GabblerCommand> onSessionGranted(SessionGranted m) {

    m.handle().tell(new PostMessage("Hello, World!"));
    return this;
  }

  private Behavior<GabblerCommand> onMessagePosted(MessagePosted m) {

    getContext().getLog().info("message has been posted by '{}': {}", m.screenName(), m.message());
    return this;
  }
}
