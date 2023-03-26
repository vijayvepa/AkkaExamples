package com.example.chat.session;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.example.chat.room.RoomCommand;
import com.example.chat.room.command.PublishSessionMessage;
import com.example.chat.session.command.NotifyClient;
import com.example.chat.session.command.PostMessage;
import com.example.chat.session.model.SessionEvent;

public class SessionFactory {

  public static Behavior<SessionCommand> create(
      ActorRef<RoomCommand> room,
      String screenName,
      ActorRef<SessionEvent> client
  ) {
    return Behaviors.receive(SessionCommand.class)
        .onMessage(NotifyClient.class, notification -> onNotifyClient(notification, client))
        .onMessage(PostMessage.class, message -> onPostMessage(message, room, screenName))
        .build();
  }

  private static Behavior<SessionCommand> onPostMessage(
      PostMessage post,
      ActorRef<RoomCommand> room,
      String screenName
  ) {
    room.tell(new PublishSessionMessage(screenName, post.message()));
    return Behaviors.same();
  }


  private static Behavior<SessionCommand> onNotifyClient(
      NotifyClient m,
      ActorRef<SessionEvent> client) {

    client.tell(m.messagePosted());
    return Behaviors.same();
  }
}
