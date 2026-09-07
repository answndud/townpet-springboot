import { expect, test } from "@playwright/test";

const password = "townpet-demo-123!";

async function login(page: import("@playwright/test").Page, email: string, next: string) {
  await page.goto(`/login?next=${encodeURIComponent(next)}`);
  await page.getByLabel("이메일").fill(email);
  await page.getByLabel("비밀번호", { exact: true }).fill(password);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(new RegExp(`${next.replaceAll("/", "\\/")}$`));
}

test("non-owner cannot mutate another member's publication", async ({ page, browser }) => {
  await login(page, "demo-member-1@townpet.local", "/posts/new");
  await page.getByLabel("제목").fill(`권한 경계 ${Date.now()}`);
  await page.getByLabel("본문").fill("원 작성자만 변경할 수 있는 원본");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);
  const publicationUrl = page.url();
  const publicationId = publicationUrl.split("/").pop()!;

  const context = await browser.newContext();
  const otherPage = await context.newPage();
  await login(otherPage, "demo-member-2@townpet.local", publicationUrl.replace(new URL(publicationUrl).origin, ""));
  await expect(otherPage.getByRole("link", { name: "수정", exact: true })).toHaveCount(0);
  await expect(otherPage.getByRole("button", { name: "삭제", exact: true })).toHaveCount(0);
  const result = await otherPage.evaluate(async (id) => {
    const csrf = document.cookie.split("; ").find((item) => item.startsWith("XSRF-TOKEN="))?.split("=")[1] ?? "";
    const response = await fetch(`/api/v1/publications/${id}`, {
      method: "DELETE",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": decodeURIComponent(csrf) },
      body: JSON.stringify({ version: 0 }),
    });
    return response.status;
  }, publicationId);
  expect(result).toBe(403);
  await expect(otherPage.getByRole("heading", { name: /권한 경계/ })).toBeVisible();
  await context.close();
});
