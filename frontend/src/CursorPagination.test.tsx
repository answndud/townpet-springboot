import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import CursorPagination from "./components/CursorPagination";
import { normalizeFeedPage } from "./api/client";

describe("CursorPagination", () => {
  it("hides the control row when there is only one page", () => {
    const { container } = render(<CursorPagination page={1} totalPages={1} hasNext={false} onPageChange={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("shows five nearby page numbers and first/last controls", () => {
    const onPageChange = vi.fn();
    render(<CursorPagination page={6} totalPages={12} hasNext onPageChange={onPageChange} />);

    expect(screen.getAllByRole("button").map((button) => button.textContent)).toEqual(["<<", "<", "4", "5", "6", "7", "8", ">", ">>"]);
    fireEvent.click(screen.getByRole("button", { name: "첫 페이지" }));
    fireEvent.click(screen.getByRole("button", { name: "마지막 페이지" }));
    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 12);
  });

  it("normalizes missing feed metadata without throwing", () => {
    expect(normalizeFeedPage(undefined)).toEqual({
      items: [],
      page: { nextCursor: null, hasNext: false, totalPages: 1 },
    });
    expect(normalizeFeedPage({ items: [], nextCursor: "legacy-cursor", hasNext: true })).toEqual({
      items: [],
      page: { nextCursor: "legacy-cursor", hasNext: true, totalPages: 2 },
    });
  });
});
