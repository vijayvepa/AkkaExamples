kubectl apply -f namespace-k.yaml
kubectl config set-context --current --namespace=shopping-cart-with-kafka
kubectl apply -f zookeeper.yaml
kubectl apply -f kafka-broker.yaml
kubectl apply -f postgres.yaml
kubectl apply -f akka-cluster-kafka.yaml
