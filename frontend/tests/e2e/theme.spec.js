const { test, expect } = require('@playwright/test');

test('TC-FE-Theme-001 主题切换持久化', async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());

  const toggle = page.locator('.theme-toggle');
  await expect(toggle).toBeVisible();

  // 初始应为 light 或 dark，点击切换一次
  await toggle.click();

  // 验证 localStorage 持久化
  const theme = await page.evaluate(() => localStorage.getItem('app-theme'));
  expect(theme).toBe('dark');

  // 刷新后依然为 dark
  await page.reload();
  const isDark = await page.evaluate(() => document.documentElement.classList.contains('dark-theme'));
  expect(isDark).toBeTruthy();
});
