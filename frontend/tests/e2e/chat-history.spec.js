const { test, expect } = require('@playwright/test');

test('TC-FE-Chat-001 历史列表与详情渲染', async ({ page }) => {
  await page.route('**/api/chat/history/list', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          { chatId: 'chat-1', lastMessage: 'hello world', createTime: new Date().toISOString(), updateTime: new Date().toISOString() }
        ],
        message: ''
      })
    });
  });

  await page.route('**/api/chat/history/detail?*', route => {
    const url = new URL(route.request().url());
    const chatId = url.searchParams.get('chatId');
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          chatId,
          messages: [
            { messageType: 'USER', message: '你好', createTime: new Date().toISOString() },
            { messageType: 'ASSISTANT', message: '你好，有什么可以帮助的？', createTime: new Date().toISOString() }
          ]
        },
        message: ''
      })
    });
  });

  await page.goto('/fitness');
  await expect(page.getByText('历史对话')).toBeVisible();
  await expect(page.locator('.dialogue-item')).toHaveCount(1);
  await expect(page.locator('.user-message')).toContainText('你好');
  await expect(page.locator('.ai-message')).toContainText('你好，有什么可以帮助的？');
});

