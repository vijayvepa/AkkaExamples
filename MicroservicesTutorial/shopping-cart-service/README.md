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

