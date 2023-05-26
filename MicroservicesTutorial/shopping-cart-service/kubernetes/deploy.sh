kubectl apply -f namespace.yaml
kubectl config set-context --current --namespace=shopping-cart-3
kubectl apply -f akka-cluster.yaml
kubectl config set-context --current --namespace=shopping-cart-k
kubectl apply -f akka-cluster-kafka.yaml