package com.example.chat.room.model;

import akka.actor.typed.ActorRef;
import com.example.chat.gabbler.GabblerCommand;
import com.example.chat.session.command.PostMessage;
import com.example.chat.session.model.SessionEvent;

public record SessionGranted(ActorRef<PostMessage> handle) implements SessionEvent, GabblerCommand {
}
