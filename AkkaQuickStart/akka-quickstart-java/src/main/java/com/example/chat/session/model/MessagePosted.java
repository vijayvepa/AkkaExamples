package com.example.chat.session.model;

import com.example.chat.gabbler.GabblerCommand;

public record MessagePosted(String screenName, String message) implements SessionEvent, GabblerCommand {
}
