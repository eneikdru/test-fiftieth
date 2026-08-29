import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  reporter: [
    ['list'],
    ['json', { outputFile: 'test-results/e2e-catalog-search-results.json' }]
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_TEST_BASE_URL || 'http://127.0.0.1:18080',
  },
  webServer: {
    command: 'node serve.mjs',
    url: 'http://127.0.0.1:18080',
    reuseExistingServer: !process.env.CI,
    cwd: '.',
  },
});
