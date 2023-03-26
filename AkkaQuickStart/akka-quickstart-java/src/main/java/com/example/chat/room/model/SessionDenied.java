package com.example.chat.room.model;

import com.example.chat.gabbler.GabblerCommand;
import com.example.chat.session.model.SessionEvent;

public record SessionDenied(String reason) implements SessionEvent, GabblerCommand {
}
