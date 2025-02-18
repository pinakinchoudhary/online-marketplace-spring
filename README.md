# Online Marketplace SpringBoot

This repository contains the code for a simple online marketplace implemented using Spring Boot. The marketplace consists of three separate services: Account Service, Marketplace Service, and Wallet Service.

## Overview

The online marketplace allows users to register, browse products, place orders, and manage their wallet balances.  The system is designed with a microservices architecture, promoting modularity and scalability.

### Services

*   **Account Service:** Manages user accounts, including registration, deletion, and discount tracking.
*   **Marketplace Service:** Handles product listings, order processing, and inventory management.
*   **Wallet Service:**  Manages user wallet balances and provides endpoints for debiting and crediting funds.

## Features


*   **User Management:**
    *   User registration with unique email validation.
    *   User deletion with cascading effects on orders and wallets.
    *   Retrieval of user details.
    *   Discount tracking (each user gets a 10% discount on their first order).

*   **Product Catalog:**
    *   Display of available products with details (name, description, price, and current stock).
    *   Retrieval of individual product details.
    *   Product data is loaded from a `products.csv` file at startup.

*   **Order Management:**
    *   Order placement with quantity selection.
    *   Real-time stock checking.
    *   Automatic discount application for first-time orders.
    *   Payment processing via the Wallet Service.
    *   Order status tracking (PLACED, CANCELLED, DELIVERED).
    *   Order cancellation (for PLACED orders) with stock restoration and refund.
    *   Marking orders as delivered.
    *   Retrieval of order details.
    *   Retrieval of orders for a specific user.

*   **Wallet Management:**
    *   Wallet creation (if it doesn't exist) upon the first wallet-related operation for a user.
    *   Balance checking.
    *   Debiting funds from the wallet.
    *   Crediting funds to the wallet.

*   **Inter-service Communication:**  The services communicate with each other via HTTP requests to ensure data consistency across the platform. For example, the Marketplace Service interacts with the Account Service to verify users and apply discounts, and with the Wallet Service to process payments.  The Account Service notifies the Marketplace and Wallet Services when a user is deleted.

*   **Data Consistency:**  The system ensures data consistency across services. For example, when a user is deleted, their orders and wallet are also removed.  When an order is cancelled, the stock is restored, and the user's wallet is credited.

*   **Dockerized Deployment:**  Each service is packaged in its own Docker container for easy setup and deployment.

## Getting Started

To run the marketplace locally, you'll need Docker installed.  Each service is packaged in its own Docker container.

### Building and Running the Services

1.  **Clone the repository:**

    ```bash
    git clone <repository_url>
    ```

2.  **Navigate to each service directory:**

    ```bash
    cd account-service  # Repeat for marketplace-service and wallet-service
    ```

3.  **Build the Docker image:**

    ```bash
    docker build -t <service_name> .  # Replace <service_name> with account-service, marketplace-service, or wallet-service
    ```

4.  **Run the Docker container:**

    ```bash
    docker run -p <host_port>:<container_port> --rm --name <container_name> --add-host=host.docker.internal:host-gateway <service_name>
    ```

    *   Replace `<host_port>` with the port you want to expose on your host machine (8080, 8081, and 8082 are recommended for account, marketplace, and wallet, respectively).
    *   Replace `<container_port>` with the port the service is running on inside the container (typically 8080).
    *   Replace `<container_name>` with a descriptive name for the container (e.g., `account`, `marketplace`, `wallet`).
    *   The `--add-host` option is crucial for inter-service communication, especially on Linux. It resolves `host.docker.internal` to the host's IP address within the containers.

    **Example:** For the Account Service:

    ```bash
    docker run -p 8080:8080 --rm --name account --add-host=host.docker.internal:host-gateway account-service
    ```

5.  **Repeat steps 3 and 4 for the Marketplace Service and Wallet Service, adjusting the ports and container names accordingly.**  For example:

    ```bash
    docker run -p 8081:8080 --rm --name marketplace --add-host=host.docker.internal:host-gateway marketplace-service
    docker run -p 8082:8080 --rm --name wallet --add-host=host.docker.internal:host-gateway wallet-service
    ```

6.  **Access the services:**  Once all containers are running, you can access the services at:

    *   Account Service: `http://localhost:8080`
    *   Marketplace Service: `http://localhost:8081`
    *   Wallet Service: `http://localhost:8082`

## Stopping and Removing Containers and Images

* `Ctrl-C` will exit out of the container.
* `--rm` flag will automatically remove the container when exited.

## API Endpoints

Refer to the project documentation (or the code itself) for detailed information on the available API endpoints for each service.

## Testing

The project includes a set of test cases to verify the functionality of each service.  It is recommended to run these tests after building and running the services.  You can use tools like Postman or curl to interact with the API endpoints.  Remember to start with requests to clear all users and orders to ensure a clean slate for each test.

## Project Structure
```
online-marketplace/
├── account-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── ... (Java source code)
├── marketplace-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── ... (Java source code)
├── wallet-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── ... (Java source code)
└── README.md
```

## Technologies Used

*   Spring Boot
*   Maven
*   Docker

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
