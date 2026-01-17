export default function Dashboard() {
  return (
    <div data-test-id="dashboard">
      <div data-test-id="api-credentials">
        <div>
          <label>API Key</label>
          <span data-test-id="api-key">key_test_abc123</span>
        </div>
        <div>
          <label>API Secret</label>
          <span data-test-id="api-secret">secret_test_xyz789</span>
        </div>
      </div>

      <div data-test-id="stats-container">
        <div data-test-id="total-transactions">0</div>
        <div data-test-id="total-amount">₹0</div>
        <div data-test-id="success-rate">0%</div>
      </div>
    </div>
  );
}
