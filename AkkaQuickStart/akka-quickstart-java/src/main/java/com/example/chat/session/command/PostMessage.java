package com.example.chat.session.command;

import com.example.chat.session.SessionCommand;

public record PostMessage(String message) implements SessionCommand {
}
