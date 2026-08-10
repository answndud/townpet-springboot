# Parity matrix와 differential testing 면접 노트

## inventory를 먼저 고정한 이유

리팩터링의 완료 기준은 새 코드의 단위 테스트 수가 아니라 기존 사용자가 관찰하는 page·API 여정의 coverage다. 그래서 legacy 파일을 기준으로 기계적인 count와 unique path 검증을 먼저 만든다. 누락이 발견되면 기능 구현 전에 build가 실패한다.

## normalize와 의미 비교

두 target의 raw 응답은 식별자·시간·서명 URL이 다를 수 있다. 이 값들을 `<uuid>`, `<timestamp>`, `<signed-url>`로 정규화하고 key 정렬 후 JSON을 비교하면 표현의 우연한 차이와 실제 business field 차이를 구분할 수 있다. trace ID 같은 진단 필드는 비교에서 제외하지만 title, status, permission 결과는 그대로 비교한다.

## 면접 답변 포인트

- matrix의 `pending`은 “아직 구현하지 않음”을 숨기지 않는 상태다.
- logical fixture는 legacy와 Spring이 같은 actor·clock·record를 사용하게 해 재현성을 높인다.
- route file count와 HTTP method count를 분리해 한 파일에 여러 mutation이 있어도 누락을 찾는다.
- normalize는 테스트 편의를 위한 무조건적 무시가 아니라, business 의미와 무관한 변동 필드의 allowlist다.
