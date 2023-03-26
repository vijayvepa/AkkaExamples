package com.example.iotexample.actors.devicegroup;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.iotexample.actors.device.Device;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.devicegroup.command.DeviceTerminated;
import com.example.iotexample.actors.devicemanager.commands.DeviceRegistered;
import com.example.iotexample.actors.devicemanager.commands.ReplyDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestTrackDevice;
import com.example.iotexample.actors.devicequery.DeviceGroupQuery;
import com.example.iotexample.actors.devicequery.DeviceGroupQueryInput;
import com.example.iotexample.actors.devicequery.commands.RequestAllTemperatures;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DeviceGroup extends AbstractBehavior<DeviceGroupCommand> {

  private final String groupId;
  private final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = new HashMap<>();

  public DeviceGroup(
      ActorContext<DeviceGroupCommand> context,
      String groupId) {
    super(context);
    this.groupId = groupId;
  }

  public static Behavior<DeviceGroupCommand> create(final String groupId) {
    return Behaviors.setup(context -> new DeviceGroup(context, groupId));
  }

  @Override
  public Receive<DeviceGroupCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(RequestTrackDevice.class, this::onRequestTrackDevice)
        .onMessage(DeviceTerminated.class, this::onDeviceTerminated)
        .onMessage(RequestDeviceList.class, this::onRequestDeviceList)
        .onMessage(RequestAllTemperatures.class, r -> r.groupId().equals(groupId), this::onRequestAllTemperatures)
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private Behavior<DeviceGroupCommand> onRequestTrackDevice(RequestTrackDevice m) {

    if (!this.groupId.equals(m.groupId())) {
      getContext().getLog().info("Ignoring track device request for group {}, this actor is responsible for group {} ", m.groupId(), this.groupId);
      return this;
    }

    final ActorRef<DeviceCommand> deviceActor = deviceIdToActor.get(m.deviceId());

    if (deviceActor != null) {
      m.replyTo().tell(new DeviceRegistered(deviceActor));
      return this;
    }

    getContext().getLog().info("Creating device for actor {}", m.deviceId());
    final ActorRef<DeviceCommand> createdDeviceActor = getContext().spawn(Device.create(this.groupId, m.deviceId()), "device-" + m.deviceId());
    getContext().watchWith(createdDeviceActor, new DeviceTerminated(createdDeviceActor, groupId, m.deviceId()));
    deviceIdToActor.put(m.deviceId(), createdDeviceActor);

    m.replyTo().tell(new DeviceRegistered(createdDeviceActor));
    return this;
  }

  private Behavior<DeviceGroupCommand> onDeviceTerminated(DeviceTerminated m) {

    getContext().getLog().info("Device actor for {} has been terminated.", m.deviceId());
    deviceIdToActor.remove(m.deviceId());

    return this;
  }

  private Behavior<DeviceGroupCommand> onRequestDeviceList(RequestDeviceList m) {

    m.replyTo().tell(new ReplyDeviceList(m.requestId(), deviceIdToActor.keySet()));
    return this;
  }

  private DeviceGroup onPostStop() {
    getContext().getLog().info("DeviceGroup {} stopped", groupId);
    return this;
  }

  private Behavior<DeviceGroupCommand> onRequestAllTemperatures(RequestAllTemperatures m) {

    final HashMap<String, ActorRef<DeviceCommand>> deviceIdToActorCopy = new HashMap<>(this.deviceIdToActor);

    getContext().spawnAnonymous(DeviceGroupQuery.create(
        new DeviceGroupQueryInput(deviceIdToActorCopy, m.requestId(), m.replyTo(), Duration.ofSeconds(3))
    ));

    return this;
  }


}
