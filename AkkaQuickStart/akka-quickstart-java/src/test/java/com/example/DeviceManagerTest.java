package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.example.iotexample.actors.device.DeviceCommand;
import com.example.iotexample.actors.device.commands.Passivate;
import com.example.iotexample.actors.device.commands.write.RecordTemperature;
import com.example.iotexample.actors.device.commands.write.TemperatureRecorded;
import com.example.iotexample.actors.devicemanager.DeviceManagerCommand;
import com.example.iotexample.actors.devicemanager.commands.DeviceRegistered;
import com.example.iotexample.actors.devicemanager.commands.ReplyDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestDeviceList;
import com.example.iotexample.actors.devicemanager.commands.RequestTrackDevice;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Set;

import static com.example.iotexample.actors.devicemanager.DeviceManager.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceManagerTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();


  @Test
  public void testDeviceRegistered() {
    final TestProbe<DeviceRegistered> replyProbe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceManagerCommand> groupActor = testKit.spawn(create());


    groupActor.tell(new RequestTrackDevice("group", "device3", replyProbe.getRef()));
    final DeviceRegistered replyMessage = replyProbe.receiveMessage();

    groupActor.tell(new RequestTrackDevice("group", "device4", replyProbe.getRef()));
    final DeviceRegistered replyMessage2 = replyProbe.receiveMessage();

    assertNotNull(replyMessage);
    assertNotEquals(replyMessage2.device(), replyMessage.device());

    assertNotNull(replyMessage);

    {
      final TestProbe<TemperatureRecorded> temperatureProbe1 = testKit.createTestProbe(TemperatureRecorded.class);
      replyMessage.device().tell(new RecordTemperature(0L, 1.0, temperatureProbe1.getRef()));
      final TemperatureRecorded temperatureMessage = temperatureProbe1.receiveMessage();

      assertNotNull(temperatureMessage);
      assertEquals(0L, temperatureMessage.requestId());
    }

    {
      final TestProbe<TemperatureRecorded> temperatureProbe1 = testKit.createTestProbe(TemperatureRecorded.class);
      replyMessage2.device().tell(new RecordTemperature(0L, 2.0, temperatureProbe1.getRef()));
      final TemperatureRecorded temperatureMessage = temperatureProbe1.receiveMessage();

      assertNotNull(temperatureMessage);
      assertEquals(0L, temperatureMessage.requestId());
    }

  }


  @Test
  public void testReturnSameActorForSameDevice() {
    final TestProbe<DeviceRegistered> replyProbe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceManagerCommand> groupActor = testKit.spawn(create());

    groupActor.tell(new RequestTrackDevice("group", "device", replyProbe.getRef()));
    final DeviceRegistered replyMessage = replyProbe.receiveMessage();
    groupActor.tell(new RequestTrackDevice("group", "device", replyProbe.getRef()));
    final DeviceRegistered replyMessage2 = replyProbe.receiveMessage();

    assertEquals(replyMessage.device(), replyMessage2.device());
  }

  @Test
  public void testListActiveDevices() {
    //region  setup
    ActorRef<DeviceManagerCommand> groupActor = testKit.spawn(create());
    {
      final TestProbe<DeviceRegistered> replyProbe = testKit.createTestProbe(DeviceRegistered.class);

      {
        groupActor.tell(new RequestTrackDevice("group", "device1", replyProbe.getRef()));
        final DeviceRegistered replyMessage = replyProbe.receiveMessage();

        assertNotNull(replyMessage);
      }
      {
        groupActor.tell(new RequestTrackDevice("group", "device2", replyProbe.getRef()));
        final DeviceRegistered replyMessage = replyProbe.receiveMessage();

        assertNotNull(replyMessage);
      }
    }
    //endregion

    final TestProbe<ReplyDeviceList> replyProbe = testKit.createTestProbe(ReplyDeviceList.class);
    groupActor.tell(new RequestDeviceList(0L, "group", replyProbe.getRef()));
    final ReplyDeviceList replyMessage = replyProbe.receiveMessage();

    assertNotNull(replyMessage);
    assertEquals(0L, replyMessage.requestId());
    assertEquals(Set.of("device1", "device2"), replyMessage.ids());

  }

  @Test
  public void testListActiveDevicesAfterOneShutsDown() {
    //region  setup
    ActorRef<DeviceManagerCommand> groupActor = testKit.spawn(create());
    ActorRef<DeviceCommand> toShutDown;
    final TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
    {


      {
        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        final DeviceRegistered replyMessage = registeredProbe.receiveMessage();

        assertNotNull(replyMessage);
      }
      {
        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        final DeviceRegistered replyMessage = registeredProbe.receiveMessage();
        toShutDown = replyMessage.device();

        assertNotNull(replyMessage);
      }
    }
    //endregion

    final TestProbe<ReplyDeviceList> deviceListTestProbe = testKit.createTestProbe(ReplyDeviceList.class);
    {

      groupActor.tell(new RequestDeviceList(0L, "group", deviceListTestProbe.getRef()));
      final ReplyDeviceList replyMessage = deviceListTestProbe.receiveMessage();

      assertNotNull(replyMessage);
      assertEquals(0L, replyMessage.requestId());
      assertEquals(Set.of("device1", "device2"), replyMessage.ids());
    }

    toShutDown.tell(Passivate.INSTANCE);
    registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

    //using awaitAssert to retry because it might take longer for the groupActor
    //to see the terminated, that order is undefined

    registeredProbe.awaitAssert(() -> {
      groupActor.tell(new RequestDeviceList(1L, "group", deviceListTestProbe.getRef()));
      ReplyDeviceList r = deviceListTestProbe.receiveMessage();

      assertEquals(1L, r.requestId());
      assertEquals(Set.of("device1"), r.ids());
      return null;
    });


  }
}


 