package com.example.chat.session;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.chat.room.command.PublishSessionMessage;
import com.example.chat.session.command.NotifyClient;
import com.example.chat.session.command.PostMessage;

public class Session extends AbstractBehavior<SessionCommand> {

  private final SessionInput input;

  public Session(ActorContext<SessionCommand> context,
                 SessionInput input) {
    super(context);
    this.input = input;
  }

  public static Behavior<SessionCommand> create(SessionInput sessionInput) {
    return Behaviors.setup(context -> new Session(context, sessionInput));
  }

  @Override
  public Receive<SessionCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(PostMessage.class, this::onPostMessage)
        .onMessage(NotifyClient.class, this::onNotifyClient)
        .build();
  }

  private Behavior<SessionCommand> onPostMessage(PostMessage m) {

    input.room().tell(new PublishSessionMessage(input.screenName(), m.message()));

    return Behaviors.same();
  }

  private Behavior<SessionCommand> onNotifyClient(NotifyClient m) {

    input.client().tell(m.messagePosted());
    return this;
  }
}
