### Payment Gateway  - Full Stack Implementation

This project is a simplified payment gateway system built as part of an assignment.
It demonstrates how a real-world payment gateway works, including merchant authentication, order creation, checkout flow, and payment processing.

The application is fully containerized using Docker and can be started with a single command.

## Overview

The system allows a merchant to:

Authenticate using API Key and API Secret

- Create orders

- Accept payments through UPI or card

- View merchant credentials on a dashboard

It also provides a checkout page where customers can fetch an order and complete the payment.

## System Architecture

The project consists of the following components:

- Backend API
Built using Spring Boot.
Handles authentication, order management, payment processing, and validations.

- PostgreSQL Database
Stores merchants, orders, and payments.
Automatically initialized and seeded on startup.

- Merchant Dashboard
A static frontend served via Nginx.
Displays merchant name and API credentials.

- Checkout Page
A static frontend served via Nginx.
Used by customers to fetch an order and make payments.

## Ports Used
Service	                        Port
Backend API	                    8000
Merchant Dashboard	            3000
Checkout Page	                3001
PostgreSQL	                    5432

## Test Merchant Credentials

A test merchant is automatically created when the application starts.

- Merchant Name: Test Merchant
- API Key:       key_test_abc123
- API Secret:    secret_test_xyz789


These credentials are used for all authenticated API requests.

## Prerequisites

- Docker

- Docker Compose

No other setup is required.

## How to Run the Project

From the project root directory:

- docker-compose up -d


This command will:

- Build all images

- Start the backend API

- Start PostgreSQL

- Start the dashboard

- Start the checkout page

Seed the database automatically

## API Usage
### Create an Order (Authenticated)

## Endpoint

POST /api/v1/orders


## Headers

X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789
Content-Type: application/json


# Request Body

{
  "amount": 50000
}


# Response

{
  "id": "order_xxxxx",
  "amount": 50000,
  "currency": "INR",
  "status": "created"
}

# Fetch Order (Public -  Used by Checkout)

# Endpoint

GET /api/v1/orders/{order_id}


No authentication headers are required.

# Create Payment -  UPI

# Endpoint

POST /api/v1/payments


# Headers

X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789
Content-Type: application/json


# Request Body

{
  "order_id": "order_xxxxx",
  "method": "upi",
  "vpa": "user@paytm"
}

# Create Payment -  Card

# Endpoint

POST /api/v1/payments


# Request Body

{
  "order_id": "order_xxxxx",
  "method": "card",
  "card": {
    "number": "4111111111111111",
    "expiry_month": "12",
    "expiry_year": "2026",
    "cvv": "123",
    "holder_name": "John Doe"
  }
}


Card validation includes:

- Luhn algorithm

- Network detection (Visa, Mastercard, Amex, RuPay)

# Fetch Payment Status

# Endpoint

GET /api/v1/payments/{payment_id}


# Headers

X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789

# Frontend Access

- Merchant Dashboard:
http://localhost:3000

- Checkout Page:
http://localhost:3001

# Notes

- All services are fully containerized.

- No manual database setup is required.

- Authentication is enforced using a custom API key filter.

- Checkout endpoints are publicly accessible as required.

# Conclusion

This project demonstrates a complete end-to-end payment gateway flow, covering backend APIs, authentication, payment logic, and frontend integration, following real-world design principles.