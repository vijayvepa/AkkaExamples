package com.example.iotexample.actors.devicemanager.commands;

import com.example.iotexample.actors.devicemanager.DeviceManagerCommand;

public record DeviceGroupTerminated(String groupId) implements DeviceManagerCommand {
}
