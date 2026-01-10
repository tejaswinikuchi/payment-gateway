# Payment Gateway – Backend (Deliverable 1)

This project implements the backend foundation of a payment gateway similar to Razorpay/Stripe.

## ✅ Implemented Features

- Dockerized backend using Spring Boot + PostgreSQL
- Single-command startup using `docker-compose up -d`
- Auto-seeded test merchant on application startup
- Secure order creation API with API key & secret authentication
- UPI payment creation API
- Correct database schema for merchants, orders, and payments
- Proper foreign key relationships and indexes
- Order & payment ID generation using required formats
- Payment records stored with `processing` status
- Database persistence verified

## 🧪 Test Merchant (Auto-Seeded)

Email: test@example.com

API Key: key_test_abc123
API Secret: secret_test_xyz789


## 🚀 How to Run

```bash
docker-compose up -d --build


API runs at: http://localhost:8000

🔌 API Examples
Create Order
curl -X POST http://localhost:8000/api/v1/orders \
 -H "X-Api-Key: key_test_abc123" \
 -H "X-Api-Secret: secret_test_xyz789" \
 -H "Content-Type: application/json" \
 -d '{"amount":50000,"receipt":"demo"}'

Create UPI Payment
curl -X POST http://localhost:8000/api/v1/payments \
 -H "Content-Type: application/json" \
 -d '{"order_id":"<ORDER_ID>","method":"upi","vpa":"user@paytm"}'

📦 Database Schema

merchants

orders

payments

All tables follow the required specification with proper constraints and indexes.

⚠️ Scope of This Submission
Implemented

Backend API

Order management

UPI payment processing

Dockerized deployment

Planned (Next Deliverables)

Card payment validation (Luhn, expiry, network)

Payment success/failure simulation

Payment status polling

Dashboard frontend

Hosted checkout page

Public checkout APIs

Test mode configuration

📝 Notes

This submission focuses on backend system correctness, database integrity, and containerized deployment.
Frontend and advanced payment simulations will be completed in the next deliverable.