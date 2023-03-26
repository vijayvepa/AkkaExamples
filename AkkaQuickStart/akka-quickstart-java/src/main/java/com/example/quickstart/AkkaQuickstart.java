package com.example.quickstart;

import akka.actor.typed.ActorSystem;
import com.example.quickstart.actors.GreeterMain;
import com.example.quickstart.commands.SayHello;

import java.io.IOException;

public class AkkaQuickstart {
  public static void main(String[] args) {
    //#actor-system
    final ActorSystem<SayHello> greeterMain = ActorSystem.create(GreeterMain.create(), "helloakka");
    //#actor-system

    //#main-send-messages
    greeterMain.tell(new SayHello("Charles"));
    //#main-send-messages

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
      greeterMain.terminate();
    }
  }
}
