package com.example.iotexample;

import akka.actor.typed.ActorSystem;
import com.example.iotexample.actors.IotSupervisor;

public class IotMain {

  public static void main(String[] args) {
    start();
  }

  static void start() {
    final ActorSystem<String> iotSystem = ActorSystem.create(IotSupervisor.create(), "iotSystem");

  }
}
