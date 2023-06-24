## Setup DB

```shell
docker ps
# >shopping-cart-service-postgres-db-1

docker exec -i shopping-cart-service-postgres-db-1 psql -U shopping-cart -t < ddl-scripts/create_tables.sql

```

## Running the sample code



1. Start a first node:

    ```
    mvn compile exec:exec -DAPP_CONFIG=local1.conf
    ```

2. (Optional) Start another node with different ports:

    ```
    mvn compile exec:exec -DAPP_CONFIG=local2.conf
    ```

3. Check for service readiness

    ```
    curl http://localhost:9101/ready
    ```

## Checking Local Data

### Local Connection

```
jdbc:postgresql://localhost:5432/shopping-cart
shopping-cart
shopping-cart
```

### Queries
```sql
-- List of all events
SELECT * FROM public.event_journal
--Projections

```

## Deployment

- Building
```shell
mvn -Ddocker.username=vijayvepa -Ddocker.registry=docker.io/vijayvepa -Ddocker.password=? package docker:push
```

## References

- [Akka Microservices](https://developer.lightbend.com/docs/akka-guide/microservices-tutorial/index.html)
- [Deploying Akka Cluster to Kubernetes](https://doc.akka.io/docs/akka-management/current/kubernetes-deployment/index.html)
- [Example Projects](https://doc.akka.io/docs/akka/current/project/examples.html)
- [Deploy Kafka with Kubernetes](https://dzone.com/articles/how-to-deploy-apache-kafka-with-kubernetes)
- [Connecting Applications in Kubernetes](https://kubernetes.io/docs/tutorials/services/connect-applications-service/)
- [Deploy Postgres](https://www.airplane.dev/blog/deploy-postgres-on-kubernetes)
- [Add DDL Script](https://stackoverflow.com/a/58450732/474377)

## StackOverflow
- [Referencing Env Vars in Kube](https://stackoverflow.com/a/49583616/474377)