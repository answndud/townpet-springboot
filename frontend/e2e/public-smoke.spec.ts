import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/v1/members/me", route => route.fulfill({status:401,contentType:"application/json",body:"{}"}));
  await page.route("**/api/v1/discovery/**", route => route.fulfill({contentType:"application/json",body:JSON.stringify({items:[],page:{hasNext:false,nextCursor:null}})}));
});

test("mobile navigation keeps guides accessible and closes on route changes", async ({ page }) => {
  await page.setViewportSize({width:390,height:844});
  await page.route("**/api/v1/local-resources*", route => route.fulfill({contentType:"application/json",body:"[]"}));
  await page.goto("/");
  await page.getByRole("button",{name:"메뉴",exact:true}).click();
  const navigation = page.getByRole("navigation",{name:"모바일 주요 이동"});
  await expect(navigation.getByRole("link",{name:"로그인",exact:true})).toHaveCount(0);
  await expect(navigation.getByRole("link",{name:"내 프로필",exact:true})).toHaveCount(0);
  await expect(page.getByTestId("header-login-link-home")).toBeVisible();
  await navigation.getByRole("link",{name:"지역 가이드",exact:true}).click();
  await expect(page.getByRole("heading",{name:"우리 동네 반려생활 가이드"})).toBeVisible();
  await expect(page.getByRole("button",{name:"메뉴",exact:true})).toHaveAttribute("aria-expanded","false");
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBeTruthy();
});

test("anonymous member-write entry redirects to login with its return path", async ({ page }) => {
  await page.goto("/marketplace/new");
  await expect(page.getByRole("heading",{name:"로그인",exact:true})).toBeVisible();
  expect(new URL(page.url()).searchParams.get("next")).toBe("/marketplace/new");
});
