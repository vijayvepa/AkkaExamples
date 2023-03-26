package com.example.actorlifecycle;

import akka.actor.typed.ActorSystem;
import com.example.actorlifecycle.actors.Main;

public class ActorLifecycle {
  public static void main(String[] args) {
    final ActorSystem<String> testSystem = ActorSystem.create(Main.create(), "testSystem");
    testSystem.tell("start");
  }
}
