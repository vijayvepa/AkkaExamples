package com.example.iotexample.actors.devicemanager;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.iotexample.actors.devicegroup.DeviceGroup;
import com.example.iotexample.actors.devicegroup.DeviceGroupCommand;
import com.example.iotexample.actors.devicemanager.commands.DeviceGroupTerminated;
import com.example.iotexample.actors.devicemanager.commands.ReplyDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestTrackDevice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceManager extends AbstractBehavior<DeviceManagerCommand> {

  private final Map<String, ActorRef<DeviceGroupCommand>> groupIdToActor = new HashMap<>();

  public static Behavior<DeviceManagerCommand> create() {
    return Behaviors.setup(DeviceManager::new);
  }

  public DeviceManager(ActorContext<DeviceManagerCommand> context) {
    super(context);
    context.getLog().info("DeviceManager started.");
  }

  @Override
  public Receive<DeviceManagerCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(RequestTrackDevice.class, this::onRequestTrackDevice)
        .onMessage(RequestDeviceList.class, this::onRequestDeviceList)
        .onMessage(DeviceGroupTerminated.class, this::onDeviceGroupTerminated)
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }


  private Behavior<DeviceManagerCommand> onRequestTrackDevice(RequestTrackDevice m) {

    final String groupId = m.groupId();
    final ActorRef<DeviceGroupCommand> group = groupIdToActor.get(groupId);

    if (group != null) {
      group.tell(m);
      return this;
    }

    getContext().getLog().info("Creating device group for {}", groupId);
    final ActorRef<DeviceGroupCommand> newGroup = getContext().spawn(DeviceGroup.create(groupId), "group-" + groupId);
    getContext().watchWith(newGroup, new DeviceGroupTerminated(groupId));
    newGroup.tell(m);
    groupIdToActor.put(groupId, newGroup);


    return this;
  }

  private Behavior<DeviceManagerCommand> onRequestDeviceList(RequestDeviceList m) {

    final ActorRef<DeviceGroupCommand> group = groupIdToActor.get(m.groupId());
    if (group != null) {
      group.tell(m);
      return this;
    }

    m.replyTo().tell(new ReplyDeviceList(m.requestId(), Collections.emptySet()));

    return this;
  }


  private Behavior<DeviceManagerCommand> onDeviceGroupTerminated(DeviceGroupTerminated m) {

    getContext().getLog().info("DeviceGroup actor for {} has been terminated", m.groupId());
    groupIdToActor.remove(m.groupId());
    return this;
  }

  private DeviceManager onPostStop() {

    getContext().getLog().info("DeviceManager stopped");
    return this;
  }

}
