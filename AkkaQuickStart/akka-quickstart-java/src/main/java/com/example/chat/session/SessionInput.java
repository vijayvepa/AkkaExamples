package com.example.chat.session;

import akka.actor.typed.ActorRef;
import com.example.chat.room.RoomCommand;
import com.example.chat.session.model.SessionEvent;

public record SessionInput(
    ActorRef<RoomCommand> room,
    String screenName,
    ActorRef<SessionEvent> client) {
}
