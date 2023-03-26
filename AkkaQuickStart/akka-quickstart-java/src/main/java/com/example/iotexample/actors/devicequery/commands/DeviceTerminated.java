package com.example.iotexample.actors.devicequery.commands;

import com.example.iotexample.actors.devicequery.DeviceGroupQueryCommand;

public record DeviceTerminated(String deviceId) implements DeviceGroupQueryCommand {
}
