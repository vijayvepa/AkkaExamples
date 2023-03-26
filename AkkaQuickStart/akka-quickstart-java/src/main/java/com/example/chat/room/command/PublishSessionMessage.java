package com.example.chat.room.command;

import com.example.chat.room.RoomCommand;

public record PublishSessionMessage(String screenName, String message) implements RoomCommand {
}
