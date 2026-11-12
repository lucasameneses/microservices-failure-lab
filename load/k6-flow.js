import http from 'k6/http';

export const options = {
  scenarios: {
    background: {
      executor: 'constant-arrival-rate',
      rate: 5,
      timeUnit: '1s',
      duration: '24h',
      preAllocatedVUs: 10,
      maxVUs: 50,
    },
  },
};

export default function () {
  http.get('http://service-a:8080/flow', { timeout: '10s' });
}
