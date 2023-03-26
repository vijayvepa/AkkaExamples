package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.example.quickstart.actors.Greeter;
import com.example.quickstart.commands.Greet;
import com.example.quickstart.commands.Greeted;
import org.junit.ClassRule;
import org.junit.Test;

//#definition
public class AkkaQuickstartTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();
//#definition

  //#test
  @Test
  public void testGreeterActorSendingOfGreeting() {
    TestProbe<Greeted> testProbe = testKit.createTestProbe();
    ActorRef<Greet> underTest = testKit.spawn(Greeter.create(), "greeter");
    underTest.tell(new Greet("Charles", testProbe.getRef()));
    testProbe.expectMessage(new Greeted("Charles", underTest));
  }
  //#test
}
