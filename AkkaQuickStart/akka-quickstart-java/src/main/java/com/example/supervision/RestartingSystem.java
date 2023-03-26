package com.example.supervision;

import akka.actor.typed.ActorSystem;
import com.example.supervision.actors.MainActor;

public class RestartingSystem {

  public static void main(String[] args) {
    start();
  }

  static void start() {
    final ActorSystem<String> testSystem = ActorSystem.create(MainActor.create(), "testSystem");
    testSystem.tell("Start");
  }
}
