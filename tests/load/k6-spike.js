import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
  },
  scenarios: {
    spike_orders: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 5 },
        { duration: '30s', target: 40 },
        { duration: '1m', target: 40 },
        { duration: '30s', target: 5 },
      ],
    },
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'local-dev-key';

export default function () {
  const orderId = `k6-spike-${__VU}-${__ITER}-${Date.now()}`;
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
