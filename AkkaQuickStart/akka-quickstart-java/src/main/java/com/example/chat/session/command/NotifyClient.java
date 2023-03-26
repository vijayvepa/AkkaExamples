package com.example.chat.session.command;

import com.example.chat.session.SessionCommand;
import com.example.chat.session.model.MessagePosted;

public record NotifyClient(MessagePosted messagePosted) implements SessionCommand {
}
