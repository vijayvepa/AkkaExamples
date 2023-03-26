package com.example.chat.room.command;

import akka.actor.typed.ActorRef;
import com.example.chat.room.RoomCommand;
import com.example.chat.session.model.SessionEvent;

public record GetSession (
    String screenName,
    ActorRef<SessionEvent> replyTo
) implements RoomCommand {
}
