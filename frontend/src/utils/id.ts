export function createRequestId() {
  return `REQ-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}
