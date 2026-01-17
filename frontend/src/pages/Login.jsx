export default function Login() {
  return (
    <form data-test-id="login-form">
      <input
        data-test-id="email-input"
        type="email"
        placeholder="Email"
      />
      <input
        data-test-id="password-input"
        type="password"
        placeholder="Password"
      />
      <button data-test-id="login-button">Login</button>
    </form>
  );
}
