package com.example.chat.room;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.chat.room.command.GetSession;
import com.example.chat.room.command.PublishSessionMessage;
import com.example.chat.room.model.SessionGranted;
import com.example.chat.session.Session;
import com.example.chat.session.SessionCommand;
import com.example.chat.session.SessionInput;
import com.example.chat.session.command.NotifyClient;
import com.example.chat.session.model.MessagePosted;
import com.example.chat.session.model.SessionEvent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Room extends AbstractBehavior<RoomCommand> {

  List<ActorRef<SessionCommand>> sessions = new ArrayList<>();

  public Room(ActorContext<RoomCommand> context) {
    super(context);
  }

  public static Behavior<RoomCommand> create() {
    return Behaviors.setup(Room::new);
  }

  @Override
  public Receive<RoomCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(GetSession.class, this::onGetSession)
        .onMessage(PublishSessionMessage.class, this::onPublishSessionMessage )
        .build();
  }


  private Behavior<RoomCommand> onGetSession(GetSession m) {

    final ActorRef<SessionEvent> client = m.replyTo();

    final ActorRef<SessionCommand> session = getContext().spawn(
        Session.create(new SessionInput(getContext().getSelf(), m.screenName(), m.replyTo())),
        URLEncoder.encode(m.screenName(), StandardCharsets.UTF_8));

    client.tell(new SessionGranted(session.narrow()));

    sessions.add(session);

    return this;
  }

  private Behavior<RoomCommand> onPublishSessionMessage(PublishSessionMessage m) {

    NotifyClient notification = new NotifyClient(new MessagePosted(m.screenName(), m.message()));
    sessions.forEach(s->s.tell(notification));
    return this;
  }
}
