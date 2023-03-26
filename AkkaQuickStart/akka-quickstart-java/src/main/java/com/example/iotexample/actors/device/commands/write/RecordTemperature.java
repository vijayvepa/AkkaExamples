package com.example.iotexample.actors.device.commands.write;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;

public record RecordTemperature(
    long requestId,
    double value,
    ActorRef<TemperatureRecorded> replyTo
) implements DeviceCommand {
}
