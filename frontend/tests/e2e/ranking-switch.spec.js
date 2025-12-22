const { test, expect } = require('@playwright/test');

test('TC-FE-Ranking-002 切换榜单类型周/月联动', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  let type = 'week';
  await page.route('**/api/fitness/ranking/list', route => {
    const resp = type === 'week'
      ? { code: 0, data: { records: [{ userId: 1, userName: '用户W', rank: 1, score: 500, totalMinutes: 200, totalCalories: 8000 }], total: 1, current: 1, pageSize: 10 } }
      : { code: 0, data: { records: [{ userId: 2, userName: '用户M', rank: 1, score: 900, totalMinutes: 500, totalCalories: 20000 }], total: 1, current: 1, pageSize: 10 } };
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(resp) });
  });

  await page.route('**/api/fitness/ranking/my?*', route => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: { myRank: 1, currentScore: 500, rankChange: 0, totalMinutes: 200, totalCalories: 8000 } }) });
  });

  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  await page.getByRole('link', { name: '健身排行榜' }).click();
  await expect(page).toHaveURL('http://localhost:8080/ranking');

  const items = page.locator('.ranking-items .ranking-item');
  await expect(items.first().locator('.user-name')).toContainText('用户W');

  type = 'month';
  await page.getByText('本月').click();
  await expect(items.first().locator('.user-name')).toContainText('用户M');
});
