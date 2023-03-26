package com.example.iotexample.actors.devicegroup.command;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.devicegroup.DeviceGroupCommand;

public record DeviceTerminated(
    ActorRef<DeviceCommand> device,
    String groupId,
    String deviceId
) implements DeviceGroupCommand {
}
