const { test, expect } = require('@playwright/test');

test('TC-FE-Knowledge-001 分类切换到营养显示卡片', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.goto('/knowledge');
  await expect(page.getByText('健身知识库')).toBeVisible();
  await page.getByText('营养知识').click();
  await expect(page.locator('.nutrition-cards .nutrient-card').first()).toBeVisible();
});

