package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.device.commands.read.RespondTemperature;
import com.example.iotexample.actors.devicequery.DeviceGroupQuery;
import com.example.iotexample.actors.devicequery.DeviceGroupQueryCommand;
import com.example.iotexample.actors.devicequery.DeviceGroupQueryInput;
import com.example.iotexample.actors.devicequery.commands.WrappedRespondTemperature;
import com.example.iotexample.actors.devicequery.model.DeviceNotAvailable;
import com.example.iotexample.actors.devicequery.model.DeviceTimedOut;
import com.example.iotexample.actors.devicequery.model.RespondAllTemperatures;
import com.example.iotexample.actors.devicequery.model.Temperature;
import com.example.iotexample.actors.devicequery.model.TemperatureNotAvailable;
import com.example.iotexample.actors.devicequery.model.TemperatureReading;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceGroupQueryTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();


  @Test
  public void testRespondAllTemperatures() {
    final TestProbe<RespondAllTemperatures> requester = testKit.createTestProbe(RespondAllTemperatures.class);
    final TestProbe<DeviceCommand> device1 = testKit.createTestProbe(DeviceCommand.class);
    final TestProbe<DeviceCommand> device2 = testKit.createTestProbe(DeviceCommand.class);
    final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = Map.of(
        "device1", device1.getRef(),
        "device2", device2.getRef());

    ActorRef<DeviceGroupQueryCommand> queryActor = testKit.spawn(
        DeviceGroupQuery.create(
            new DeviceGroupQueryInput(
                deviceIdToActor,
                1L,
                requester.getRef(),
                Duration.ofSeconds(3)
            )));

    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(1.0), "device1")));
    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(2.0), "device2")));

    final RespondAllTemperatures response = requester.receiveMessage();

    assertNotNull(response);

    assertEquals(1L, response.requestId());
    final Map<String, Temperature> expectedTemperatures =
        Map.of("device1", new Temperature(1.0), "device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures());
  }

  @Test
  public void testRespondNotAvailableForDevicesWithNoReadings() {
    final TestProbe<RespondAllTemperatures> requester = testKit.createTestProbe(RespondAllTemperatures.class);
    final TestProbe<DeviceCommand> device1 = testKit.createTestProbe(DeviceCommand.class);
    final TestProbe<DeviceCommand> device2 = testKit.createTestProbe(DeviceCommand.class);
    final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = Map.of(
        "device1", device1.getRef(),
        "device2", device2.getRef());

    ActorRef<DeviceGroupQueryCommand> queryActor = testKit.spawn(
        DeviceGroupQuery.create(
            new DeviceGroupQueryInput(
                deviceIdToActor,
                1L,
                requester.getRef(),
                Duration.ofSeconds(3)
            )));

    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.empty(), "device1")));
    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(2.0), "device2")));

    final RespondAllTemperatures response = requester.receiveMessage();

    assertNotNull(response);

    assertEquals(1L, response.requestId());
    final Map<String, TemperatureReading> expectedTemperatures =
        Map.of("device1", TemperatureNotAvailable.INSTANCE, "device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures());
  }

  @Test
  public void testRespondDeviceNotAvailableForStoppedDevices() {
    final TestProbe<RespondAllTemperatures> requester = testKit.createTestProbe(RespondAllTemperatures.class);
    final TestProbe<DeviceCommand> device1 = testKit.createTestProbe(DeviceCommand.class);
    final TestProbe<DeviceCommand> device2 = testKit.createTestProbe(DeviceCommand.class);
    final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = Map.of(
        "device1", device1.getRef(),
        "device2", device2.getRef());

    ActorRef<DeviceGroupQueryCommand> queryActor = testKit.spawn(
        DeviceGroupQuery.create(
            new DeviceGroupQueryInput(
                deviceIdToActor,
                1L,
                requester.getRef(),
                Duration.ofSeconds(3)
            )));

    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(2.0), "device2")));
    device1.stop();

    final RespondAllTemperatures response = requester.receiveMessage();

    assertNotNull(response);

    assertEquals(1L, response.requestId());
    final Map<String, TemperatureReading> expectedTemperatures =
        Map.of("device1", DeviceNotAvailable.INSTANCE, "device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures());
  }

  @Test
  public void testRespondAllTemperaturesForRespondedButStoppedDevices() {
    final TestProbe<RespondAllTemperatures> requester = testKit.createTestProbe(RespondAllTemperatures.class);
    final TestProbe<DeviceCommand> device1 = testKit.createTestProbe(DeviceCommand.class);
    final TestProbe<DeviceCommand> device2 = testKit.createTestProbe(DeviceCommand.class);
    final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = Map.of(
        "device1", device1.getRef(),
        "device2", device2.getRef());

    ActorRef<DeviceGroupQueryCommand> queryActor = testKit.spawn(
        DeviceGroupQuery.create(
            new DeviceGroupQueryInput(
                deviceIdToActor,
                1L,
                requester.getRef(),
                Duration.ofSeconds(3)
            )));

    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(1.0), "device1")));
    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(2.0), "device2")));
    device1.stop();

    final RespondAllTemperatures response = requester.receiveMessage();

    assertNotNull(response);

    assertEquals(1L, response.requestId());
    final Map<String, TemperatureReading> expectedTemperatures =
        Map.of("device1", new Temperature(1.0), "device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures());
  }

  @Test
  public void testRespondTimedOutForNoResponseInTime() {
    final TestProbe<RespondAllTemperatures> requester = testKit.createTestProbe(RespondAllTemperatures.class);
    final TestProbe<DeviceCommand> device1 = testKit.createTestProbe(DeviceCommand.class);
    final TestProbe<DeviceCommand> device2 = testKit.createTestProbe(DeviceCommand.class);
    final Map<String, ActorRef<DeviceCommand>> deviceIdToActor = Map.of(
        "device1", device1.getRef(),
        "device2", device2.getRef());

    ActorRef<DeviceGroupQueryCommand> queryActor = testKit.spawn(
        DeviceGroupQuery.create(
            new DeviceGroupQueryInput(
                deviceIdToActor,
                1L,
                requester.getRef(),
                Duration.ofMillis(200)
            )));

    queryActor.tell(new WrappedRespondTemperature(
        new RespondTemperature(0L, Optional.of(2.0), "device2")));

    final RespondAllTemperatures response = requester.receiveMessage();

    assertNotNull(response);

    assertEquals(1L, response.requestId());
    final Map<String, TemperatureReading> expectedTemperatures =
        Map.of("device1", DeviceTimedOut.INSTANCE, "device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures());
  }
}


 