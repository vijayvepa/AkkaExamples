package com.example.chat.gabbler;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import com.example.chat.room.model.SessionDenied;
import com.example.chat.room.model.SessionGranted;
import com.example.chat.session.command.PostMessage;
import com.example.chat.session.model.MessagePosted;
import com.example.chat.session.model.SessionEvent;

public class GabblerFactory {
  private final ActorContext<SessionEvent> context;

  public GabblerFactory(ActorContext<SessionEvent> context) {
    this.context = context;
  }

  public static Behavior<SessionEvent> create(){
    return Behaviors.setup(ctx -> new GabblerFactory(ctx).behavior());
  }

  private Behavior<SessionEvent> behavior() {
    return Behaviors.receive(SessionEvent.class)
        .onMessage(SessionDenied.class, this::onSessionDenied)
        .onMessage(SessionGranted.class, this::onSessionGranted)
        .onMessage(MessagePosted.class, this::onMessagePosted)
        .build();
  }

  private Behavior<SessionEvent> onMessagePosted(MessagePosted m) {
    context.getLog().info("message has been posted by '{}': {}", m.screenName(), m.message());
    return Behaviors.stopped();
  }

  private Behavior<SessionEvent> onSessionGranted(SessionGranted m) {
    m.handle().tell(new PostMessage("Hello, world!"));
    return Behaviors.same();
  }

  private Behavior<SessionEvent> onSessionDenied(SessionDenied m) {
    context.getLog().info("cannot start chat room session: {}", m.reason());
    return Behaviors.stopped();
  }


}
