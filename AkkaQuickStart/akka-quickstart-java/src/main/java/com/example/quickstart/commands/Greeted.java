package com.example.quickstart.commands;

import akka.actor.typed.ActorRef;

import java.util.Objects;

public record Greeted(
    String whom,
    ActorRef<Greet> from
) {

  // #greeter
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Greeted greeted = (Greeted) o;
    return Objects.equals(whom(), greeted.whom()) &&
        Objects.equals(from(), greeted.from());
  }

  @Override
  public int hashCode() {
    return Objects.hash(whom(), from());
  }

  @Override
  public String toString() {
    return "Greeted{" +
        "whom='" + whom() + '\'' +
        ", from=" + from() +
        '}';
  }
// #greeter
}
