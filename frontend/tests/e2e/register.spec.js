const { test, expect } = require('@playwright/test');

test('TC-FE-Register-001 注册校验与成功跳转登录', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/register', route => {
    const request = route.request();
    const postData = request.postDataJSON();
    if (postData?.userAccount && postData?.userPassword && postData?.checkPassword && postData.userPassword === postData.checkPassword) {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: { id: 2 } }) });
    } else {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 40000, message: '参数错误' }) });
    }
  });

  await page.goto('/register');
  await expect(page).toHaveURL('http://localhost:8080/register');

  await page.getByPlaceholder('请输入您的账号（至少4个字符）').fill('u');
  await page.getByRole('button', { name: '注册' }).click();
  await expect(page.locator('.arco-message').getByText('账号至少4个字符')).toBeVisible();

  await page.getByPlaceholder('请输入您的账号（至少4个字符）').fill('user123');
  await page.getByPlaceholder('请输入您的密码（至少8个字符）').fill('short');
  await page.getByRole('button', { name: '注册' }).click();
  await expect(page.locator('.arco-message').getByText('密码至少8个字符')).toBeVisible();

  await page.getByPlaceholder('请输入您的密码（至少8个字符）').fill('password123');
  await page.getByPlaceholder('请再次输入密码').fill('password321');
  await page.getByRole('button', { name: '注册' }).click();
  await expect(page.locator('.arco-message').getByText('两次输入的密码不一致')).toBeVisible();

  await page.getByPlaceholder('请再次输入密码').fill('password123');
  await page.getByRole('button', { name: '注册' }).click();
  await expect(page).toHaveURL('http://localhost:8080/login');
});
