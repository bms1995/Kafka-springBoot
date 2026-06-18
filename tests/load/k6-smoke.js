import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000'],
  },
  scenarios: {
    steady_orders: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
    },
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'local-dev-key';

export default function () {
  const orderId = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    orderId,
    productName: 'MacBook Pro',
    quantity: 1,
    amount: 250,
    customerEmail: 'client@test.com',
  });

  const response = http.post(`${baseUrl}/api/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
    },
  });

  check(response, {
    'order accepted': (res) => res.status >= 200 && res.status < 300,
  });

  sleep(1);
}
