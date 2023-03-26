package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.Device;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.device.commands.read.ReadTemperature;
import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import com.example.iotexample.actors.device.commands.write.RecordTemperature;
import com.example.iotexample.actors.device.commands.write.TemperatureRecorded;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceActorTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();


  @Test
  public void testRespondTemperature() {
    final TestProbe<RespondTemperature> replyProbe = testKit.createTestProbe(RespondTemperature.class);
    ActorRef<DeviceCommand> actor = testKit.spawn(Device.create("group", "device"));

    actor.tell(new ReadTemperature(42L, replyProbe.getRef()));

    final RespondTemperature replyMessage = replyProbe.receiveMessage();

    assertNotNull(replyMessage);
    assertEquals(42L, replyMessage.requestId());
    assertEquals(Optional.empty(), replyMessage.value());
  }


  @Test
  public void testRespondTemperatureForWrites() {

    ActorRef<DeviceCommand> actor = testKit.spawn(Device.create("group", "device"));

    final TestProbe<RespondTemperature> readProbe = testKit.createTestProbe(RespondTemperature.class);
    final TestProbe<TemperatureRecorded> writeProbe = testKit.createTestProbe(TemperatureRecorded.class);

    actor.tell(new RecordTemperature(1L, 24.0, writeProbe.getRef()));
    final TemperatureRecorded temperatureRecorded = writeProbe.receiveMessage();
    assertEquals(1L, temperatureRecorded.requestId());

    actor.tell(new ReadTemperature(2L, readProbe.getRef()));

    final RespondTemperature replyMessage1 = readProbe.receiveMessage();

    assertNotNull(replyMessage1);
    assertEquals(2L, replyMessage1.requestId());
    assertEquals(Optional.of(24.0), replyMessage1.value());

    actor.tell(new RecordTemperature(3L, 55.0, writeProbe.getRef()));
    final TemperatureRecorded temperatureRecorded2 = writeProbe.receiveMessage();
    assertEquals(3L, temperatureRecorded2.requestId());

    actor.tell(new ReadTemperature(4L, readProbe.getRef()));

    final RespondTemperature replyMessage2 = readProbe.receiveMessage();

    assertNotNull(replyMessage2);
    assertEquals(4L, replyMessage2.requestId());
    assertEquals(Optional.of(55.0), replyMessage2.value());

  }
}


 