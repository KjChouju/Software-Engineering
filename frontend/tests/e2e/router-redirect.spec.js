const { test, expect } = require('@playwright/test');

test('TC-FE-Router-002 未登录访问受限页带redirect参数', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.goto('/data');
  await expect(page).toHaveURL('http://localhost:8080/login?redirect=/data');
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible();
});
