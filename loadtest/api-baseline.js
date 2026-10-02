import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL;
const TOKEN = __ENV.TEST_JWT;
const API_PATH = __ENV.API_PATH || '/users/me/items';
const RATE = Number(__ENV.RATE || 2);

export const options = {
  scenarios: {
    api_baseline: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 2,
      maxVUs: 5,
    },
  },
  thresholds: {
    checks: ['rate == 1'],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}${API_PATH}`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
    tags: { name: API_PATH },
  });

  check(response, {
    [`${API_PATH} returns 200`]: (res) => res.status === 200,
  });
}
