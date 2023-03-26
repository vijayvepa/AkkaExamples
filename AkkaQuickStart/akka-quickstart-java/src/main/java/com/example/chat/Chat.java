package com.example.chat;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import com.example.chat.gabbler.Gabbler;
import com.example.chat.gabbler.GabblerCommand;
import com.example.chat.room.Room;
import com.example.chat.room.RoomCommand;
import com.example.chat.room.command.GetSession;

public class Chat {


  public static Behavior<Void> create(){
    return Behaviors.setup(context -> {
      ActorRef<RoomCommand> room = context.spawn(Room.create(), "chatRoom");
      ActorRef<GabblerCommand> gabbler = context.spawn(Gabbler.create(), "gabbler");
      context.watch(gabbler);
      room.tell(new GetSession("ol' Gabbler", gabbler.unsafeUpcast()));

      return Behaviors.receive(Void.class)
          .onSignal(Terminated.class, signal->Behaviors.stopped())
          .build();
    });


  }

  public static void main(String[] args) {
    ActorSystem.create(Chat.create(), "ChatRoomDemo");
  }
}
