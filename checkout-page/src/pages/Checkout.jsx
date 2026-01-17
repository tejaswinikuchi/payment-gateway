import React, { useState } from "react";

export default function Checkout() {
  const [method, setMethod] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [status, setStatus] = useState(null);

  const handlePay = (e) => {
    e.preventDefault();
    setProcessing(true);
    setStatus(null);

    setTimeout(() => {
      setProcessing(false);
      setStatus("success");
    }, 2000);
  };

  return (
    <div data-test-id="checkout-container">

      <div data-test-id="order-summary">
        <h2>Complete Payment</h2>
        <div>
          <span>Amount: </span>
          <span data-test-id="order-amount">₹500.00</span>
        </div>
        <div>
          <span>Order ID: </span>
          <span data-test-id="order-id">order_test</span>
        </div>
      </div>

      <div data-test-id="payment-methods">
        <button data-test-id="method-upi" onClick={() => setMethod("upi")}>
          UPI
        </button>
        <button data-test-id="method-card" onClick={() => setMethod("card")}>
          Card
        </button>
      </div>

      {method === "upi" && !processing && (
        <form data-test-id="upi-form" onSubmit={handlePay}>
          <input data-test-id="vpa-input" placeholder="username@bank" />
          <button data-test-id="pay-button">Pay ₹500</button>
        </form>
      )}

      {method === "card" && !processing && (
        <form data-test-id="card-form" onSubmit={handlePay}>
          <input data-test-id="card-number-input" placeholder="Card Number" />
          <input data-test-id="expiry-input" placeholder="MM/YY" />
          <input data-test-id="cvv-input" placeholder="CVV" />
          <input data-test-id="cardholder-name-input" placeholder="Name" />
          <button data-test-id="pay-button">Pay ₹500</button>
        </form>
      )}

      {processing && (
        <div data-test-id="processing-state">
          <span data-test-id="processing-message">
            Processing payment...
          </span>
        </div>
      )}

      {status === "success" && (
        <div data-test-id="success-state">
          <h2>Payment Successful!</h2>
          <span data-test-id="payment-id">pay_test</span>
          <span data-test-id="success-message">
            Your payment has been processed successfully
          </span>
        </div>
      )}

      {status === "failed" && (
        <div data-test-id="error-state">
          <span data-test-id="error-message">
            Payment could not be processed
          </span>
          <button data-test-id="retry-button">Try Again</button>
        </div>
      )}

    </div>
  );
}
