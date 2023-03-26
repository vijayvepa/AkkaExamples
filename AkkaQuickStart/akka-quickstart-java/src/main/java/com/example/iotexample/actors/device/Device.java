package com.example.iotexample.actors.device;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.iotexample.actors.device.commands.Passivate;
import com.example.iotexample.actors.device.commands.read.ReadTemperature;
import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import com.example.iotexample.actors.device.commands.write.RecordTemperature;
import com.example.iotexample.actors.device.commands.write.TemperatureRecorded;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Device extends AbstractBehavior<DeviceCommand> {

  private final String groupId;
  private final String deviceId;
  private Optional<Double> lastTemperatureReading = Optional.empty();

  public Device(
      ActorContext<DeviceCommand> context,
      String groupId,
      String deviceId) {
    super(context);
    this.groupId = groupId;
    this.deviceId = deviceId;
  }

  public static Behavior<DeviceCommand> create(
      final String groupId,
      final String deviceId) {
    return Behaviors.setup(context -> new Device(context, groupId, deviceId));
  }

  @Override
  public Receive<DeviceCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(ReadTemperature.class, this::onReadTemperature)
        .onMessage(RecordTemperature.class, this::onRecordTemperature)
        .onMessage(Passivate.class, m -> Behaviors.stopped())
        .onSignal(PostStop.class, signal -> this.onPostStop())
        .build();
  }

  private Behavior<DeviceCommand> onReadTemperature(ReadTemperature r) {

    r.replyTo().tell(
        new RespondTemperature(r.requestId(), lastTemperatureReading, deviceId)
    );
    return this;
  }

  private Behavior<DeviceCommand> onRecordTemperature(RecordTemperature r) {
    getContext().getLog().info("Recorded temperature reading {} with {}", r.value(), r.requestId());

    lastTemperatureReading = Optional.of(r.value());

    r.replyTo().tell(
        new TemperatureRecorded(r.requestId())
    );
    return this;
  }

  private Device onPostStop() {
    getContext().getLog().info("Device actor {}-{} stopped", groupId, deviceId);
    return this;
  }
}
