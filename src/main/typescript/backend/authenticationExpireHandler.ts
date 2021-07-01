export function authenticationExpireHandler(): void {
  if (window.location.pathname.startsWith("/login")) return;
  localStorage.removeItem("accessToken");
  window.location.href = "/login";
}
