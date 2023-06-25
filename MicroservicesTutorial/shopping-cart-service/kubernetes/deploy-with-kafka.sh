
kubectl apply -f namespace-k.yaml
kubectl config set-context --current --namespace=shopping-cart-with-kafka
kubectl create configmap create-tables --from-file=../ddl-scripts/create_tables.sql
kubectl create configmap create-user-tables --from-file=../ddl-scripts/create_user_tables.sql
kubectl apply -f zookeeper.yaml
kubectl apply -f kafka-broker.yaml
kubectl apply -f postgres.yaml
#kubectl apply -f init-db-scripts.yaml
kubectl apply -f akka-cluster-kafka.yaml
