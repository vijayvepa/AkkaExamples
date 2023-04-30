package shopping;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.ShoppingCart;
import shopping.grpc.ShoppingCartServer;
import shopping.grpc.ShoppingCartServiceImpl;
import shopping.kafka.ProduceEventsProjection;
import shopping.order.ShoppingOrderProjection;
import shopping.popularity.ItemPopularityProjection;
import shopping.popularity.repository.ItemPopularityRepository;
import akka.grpc.GrpcClientSettings;
import shopping.order.proto.ShoppingOrderService;
import shopping.order.proto.ShoppingOrderServiceClient;


public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  static ShoppingOrderService orderServiceClient(ActorSystem<?> system) {
    GrpcClientSettings orderServiceClientSettings =
        GrpcClientSettings.connectToServiceAt(
                system.settings().config().getString("shopping-order-service.host"),
                system.settings().config().getInt("shopping-order-service.port"),
                system)
            .withTls(false);

    return ShoppingOrderServiceClient.create(orderServiceClientSettings, system);
  }

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    try {
      init(system, orderServiceClient(system));

    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system, ShoppingOrderService shoppingOrderService) {
    AkkaManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ApplicationContext springContext = SpringIntegration.applicationContext(system);

    ItemPopularityRepository itemPopularityRepository =
        springContext.getBean(ItemPopularityRepository.class);
    final JpaTransactionManager jpaTransactionManager =
        springContext.getBean(JpaTransactionManager.class);

    ItemPopularityProjection.init(system, jpaTransactionManager, itemPopularityRepository);
    ProduceEventsProjection.init(system, jpaTransactionManager);

    final Config config = system.settings().config();
    final String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    final int grpcPort = config.getInt("shopping-cart-service.grpc.port");

    final ShoppingCartServiceImpl shoppingCartService = new ShoppingCartServiceImpl(system, itemPopularityRepository);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, shoppingCartService);
    ShoppingCart.init(system);

    ShoppingOrderProjection.init(system, jpaTransactionManager, shoppingOrderService);

  }
}
