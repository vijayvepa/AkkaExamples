package com.example.iotexample.actors.devicequery;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.devicequery.model.RespondAllTemperatures;

import java.time.Duration;
import java.util.Map;

public record DeviceGroupQueryInput(
    Map<String, ActorRef<DeviceCommand>> deviceIdToActor,
    long requestId,
    ActorRef<RespondAllTemperatures> requester,
    Duration timeout

) {
}
