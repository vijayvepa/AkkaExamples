package com.example.iotexample.actors.devicemanager.commands;

import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;

public record DeviceRegistered(ActorRef<DeviceCommand> device) {
}
