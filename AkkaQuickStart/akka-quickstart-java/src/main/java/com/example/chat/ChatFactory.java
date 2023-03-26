package com.example.chat;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import com.example.chat.gabbler.GabblerFactory;
import com.example.chat.room.RoomCommand;
import com.example.chat.room.RoomFactory;
import com.example.chat.room.command.GetSession;
import com.example.chat.session.model.SessionEvent;

public class ChatFactory {

  public static Behavior<Void> create(){
    return Behaviors.setup(context -> {
      ActorRef<RoomCommand> room = context.spawn(RoomFactory.create(), "chatRoom");
      ActorRef<SessionEvent> gabbler = context.spawn(GabblerFactory.create(), "gabbler");
      context.watch(gabbler);
      room.tell(new GetSession("ol' Gabbler", gabbler));

      return Behaviors.receive(Void.class)
          .onSignal(Terminated.class, signal->Behaviors.stopped())
          .build();
    });


  }

  public static void main(String[] args) {
    ActorSystem.create(ChatFactory.create(), "ChatRoomDemo");
  }
}
