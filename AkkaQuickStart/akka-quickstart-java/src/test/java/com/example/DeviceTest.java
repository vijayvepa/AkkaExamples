package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.Device;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.device.commands.read.ReadTemperature;
import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DeviceTest {
  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
    final TestProbe<RespondTemperature> testProbe = testKit.createTestProbe(RespondTemperature.class);
    ActorRef<DeviceCommand> deviceActor = testKit.spawn(Device.create("groupId", "deviceId"));

    deviceActor.tell(new ReadTemperature(42L, testProbe.getRef()));

    final RespondTemperature respondTemperature = testProbe.receiveMessage();

    assertEquals(42L, respondTemperature.requestId());
    assertEquals(Optional.empty(), respondTemperature.value());
  }
}
