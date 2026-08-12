import { expect, test } from "@playwright/test";

const MEMBER_EMAIL = "demo-member-1@townpet.local";
const MEMBER_PASSWORD = "townpet-demo-123!";
const MODERATOR_EMAIL = "demo-moderator@townpet.local";
const MODERATOR_PASSWORD = "townpet-moderator-123!";

async function login(page: import("@playwright/test").Page, email: string, password: string, next: string) {
  await page.goto(`/login?next=${encodeURIComponent(next)}`);
  await page.getByLabel("이메일").fill(email);
  await page.getByLabel("비밀번호", { exact: true }).fill(password);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect.poll(() => new URL(page.url()).pathname).toBe(next);
}

test.describe("desktop domain write error journeys", () => {
  test("shows the marketplace conflict without losing the form", async ({ page }) => {
    let called = false;
    await page.route("**/api/v1/marketplace/listings**", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 409, contentType: "application/problem+json", body: JSON.stringify({ detail: "conflict" }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/marketplace/new");
    await page.getByLabel("제목").fill("충돌 검증 거래 글");
    await page.getByLabel("설명").fill("서버 충돌 시 작성 내용이 유지되어야 합니다.");
    await page.getByLabel("가격(원)").fill("10000");
    await page.getByRole("button", { name: "등록" }).click();
    await expect(page.getByRole("alert")).toHaveText("예약 이후에는 거래 조건을 수정할 수 없습니다.");
    await expect(page.getByLabel("제목")).toHaveValue("충돌 검증 거래 글");
    expect(called).toBe(true);
  });

  test("shows a lost-found validation error and preserves the coordinates", async ({ page }) => {
    let called = false;
    await page.route("**/api/v1/lost-found/alerts**", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 422, contentType: "application/problem+json", body: JSON.stringify({ detail: "invalid coordinates" }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/lost-found/new");
    await page.getByLabel("제목").fill("좌표 검증 분실 제보");
    await page.getByLabel("설명").fill("좌표 오류 응답을 확인합니다.");
    await page.getByLabel("위도").fill("37.550");
    await page.getByLabel("경도").fill("126.910");
    await page.getByRole("button", { name: "제보 등록" }).click();
    await expect(page.getByRole("alert")).toHaveText("제목, 설명과 위치를 확인해 주세요.");
    await expect(page.getByLabel("위도")).toHaveValue("37.550");
    expect(called).toBe(true);
  });

  test("creates a lost-found alert and opens its detail route", async ({ page }) => {
    const alertId = "00000000-0000-4000-8000-000000009101";
    let called = false;
    await page.route("**/api/v1/lost-found/alerts**", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ id: alertId }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/lost-found/new");
    await page.getByLabel("제목").fill("정상 생성 분실 제보");
    await page.getByLabel("설명").fill("정상 write 여정을 확인합니다.");
    await page.getByLabel("위도").fill("37.550");
    await page.getByLabel("경도").fill("126.910");
    await page.getByRole("button", { name: "제보 등록" }).click();
    await expect.poll(() => new URL(page.url()).pathname).toBe(`/lost-found/${alertId}`);
    expect(called).toBe(true);
  });

  test("shows a care request error after the server rejects the write", async ({ page }) => {
    let called = false;
    await page.route("**/api/v1/care/requests", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 422, contentType: "application/problem+json", body: JSON.stringify({ detail: "invalid request" }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/care/new");
    await page.getByLabel("제목").fill("돌봄 검증 요청");
    await page.getByLabel("설명").fill("서버 validation 실패를 확인합니다.");
    await page.getByLabel("장소").fill("서울 마포구");
    await page.getByLabel("시작 시각").fill("2026-08-20T10:00");
    await page.getByLabel("종료 시각").fill("2026-08-20T12:00");
    await page.getByRole("button", { name: "등록" }).click();
    await expect(page.getByRole("alert")).toHaveText("돌봄 요청 내용을 확인해 주세요.");
    await expect(page.getByLabel("제목")).toHaveValue("돌봄 검증 요청");
    expect(called).toBe(true);
  });

  test("creates a care request and returns to the list", async ({ page }) => {
    let called = false;
    await page.route("**/api/v1/care/requests", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ id: "00000000-0000-4000-8000-000000009102" }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/care/new");
    await page.getByLabel("제목").fill("정상 생성 돌봄 요청");
    await page.getByLabel("설명").fill("정상 write 여정을 확인합니다.");
    await page.getByLabel("장소").fill("서울 마포구");
    await page.getByLabel("시작 시각").fill("2026-08-20T10:00");
    await page.getByLabel("종료 시각").fill("2026-08-20T12:00");
    await page.getByRole("button", { name: "등록" }).click();
    await expect.poll(() => new URL(page.url()).pathname).toBe("/care");
    expect(called).toBe(true);
  });

  test("shows a gathering creation error after the server rejects the write", async ({ page }) => {
    let called = false;
    await page.route("**/api/v1/gatherings", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 422, contentType: "application/problem+json", body: JSON.stringify({ detail: "invalid gathering" }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/gatherings/new");
    await page.getByLabel("제목").fill("모임 검증 생성");
    await page.getByLabel("설명").fill("서버 validation 실패를 확인합니다.");
    await page.getByLabel("장소").fill("망원 한강공원");
    await page.getByLabel("일시").fill("2026-08-20T10:00");
    await page.getByLabel("정원").fill("8");
    await page.getByRole("button", { name: "모임 만들기" }).click();
    await expect(page.getByRole("alert")).toHaveText("모임을 만들지 못했습니다.");
    await expect(page.getByLabel("제목")).toHaveValue("모임 검증 생성");
    expect(called).toBe(true);
  });

  test("creates a gathering and opens its detail route", async ({ page }) => {
    const gatheringId = "00000000-0000-4000-8000-000000009103";
    let called = false;
    await page.route("**/api/v1/gatherings", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      called = true;
      await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ id: gatheringId }) });
    });
    await login(page, MEMBER_EMAIL, MEMBER_PASSWORD, "/gatherings/new");
    await page.getByLabel("제목").fill("정상 생성 산책 모임");
    await page.getByLabel("설명").fill("정상 write 여정을 확인합니다.");
    await page.getByLabel("장소").fill("망원 한강공원");
    await page.getByLabel("일시").fill("2026-08-20T10:00");
    await page.getByLabel("정원").fill("8");
    await page.getByRole("button", { name: "모임 만들기" }).click();
    await expect.poll(() => new URL(page.url()).pathname).toBe(`/gatherings/${gatheringId}`);
    expect(called).toBe(true);
  });

  test("shows moderator member-action errors on the direct queue", async ({ page }) => {
    let called = false;
    await page.route("**/api/admin/moderation/users/sanction", async (route) => {
      called = true;
      await route.fulfill({ status: 404, contentType: "application/problem+json", body: JSON.stringify({ detail: "member not found" }) });
    });
    await login(page, MODERATOR_EMAIL, MODERATOR_PASSWORD, "/admin/moderation/direct");
    await page.getByLabel("회원 ID").fill("00000000-0000-4000-8000-000000009999");
    await page.getByLabel("사유").fill("오류 경계 확인");
    await page.getByRole("button", { name: "계정 제재" }).click();
    await expect(page.getByRole("alert")).toHaveText("회원을 찾을 수 없습니다.");
    expect(called).toBe(true);
  });

  test("announces a successful moderator member action", async ({ page }) => {
    let called = false;
    await page.route("**/api/admin/moderation/users/sanction", async (route) => {
      called = true;
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ memberId: "00000000-0000-4000-8000-000000000202", action: "sanction", affectedPublications: 0 }) });
    });
    await login(page, MODERATOR_EMAIL, MODERATOR_PASSWORD, "/admin/moderation/direct");
    await page.getByLabel("회원 ID").fill("00000000-0000-4000-8000-000000000202");
    await page.getByLabel("사유").fill("정상 조치 여정 확인");
    await page.getByRole("button", { name: "계정 제재" }).click();
    await expect(page.getByRole("status")).toHaveText("회원 운영 조치를 완료했습니다.");
    expect(called).toBe(true);
  });
});
