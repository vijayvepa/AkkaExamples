package com.example.actorrefs;

import akka.actor.typed.ActorSystem;
import com.example.actorrefs.actors.Main;

public class ActorHierarchyExperiments {
  public static void main(String[] args) {
    final ActorSystem<String> testSystem = ActorSystem.create(Main.create(), "testSystem");
    testSystem.tell("start");
  }
}
