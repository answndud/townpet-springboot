import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("member and guest feeds show the same public publication posts", async ({ browser, page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("status of 401")) browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const firstTitle = `TownPet ${device} 첫 번째 공개 글`;
  const secondTitle = `TownPet ${device} 두 번째 공개 글`;

  await page.goto("/login?next=/onboarding");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/onboarding$/);
  await page.getByLabel("대표 동네").selectOption({ label: "서울 마포구" });
  await page.getByRole("button", { name: "설정 저장" }).click();
  await expect(page.getByText("내 동네와 반려동물 정보가 저장되었습니다.")).toBeVisible();

  await createPublication(page, firstTitle);
  await createPublication(page, secondTitle);

  await page.goto("/feed");
  await expect(page.getByRole("heading", { name: "내 피드" })).toBeVisible();
  await expect(page.getByRole("heading", { name: firstTitle }).first()).toBeVisible();
  await expect(page.getByRole("heading", { name: secondTitle }).first()).toBeVisible();

  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  await guestPage.goto("http://127.0.0.1:4173/?view=all");
  await expect(guestPage.getByRole("heading", { name: "전체글" })).toBeVisible();
  await expect(guestPage.getByRole("heading", { name: firstTitle }).first()).toBeVisible();
  await expect(guestPage.getByRole("heading", { name: secondTitle }).first()).toBeVisible();
  await guestContext.close();
  expect(browserErrors).toEqual([]);
});

test("guest feed aggregates the synthetic community boards and preserves detail links", async ({ page }) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("status of 401")) browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  await assertFeedItem(page, "망원 산책 초보자를 위한 저녁 코스", "/posts/");
  await assertFeedItem(page, "소형 이동장 판매합니다", "/marketplace/");
  await assertFeedItem(page, "차분한 성격의 믹스견 가족을 찾습니다", "/adoptions/");
  await assertFeedItem(page, "성산동에서 보라색 목줄 강아지를 찾습니다", "/lost-found/");
  await assertFeedItem(page, "망원 한강 저녁 산책 데모", "/gatherings/");
  await assertFeedItem(page, "주말 고양이 돌봄 요청 데모", "/care/");
  await assertFeedItem(page, "망원우리동물병원", "/hospital-reviews");
  await assertFeedItem(page, "보호소 산책 봉사 데모", "/volunteer");
  await assertFeedItem(page, "망원 한강 산책 코스", "/guides/");
  expect(browserErrors).toEqual([]);
});

async function assertFeedItem(
  page: import("@playwright/test").Page,
  title: string,
  hrefPrefix: string,
) {
  await page.goto(`/search/guest?q=${encodeURIComponent(title)}`);
  await expect(page.getByRole("heading", { name: "반려생활 정보 검색" })).toBeVisible();
  const item = page.getByRole("link", { name: title, exact: true });
  await item.scrollIntoViewIfNeeded();
  await expect(item).toBeVisible();
  await expect(item).toHaveAttribute("href", new RegExp(`^${hrefPrefix.replace(/[.*+?^${}()|[\\]\\\\]/g, "\\\\$&")}`));
}

async function createPublication(
  page: import("@playwright/test").Page,
  title: string,
) {
  await page.goto("/posts/new");
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("본문").fill(`${title}의 실제 PostgreSQL 피드 검증 본문입니다.`);
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);
}
