import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.1.0/index.js';
import { log } from 'k6';

export const options = {
    iterations: 10000, // Total number of requests
    vus: 1000, // Number of virtual users
};

export default function () {
    // Generate a random query string with arbitrary length
    const query = encodeURIComponent(randomString(Math.floor(Math.random() * 100) + 1));
    const url = `http://192.168.230.200:8112/search?q=${query}`;

    // Make the HTTP request
    const res = http.get(url);

    // Log the response status
    log(`Response status: ${res.status}`);

    // Check if the response status is 200
    const checkRes = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // Log if the check failed
    if (!checkRes) {
        log(`Request failed with status: ${res.status}`);
    }

    // Imitate a new client for every 50 requests
    if (__ITER % 50 === 0) {
        http.cookieJar().clear();
    }

    // Sleep for a random duration between 1 and 3 seconds
    sleep(Math.random() * 2 + 1);
}
