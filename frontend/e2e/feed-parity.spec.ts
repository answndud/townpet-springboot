import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("member feed includes owned local posts while guest feed remains global", async ({ browser, page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const globalTitle = `TownPet ${device} 전체 피드 글`;
  const localTitle = `TownPet ${device} 동네 피드 글`;

  await page.goto("/login?next=/onboarding");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/onboarding$/);
  await page.getByLabel("대표 동네").selectOption({ label: "서울 마포구" });
  await page.getByRole("button", { name: "설정 저장" }).click();
  await expect(page.getByText("내 동네와 반려동물 정보가 저장되었습니다.")).toBeVisible();

  await createPublication(page, globalTitle, "GLOBAL");
  await createPublication(page, localTitle, "LOCAL");

  await page.goto("/feed");
  await expect(page.getByRole("heading", { name: "내 동네와 전체 새 글" })).toBeVisible();
  await expect(page.getByRole("heading", { name: globalTitle })).toBeVisible();
  await expect(page.getByRole("heading", { name: localTitle })).toBeVisible();

  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  await guestPage.goto("http://localhost:5173/feed/guest");
  await expect(guestPage.getByRole("heading", { name: "공개 반려생활 피드" })).toBeVisible();
  await expect(guestPage.getByRole("heading", { name: globalTitle })).toBeVisible();
  await expect(guestPage.getByRole("heading", { name: localTitle })).toHaveCount(0);
  await guestContext.close();
  expect(browserErrors).toEqual([]);
});

async function createPublication(
  page: import("@playwright/test").Page,
  title: string,
  scope: "GLOBAL" | "LOCAL",
) {
  await page.goto("/posts/new");
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("본문").fill(`${title}의 실제 PostgreSQL 피드 검증 본문입니다.`);
  if (scope === "LOCAL") {
    await page.getByRole("radio", { name: /내 동네/ }).check();
  }
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);
}
