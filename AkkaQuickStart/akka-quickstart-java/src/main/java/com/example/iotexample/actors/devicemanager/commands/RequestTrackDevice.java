package com.example.iotexample.actors.devicemanager.commands;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.devicegroup.DeviceGroupCommand;
import com.example.iotexample.actors.devicemanager.DeviceManagerCommand;

public record RequestTrackDevice(
    String groupId,
    String deviceId,
    ActorRef<DeviceRegistered> replyTo
)
    implements DeviceManagerCommand, DeviceGroupCommand {
}
