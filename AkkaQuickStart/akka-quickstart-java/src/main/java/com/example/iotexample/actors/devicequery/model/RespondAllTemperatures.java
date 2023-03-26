package com.example.iotexample.actors.devicequery.model;

import java.util.Map;

public record RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
}
