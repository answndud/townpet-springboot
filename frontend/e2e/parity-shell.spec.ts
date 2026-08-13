import { expect, test } from "@playwright/test";

test.describe("TownPet shell parity", () => {
  test("home preserves primary acquisition journeys", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle("TownPet | 우리 동네 반려생활 정보");
    await expect(page.getByRole("heading", { name: "HOT 글" })).toBeVisible();
    await expect(page.getByRole("button", { name: "동물 게시판" })).toBeVisible();
    await expect(page.getByTestId("header-login-link-home")).toHaveAttribute("href", "/login");
    await expect(page.locator("img[alt=TownPet]")).toBeVisible();
  });

  test("mobile shell keeps the topic row usable", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "HOT 글" })).toBeVisible();
    await expect(page.getByRole("button", { name: "동물 게시판" })).toBeVisible();
  });
});
