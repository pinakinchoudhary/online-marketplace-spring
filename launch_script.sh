#!/bin/bash
set -e

echo "Stopping and deleting existing minikube cluster..."
minikube stop
minikube delete

echo "Starting minikube..."
minikube start --driver=docker

echo "Setting Docker environment for minikube..."
eval $(minikube docker-env)

echo "Building Docker images inside minikube's Docker daemon..."
docker build -t vansh-pinakin-accountservice:latest ./accountService
docker build -t vansh-pinakin-marketplaceservice:latest ./marketplaceService
docker build -t vansh-pinakin-walletservice:latest ./walletService

echo "Deploying services to minikube..."
kubectl apply -f k8s/accountservice.yaml
kubectl apply -f k8s/marketplaceservice.yaml
kubectl apply -f k8s/walletservice.yaml

echo "Waiting a few seconds for pods to initialize..."
sleep 10

echo "Setting up port forwarding for each service..."
# Forward accountService (service port 8080) to localhost:8080
kubectl port-forward service/accountservice 8080:8080 &
# Forward marketplaceService (service port 8080) to localhost:8081
kubectl port-forward service/marketplaceservice 8081:8080 &
# Forward walletService (service port 8080) to localhost:8082
kubectl port-forward service/walletservice 8082:8080 &

echo "Starting minikube tunnel in the background..."
minikube tunnel &
sleep 5

echo "Services are accessible at:"
echo "  Account Service:      http://localhost:8080"
echo "  Marketplace Service:  http://localhost:8081"
echo "  Wallet Service:       http://localhost:8082"

# Prevent the script from exiting immediately (so port-forwards remain active)
wait

