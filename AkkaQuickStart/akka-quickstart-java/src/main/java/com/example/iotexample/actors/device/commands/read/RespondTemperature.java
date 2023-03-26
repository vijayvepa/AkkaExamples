package com.example.iotexample.actors.device.commands.read;

import java.util.Optional;

public record RespondTemperature(long requestId, Optional<Double> value, String deviceId) {
}
