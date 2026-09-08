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
      if (message.type() === "error") browserErrors.push(`console: ${message.text()}`);
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
  await login(fillerPage, "e2e-member-desktop@townpet.local", gatheringUrl.replace(new URL(gatheringUrl).origin, ""));
  const fillerJoin = fillerPage.waitForResponse((response) => response.url().endsWith(`/api/v1/gatherings/${gatheringId}/participants`) && response.request().method() === "POST");
  await fillerPage.getByRole("button", { name: "참여하기" }).click();
  expect((await fillerJoin).status()).toBe(200);
  await expect(fillerPage.getByText("2/2명 참여")).toBeVisible();
  await fillerContext.close();

  const thirdContext = await browser.newContext();
  const thirdPage = await thirdContext.newPage();
  observeBrowserErrors(thirdPage);
  await login(thirdPage, "e2e-member-mobile@townpet.local", gatheringUrl.replace(new URL(gatheringUrl).origin, ""));
  const overflowJoin = thirdPage.waitForResponse((response) => response.url().endsWith(`/api/v1/gatherings/${gatheringId}/participants`) && response.request().method() === "POST");
  await thirdPage.getByRole("button", { name: "참여하기" }).click();
  expect((await overflowJoin).status()).toBe(409);
  await expect(thirdPage.getByRole("alert")).toHaveText("모임 정원이 가득 찼습니다.");
  await expect(thirdPage.getByText("2/2명 참여")).toBeVisible();
  await thirdContext.close();

  await page.reload();
  await expect(page.getByText("2/2명 참여")).toBeVisible();
  await page.getByRole("button", { name: "모임 취소" }).click();
  await expect(page.getByRole("button", { name: "모임 취소" })).toHaveCount(0);
  expect(browserErrors).toEqual([]);
});
