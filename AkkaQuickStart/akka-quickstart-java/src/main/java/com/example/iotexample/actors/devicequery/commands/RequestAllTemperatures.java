package com.example.iotexample.actors.devicequery.commands;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.devicegroup.DeviceGroupCommand;
import com.example.iotexample.actors.devicequery.DeviceGroupQueryCommand;
import com.example.iotexample.actors.devicequery.model.RespondAllTemperatures;

public record RequestAllTemperatures(
    long requestId,
    String groupId,
    ActorRef<RespondAllTemperatures> replyTo
) implements DeviceGroupQueryCommand, DeviceGroupCommand {
}
