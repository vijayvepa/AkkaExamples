package com.example.chat.room;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import com.example.chat.room.command.GetSession;
import com.example.chat.room.command.PublishSessionMessage;
import com.example.chat.room.model.SessionGranted;
import com.example.chat.session.SessionCommand;
import com.example.chat.session.SessionFactory;
import com.example.chat.session.command.NotifyClient;
import com.example.chat.session.model.MessagePosted;
import com.example.chat.session.model.SessionEvent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RoomFactory {

  ActorContext<RoomCommand> context;


  public RoomFactory(ActorContext<RoomCommand> context) {
     this.context = context;
  }

  public static Behavior<RoomCommand> create() {
    return Behaviors.setup(context->new RoomFactory(context).setupCommands(new ArrayList<>()));
  }

  private Behavior<RoomCommand> setupCommands(List<ActorRef<SessionCommand>> sessions) {
    return Behaviors.receive(RoomCommand.class)
        .onMessage(GetSession.class, m->onGetSession(m, sessions))
        .onMessage(PublishSessionMessage.class, m->onPublishSessionMessage(m, sessions))
        .build();
  }


  private Behavior<RoomCommand> onGetSession(
      GetSession m,
      List<ActorRef<SessionCommand>> sessions) {

    final ActorRef<SessionEvent> client = m.replyTo();

    final ActorRef<SessionCommand> session = context.spawn(
        SessionFactory.create(context.getSelf(), m.screenName(), client),
        URLEncoder.encode(m.screenName(), StandardCharsets.UTF_8));

    client.tell(new SessionGranted(session.narrow()));
    final ArrayList<ActorRef<SessionCommand>> sessionsCopy = new ArrayList<>(sessions);
    sessionsCopy.add(session);

    return setupCommands(sessionsCopy);
  }

  private Behavior<RoomCommand> onPublishSessionMessage(
      PublishSessionMessage m,
      List<ActorRef<SessionCommand>> sessions) {

    NotifyClient notification =
        new NotifyClient(new MessagePosted(m.screenName(), m.message()));

    sessions.forEach(s->s.tell(notification));
    return Behaviors.same();
  }
}
