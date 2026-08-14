import { expect, test } from "@playwright/test";

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
