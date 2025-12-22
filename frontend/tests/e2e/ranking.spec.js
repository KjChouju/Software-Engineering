const { test, expect } = require('@playwright/test');

test('TC-FE-Ranking-001 排行榜列表渲染与积分显示', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  await page.route('**/api/fitness/ranking/my?*', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          myRank: 2,
          currentScore: 520,
          rankChange: 1,
          totalMinutes: 300,
          totalCalories: 12000
        }
      })
    });
  });

  await page.route('**/api/fitness/ranking/list', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          records: [
            { userId: 1, userName: '用户A', rank: 1, score: 980, totalMinutes: 400, totalCalories: 15000 },
            { userId: 2, userName: '用户B', rank: 2, score: 820, totalMinutes: 350, totalCalories: 13000 },
            { userId: 3, userName: '用户C', rank: 3, score: 760, totalMinutes: 320, totalCalories: 11000 }
          ],
          total: 3,
          current: 1,
          pageSize: 10,
          statisticInfo: null
        }
      })
    });
  });

  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  await page.getByRole('link', { name: '健身排行榜' }).click();
  await expect(page).toHaveURL('http://localhost:8080/ranking');

  await expect(page.getByText('排行榜')).toBeVisible();
  const items = page.locator('.ranking-items .ranking-item');
  await expect(items).toHaveCount(3);
  await expect(items.nth(0).locator('.user-name')).toContainText('用户A');
  await expect(items.nth(0).locator('.score')).toHaveText('980');
});

