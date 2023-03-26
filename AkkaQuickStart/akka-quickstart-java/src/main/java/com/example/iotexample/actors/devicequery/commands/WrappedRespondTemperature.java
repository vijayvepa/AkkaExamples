package com.example.iotexample.actors.devicequery.commands;

import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import com.example.iotexample.actors.devicequery.DeviceGroupQueryCommand;

public record WrappedRespondTemperature(RespondTemperature response)
    implements DeviceGroupQueryCommand {
}
