import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000'],
  },
  scenarios: {
    endurance_orders: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '10m',
    },
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'local-dev-key';

export default function () {
  const orderId = `k6-endurance-${__VU}-${__ITER}-${Date.now()}`;
  const response = http.post(
    `${baseUrl}/api/orders`,
    JSON.stringify({
      orderId,
      productName: 'MacBook Pro',
      quantity: 1,
      amount: 250,
      customerEmail: 'client@test.com',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': apiKey,
      },
    }
  );

  check(response, {
    'order accepted': (res) => res.status >= 200 && res.status < 300,
  });

  sleep(1);
}
