const DATE_TIME_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "numeric",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
});
const DATE_TIME_LONG_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});
const DATE_ONLY_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "numeric",
  day: "numeric",
});
const DATE_MEDIUM_TIME_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  dateStyle: "medium",
  timeStyle: "short",
});

export function formatDateTime(value: string) {
  return DATE_TIME_FORMATTER.format(new Date(value));
}

export function formatDateTimeLong(value: string) {
  return DATE_TIME_LONG_FORMATTER.format(new Date(value));
}

export function formatDateOnly(value: string) {
  return DATE_ONLY_FORMATTER.format(new Date(value));
}

export function formatDateMediumTime(value: string) {
  return DATE_MEDIUM_TIME_FORMATTER.format(new Date(value));
}
