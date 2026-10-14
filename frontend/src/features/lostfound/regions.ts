// Broad search centres for the portfolio's supported Seoul areas; never exact sighting coordinates.
export const SEARCH_REGIONS = [
  { label: "마포·서대문", latitude: 37.555, longitude: 126.915 },
  { label: "성동·광진", latitude: 37.545, longitude: 127.045 },
  { label: "송파·강동", latitude: 37.515, longitude: 127.10 },
  { label: "용산·중구", latitude: 37.54, longitude: 126.98 },
  { label: "강남·서초", latitude: 37.49, longitude: 127.03 },
] as const;
export function approximateArea(point: { latitude: number; longitude: number }) {
  const nearest = SEARCH_REGIONS.map(region => ({ region, distance: Math.hypot((point.latitude - region.latitude) * 111, (point.longitude - region.longitude) * 88) })).sort((a,b) => a.distance - b.distance)[0];
  return nearest && nearest.distance <= 7 ? `${nearest.region.label} 인근` : "근사 위치 공개";
}
