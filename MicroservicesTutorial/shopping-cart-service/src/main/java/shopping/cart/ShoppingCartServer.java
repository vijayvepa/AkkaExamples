package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.grpc.javadsl.ServerReflection;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.function.Function;


import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

import shopping.cart.proto.ShoppingCartService;
import shopping.cart.proto.ShoppingCartServiceHandlerFactory;


public class ShoppingCartServer {

  private ShoppingCartServer() {
  }

  static void start(
      String host,
      int port,
      ActorSystem<?> system,
      ShoppingCartService grpcService) {

    @SuppressWarnings("unchecked")
    Function<HttpRequest, CompletionStage<HttpResponse>> service =
        ServiceHandler.concatOrNotFound(
            getGrpcServiceSystem(system, grpcService),
            getServerReflection(system));

    CompletionStage<ServerBinding> bound =
        Http.get(system).newServerAt(host, port).bind(service);

    bound.whenComplete(
        (binding, ex) -> setupCoordinatedShutdown(binding, ex, system));
  }

  private static Function<HttpRequest, CompletionStage<HttpResponse>> getGrpcServiceSystem(
      ActorSystem<?> system,
      ShoppingCartService grpcService) {
    return ShoppingCartServiceHandlerFactory.create(grpcService, system);
  }

  /**
   *
   * ServerReflection enabled to support grpcurl without import-path and proto parameters
   * @param system actor system
   * @return server reflection
   */
  private static Function<HttpRequest, CompletionStage<HttpResponse>> getServerReflection(ActorSystem<?> system) {
    return ServerReflection.create(
        Collections.singletonList(ShoppingCartService.description), system);
  }

  static void setupCoordinatedShutdown (ServerBinding binding, Throwable ex, ActorSystem<?> system){
    if (binding == null) {
      system.log().error("Failed to bind gRPC endpoint, terminating system", ex);
      system.terminate();
      return;
    }

    binding.addToCoordinatedShutdown(Duration.ofSeconds(3), system);
    InetSocketAddress address = binding.localAddress();
    system
        .log()
        .info(
            "Shopping online at gRPC server {}:{}",
            address.getHostString(),
            address.getPort());
  }
}