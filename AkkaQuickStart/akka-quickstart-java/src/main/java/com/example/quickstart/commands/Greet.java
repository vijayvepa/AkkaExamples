package com.example.quickstart.commands;

import akka.actor.typed.ActorRef;

public record Greet(
    String whom,
    ActorRef<Greeted> replyTo
) {
}
