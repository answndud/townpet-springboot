import { expect, test } from "@playwright/test";

const publication = {
  id: "00000000-0000-4000-8000-000000003901",
  title: "데스크톱 시각 기준선 게시글",
  body: "1280과 1440 데스크톱 화면의 시각 회귀를 확인하는 고정 fixture입니다.",
  scope: "GLOBAL",
  authorId: "00000000-0000-4000-8000-000000000202",
  neighborhoodId: null,
  lifecycle: "ACTIVE",
  createdAt: "2026-08-01T10:00:00Z",
  updatedAt: "2026-08-01T10:00:00Z",
  version: 0,
};
const member = {
  id: "00000000-0000-4000-8000-000000000201",
  nickname: "demo-member-1",
  role: "MEMBER",
  bio: null,
  neighborhoodId: null,
  pets: [],
  showPublicPosts: true,
  showPublicComments: true,
  showPublicPets: true,
  showPublicReactions: true,
};
const comment = {
  id: "00000000-0000-4000-8000-000000003902",
  publicationId: publication.id,
  authorId: "00000000-0000-4000-8000-000000000202",
  parentCommentId: null,
  body: "댓글 아래에 답글 입력창이 열리는지 확인합니다.",
  lifecycle: "ACTIVE",
  createdAt: "2026-08-01T10:00:00Z",
  updatedAt: "2026-08-01T10:00:00Z",
  version: 0,
};

for (const viewport of [
  { name: "desktop-1280", width: 1280, height: 900 },
  { name: "desktop-1440", width: 1440, height: 900 },
]) {
  test.describe(viewport.name, () => {
    test.use({ viewport });

    test.beforeEach(async ({ page }) => {
      await page.route("**/api/v1/members/me", (route) => route.fulfill({ status: 401, contentType: "application/json", body: JSON.stringify({ detail: "Unauthorized" }) }));
      await page.route("**/api/v1/feed*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ items: [publication], page: { nextCursor: null, hasNext: false } }) }));
      await page.route("**/api/v1/publications/*/comments", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ items: [] }) }));
      await page.route("**/api/v1/publications/*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(publication) }));
    });

    test("keeps the home shell visually stable", async ({ page }) => {
      await page.goto("/");
      await expect(page.getByRole("heading", { name: "우리 동네 반려생활 정보" })).toBeVisible();
      await expect(page).toHaveScreenshot(`${viewport.name}-home.png`, { animations: "disabled", caret: "hide" });
    });

    test("keeps the public feed layout visually stable", async ({ page }) => {
      await page.goto("/feed/guest");
      await expect(page.getByRole("heading", { name: "공개 반려생활 피드" })).toBeVisible();
      await expect(page.getByRole("heading", { name: publication.title })).toBeVisible();
      await expect(page).toHaveScreenshot(`${viewport.name}-public-feed.png`, { animations: "disabled", caret: "hide" });
    });

    test("keeps the marketplace form visually stable", async ({ page }) => {
      await page.route("**/api/v1/members/me", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(member) }));
      await page.goto("/marketplace/new");
      await expect(page.getByRole("heading", { name: "새 거래 글" })).toBeVisible();
      await expect(page).toHaveScreenshot(`${viewport.name}-marketplace-form.png`, { animations: "disabled", caret: "hide" });
    });

    test("keeps the nested reply composer visually stable", async ({ page }) => {
      await page.route("**/api/v1/members/me", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(member) }));
      await page.route(`**/api/v1/publications/${publication.id}`, (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(publication) }));
      await page.route(`**/api/v1/publications/${publication.id}/comments`, (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ items: [comment] }) }));
      await page.route(`**/api/v1/publications/${publication.id}/reaction`, (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ active: false, count: 1 }) }));
      await page.route(`**/api/v1/publications/${publication.id}/bookmark`, (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ active: false }) }));
      await page.route(`**/api/v1/members/${comment.authorId}/relationship`, (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ following: false, blocking: false }) }));
      await page.route("**/api/posts/*/view", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ viewCount: 1 }) }));
      await page.goto(`/posts/${publication.id}`);
      await expect(page.getByText(comment.body)).toBeVisible();
      await page.getByRole("button", { name: "답글" }).click();
      await expect(page.getByRole("form", { name: "답글 작성" })).toBeVisible();
      await page.evaluate(() => window.scrollTo(0, 0));
      await expect(page).toHaveScreenshot(`${viewport.name}-reply-composer.png`, { animations: "disabled", caret: "hide" });
    });
  });
}
