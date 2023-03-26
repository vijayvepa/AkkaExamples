package com.example.iotexample.actors.device.commands.read;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;

public record ReadTemperature(
    long requestId,
    ActorRef<RespondTemperature> replyTo
)
    implements DeviceCommand {
}
