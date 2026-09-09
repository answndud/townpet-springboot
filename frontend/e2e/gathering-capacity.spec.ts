import { expect, test } from "@playwright/test";

const password = "townpet-demo-123!";

async function login(page: import("@playwright/test").Page, email: string, next: string) {
  await page.goto(`/login?next=${encodeURIComponent(next)}`);
  await page.getByLabel("이메일").fill(email);
  await page.getByLabel("비밀번호", { exact: true }).fill(password);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(new RegExp(`${next.replaceAll("/", "\\/")}$`));
}

test("full gathering rejects the next participant through the live API", async ({ page, browser }) => {
  const browserErrors: string[] = [];
  const observeBrowserErrors = (observedPage: import("@playwright/test").Page) => {
    observedPage.on("console", (message) => {
      if (message.type() !== "error") return;
      const text = message.text();
      if (text.includes("static.cloudflareinsights.com/beacon") && text.includes("Content Security Policy")) return;
      if (text.includes("Failed to load resource: the server responded with a status of 409")) return;
      browserErrors.push(`console: ${text}`);
    });
    observedPage.on("pageerror", (error) => browserErrors.push(`page: ${error.message}`));
  };

  observeBrowserErrors(page);
  await login(page, "demo-member-1@townpet.local", "/gatherings/new");
  await page.getByLabel("제목").fill(`정원 경계 ${Date.now()}`);
  await page.getByLabel("설명").fill("실제 backend 정원 경계 E2E");
  await page.getByLabel("장소").fill("서울");
  await page.getByLabel("일시").fill("2027-01-01T09:00");
  await page.getByRole("spinbutton", { name: "정원" }).fill("2");
  await page.getByRole("button", { name: "모임 만들기" }).click();
  await expect(page).toHaveURL(/\/gatherings\/[0-9a-f-]+$/);
  const gatheringUrl = page.url();
  const gatheringId = new URL(gatheringUrl).pathname.split("/").pop()!;

  const secondContext = await browser.newContext();
  const secondPage = await secondContext.newPage();
  observeBrowserErrors(secondPage);
  await login(secondPage, "demo-member-2@townpet.local", gatheringUrl.replace(new URL(gatheringUrl).origin, ""));
  const secondJoin = secondPage.waitForResponse((response) => response.url().endsWith(`/api/v1/gatherings/${gatheringId}/participants`) && response.request().method() === "POST");
  await secondPage.getByRole("button", { name: "참여하기" }).click();
  expect((await secondJoin).status()).toBe(200);
  await expect(secondPage.getByText("1/2명 참여")).toBeVisible();
  await secondContext.close();

  const fillerContext = await browser.newContext();
  const fillerPage = await fillerContext.newPage();
  observeBrowserErrors(fillerPage);
  await login(fillerPage, "demo-member-3@townpet.local", gatheringUrl.replace(new URL(gatheringUrl).origin, ""));
  const fillerJoin = fillerPage.waitForResponse((response) => response.url().endsWith(`/api/v1/gatherings/${gatheringId}/participants`) && response.request().method() === "POST");
  await fillerPage.getByRole("button", { name: "참여하기" }).click();
  expect((await fillerJoin).status()).toBe(200);
  await expect(fillerPage.getByText("2/2명 참여")).toBeVisible();
  await fillerContext.close();

  await page.reload();
  await expect(page.getByText("2/2명 참여")).toBeVisible();
  const overflowJoin = page.waitForResponse((response) => response.url().endsWith(`/api/v1/gatherings/${gatheringId}/participants`) && response.request().method() === "POST");
  const overflowStatus = await page.evaluate(async (id) => {
    const csrf = document.cookie.split("; ").find((item) => item.startsWith("XSRF-TOKEN="))?.split("=")[1] ?? "";
    const response = await fetch(`/api/v1/gatherings/${id}/participants`, {
      method: "POST",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": decodeURIComponent(csrf) },
    });
    return response.status;
  }, gatheringId);
  expect((await overflowJoin).status()).toBe(409);
  expect(overflowStatus).toBe(409);
  await expect(page.getByText("2/2명 참여")).toBeVisible();

  await page.getByRole("button", { name: "모임 취소" }).click();
  await expect(page.getByRole("button", { name: "모임 취소" })).toHaveCount(0);
  expect(browserErrors).toEqual([]);
});
