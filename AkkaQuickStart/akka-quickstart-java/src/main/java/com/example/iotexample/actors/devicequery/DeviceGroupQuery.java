package com.example.iotexample.actors.devicequery;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.device.commands.read.ReadTemperature;
import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import com.example.iotexample.actors.devicequery.commands.CollectionTimeout;
import com.example.iotexample.actors.devicequery.commands.DeviceTerminated;
import com.example.iotexample.actors.devicequery.commands.WrappedRespondTemperature;
import com.example.iotexample.actors.devicequery.model.DeviceNotAvailable;
import com.example.iotexample.actors.devicequery.model.DeviceTimedOut;
import com.example.iotexample.actors.devicequery.model.RespondAllTemperatures;
import com.example.iotexample.actors.devicequery.model.Temperature;
import com.example.iotexample.actors.devicequery.model.TemperatureNotAvailable;
import com.example.iotexample.actors.devicequery.model.TemperatureReading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceGroupQuery extends AbstractBehavior<DeviceGroupQueryCommand> {

  private final Map<String, TemperatureReading> repliesSoFar = new HashMap<>();
  private final Set<String> stillWaiting;
  DeviceGroupQueryInput input;

  public DeviceGroupQuery(
      DeviceGroupQueryInput input,
      TimerScheduler<DeviceGroupQueryCommand> timers,
      ActorContext<DeviceGroupQueryCommand> context) {
    super(context);
    this.input = input;

    timers.startSingleTimer(CollectionTimeout.INSTANCE, input.timeout());
    startQuery(context);

    this.stillWaiting = new HashSet<>(input.deviceIdToActor().keySet());
  }

  private void startQuery(ActorContext<DeviceGroupQueryCommand> context) {

    ActorRef<RespondTemperature> respondTemperatureAdapter =
        context.messageAdapter(RespondTemperature.class, WrappedRespondTemperature::new);

    for (Map.Entry<String, ActorRef<DeviceCommand>> entry : input.deviceIdToActor().entrySet()) {
      context.watchWith(entry.getValue(), new DeviceTerminated(entry.getKey()));
      entry.getValue().tell(new ReadTemperature(0L, respondTemperatureAdapter));
    }


  }

  public static Behavior<DeviceGroupQueryCommand> create(DeviceGroupQueryInput input) {
    return Behaviors.setup(context ->
        Behaviors.withTimers(timers ->
            new DeviceGroupQuery(input, timers, context)));
  }

  @Override
  public Receive<DeviceGroupQueryCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(WrappedRespondTemperature.class, this::onRespondTemperature)
        .onMessage(DeviceTerminated.class, this::onDeviceTerminated)
        .onMessage(CollectionTimeout.class, this::onCollectionTimeout)
        .build();
  }

  private Behavior<DeviceGroupQueryCommand> onRespondTemperature(WrappedRespondTemperature m) {
    final TemperatureReading temperatureReading = m.response().value()
        .map(v -> (TemperatureReading) new Temperature(v))
        .orElse(TemperatureNotAvailable.INSTANCE);

    final String deviceId = m.response().deviceId();

    repliesSoFar.put(deviceId, temperatureReading);
    stillWaiting.remove(deviceId);

    return respondWhenAllCollected();
  }

  private Behavior<DeviceGroupQueryCommand> onDeviceTerminated(DeviceTerminated m) {

    if (!stillWaiting.contains(m.deviceId())) {
      return respondWhenAllCollected();
    }

    repliesSoFar.put(m.deviceId(), DeviceNotAvailable.INSTANCE);
    stillWaiting.remove(m.deviceId());

    return respondWhenAllCollected();
  }

  private Behavior<DeviceGroupQueryCommand> onCollectionTimeout(CollectionTimeout m) {

    for (String deviceId : stillWaiting) {
      repliesSoFar.put(deviceId, DeviceTimedOut.INSTANCE);
    }
    stillWaiting.clear();

    return respondWhenAllCollected();
  }

  private Behavior<DeviceGroupQueryCommand> respondWhenAllCollected() {
    if (!stillWaiting.isEmpty()) {
      return this;
    }

    input.requester().tell(new RespondAllTemperatures(input.requestId(), repliesSoFar));
    return Behaviors.stopped();
  }
}
