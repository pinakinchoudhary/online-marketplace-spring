#!/bin/bash

minikube start

eval $(minikube docker-env)

(
  cd accountService || { echo "Failed to change directory"; exit 1; }
  docker build -t account-service .
)

(
    cd marketplaceService || { echo "Failed to change directory"; exit 1; }
    docker build -t marketplace-service .
)
(
      cd walletService || { echo "Failed to change directory"; exit 1; }
      docker build -t wallet-service .
)

minikube kubectl -- create deployment account-service \
                  --image=account-service || { echo "Failed to run kubectl command"; exit 1; }