import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import CursorPagination from "./components/CursorPagination";
import { normalizeFeedPage } from "./api/client";

describe("CursorPagination", () => {
  it("hides the control row when there is only one page", () => {
    const { container } = render(<CursorPagination page={1} hasPrevious={false} hasNext={false} onPageChange={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("moves through the cursor history", () => {
    const onPageChange = vi.fn();
    render(<CursorPagination page={2} hasPrevious hasNext onPageChange={onPageChange} />);

    expect(screen.getByText("2페이지")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "이전 페이지" }));
    fireEvent.click(screen.getByRole("button", { name: "다음 페이지" }));
    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 3);
  });

  it("normalizes missing feed metadata without throwing", () => {
    expect(normalizeFeedPage(undefined)).toEqual({
      items: [],
      page: { nextCursor: null, hasNext: false },
    });
    expect(normalizeFeedPage({ items: [], nextCursor: "legacy-cursor", hasNext: true })).toEqual({
      items: [],
      page: { nextCursor: "legacy-cursor", hasNext: true },
    });
  });
});
