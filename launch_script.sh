#!/bin/bash
set -e

echo "Starting minikube..."
minikube start --driver=docker

echo "Setting Docker environment for minikube..."
eval $(minikube docker-env)

echo "Building Docker images inside minikube's Docker daemon..."
docker build -t vansh-pinakin-accountservice:latest ./accountService
docker build -t vansh-pinakin-marketplaceservice:latest ./marketplaceService
docker build -t vansh-pinakin-walletservice:latest ./walletService
docker build -t vansh-pinakin-h2-database:latest ./h2-database

echo "Deploying services to minikube..."
kubectl apply -f config/accountservice.yaml
kubectl apply -f config/marketplaceservice.yaml
kubectl apply -f config/walletservice.yaml
kubectl apply -f config/h2-database.yaml

echo "Waiting a few seconds for pods to initialize..."
sleep 30

echo "Setting up port forwarding for each service..."
kubectl port-forward service/accountservice 8080:8080 &
kubectl port-forward service/marketplaceservice 8081:8081 &
kubectl port-forward service/walletservice 8082:8082 &
kubectl port-forward service/h2-database 9081:9081 9082:9082 &

sleep 5
echo "Starting minikube tunnel in the background..."
minikube tunnel &

# Prevent the script from exiting immediately (so port-forwards remain active)
wait

