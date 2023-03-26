package com.example.iotexample.actors.devicemanager.commands;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.devicegroup.DeviceGroupCommand;
import com.example.iotexample.actors.devicemanager.DeviceManagerCommand;

public record RequestDeviceList(
    long requestId,
    String groupId,
    ActorRef<ReplyDeviceList> replyTo
)
    implements DeviceGroupCommand, DeviceManagerCommand {
}
