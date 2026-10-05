# FINBRIEF Next.js frontend

기존 `frontend`(React/Vite)와 병행하는 Next.js App Router 앱입니다.
기존 화면 컴포넌트와 스타일을 이 폴더로 복사하여 독립적으로 개발합니다.
원본 변경은 자동으로 반영되지 않으므로 이후 수정은 두 앱의 반영 범위를 확인합니다.

## 로컬 실행

Node.js 22 이상을 사용합니다. 저장소 루트에서:

```powershell
cd frontend-next
npm ci
npm run dev
```

접속: http://localhost:3001/dashboard
기본 API 대상은 V1 `http://127.0.0.1:8081`입니다.
V2에 연결하려면 실행 전 `$env:BACKEND_URL='http://127.0.0.1:8082'`를 설정합니다.
`BACKEND_URL`은 서버 전용이며 브라우저에는 노출하지 않습니다.

```powershell
npm run typecheck
npm run test:features
npm run build
npm run test:proxy
npm start
```

## Docker 병행 실행

기존 V1 백엔드와 `investment_default` 네트워크가 실행 중이어야 합니다.
저장소 루트에서:

```powershell
docker compose -f docker-compose.frontend-next.yml build frontend-next
docker compose -f docker-compose.frontend-next.yml up -d frontend-next
```

접속: http://localhost:4175/dashboard
기존 V1 4173, V2 4174와 별개이며 기존 서비스를 재생성하지 않습니다.
Docker에서는 서버 전용 `BACKEND_URL=http://investment-backend:8080`을 사용합니다.
중지할 때는 이 Compose 파일의 `stop frontend-next`를 사용합니다.

## 화면과 데이터 경로

메뉴별 URL: `/dashboard`, `/briefing`, `/holdings`, `/additional`, `/history`,
`/reference`, `/operations`, `/marketadmin`, `/bondyields`, `/volumescreen`, `/exchangerates`.
루트는 `/dashboard`로 이동하며 미등록 URL은 404를 반환합니다.

각 메뉴는 `src/app/(investment)/<메뉴>/page.tsx`로 정의합니다.
`(investment)`는 URL에 포함되지 않는 라우트 그룹이며, 그룹의 `layout.tsx`가
공통 `InvestmentShell`(사이드바·모바일 메뉴·헤더·알림)을 제공합니다.
페이지별 제목은 각 서버 페이지의 `metadata`에서 설정합니다.

- `src/features/<메뉴>`: 해당 메뉴의 조회·저장·상태·검증·표시를 함께 보관합니다.
- `src/shared`: 공통 HTTP 처리·타입·계좌 코드 Provider·표시 형식·UI·알림 Context.
- `src/lib/pages.ts`: 공통 메뉴 URL·표시명·아이콘.

각 기능 안에서는 다음 책임을 구분합니다. 서버 조회만 필요한 기능에는 브라우저 API나 훅을 억지로 만들지 않습니다.

| 위치 | 책임 |
| --- | --- |
| `app/(investment)/<메뉴>/page.tsx` | URL, metadata, 서버 초기 조회, 화면 구성 |
| `features/<메뉴>/server/queries.ts` | 서버 전용 읽기 API 호출 |
| `features/<메뉴>/api.ts` | 브라우저 조회·저장 URL, HTTP 메서드, 직렬화 |
| `features/<메뉴>/types.ts` | 메뉴의 응답·폼 타입 |
| `features/<메뉴>/model.ts` | 순수 변환, 표시용 집계, 입력 검증·정규화 |
| `features/<메뉴>/use*.ts` | 조회 수명주기, 로딩·오류, 필터·폼 상태, 저장 흐름 |
| `features/<메뉴>/*View.tsx` | JSX, 상태 표시, 사용자 이벤트 연결 |

예를 들어 보유종목은 다음 순서로 읽습니다.

```text
app/(investment)/holdings/page.tsx
  → features/holdings/HoldingsView.tsx
    → useHoldings.ts             목록·계좌 조회와 정렬·요약
    → useHoldingCreate.ts        신규 등록 폼·저장 흐름
    → useHoldingEditor.ts        일괄 편집 상태·검증·저장 흐름
      → model.ts                변경행 판단, 국내 매입금액 검증, PATCH payload
      → api.ts                  getHoldings / createHolding / updateAccountHoldings
      → types.ts                Holding / Draft / AdminAccount / AdminStock
```

`dashboard`, `briefing`, `additional`, `history`, `holdings`, `reference`, `operations`,
`marketadmin`, `bondyields`, `exchangerates`, `volumescreen`이 각각 독립 feature입니다.
정기매수 정규화·표시 규칙은 `operations/model.ts`, 환율 차트 계산은
`exchangerates/model.ts`에서 확인할 수 있습니다.

메뉴는 Next.js `Link`로 이동합니다. 레이아웃과 페이지 진입점은 서버 컴포넌트이며,
상태·이벤트가 필요한 본문은 `use client` 경계로 분리했습니다.
전체 앱의 `ssr: false`를 제거하여 초기 HTML에 메뉴와 각 화면의 초기 상태가 포함됩니다.
대시보드·브리핑·추가매수는 서버의 `queries.ts`에서 데이터를 조회해 초기 HTML에 반영합니다.
브리핑 본문은 서버 컴포넌트입니다. 서버 조회는 `BACKEND_URL`의 고정 origin을 사용하고,
요청의 Cookie·Authorization을 전달하며 `cache: no-store`와 15초 제한을 적용합니다.
조회 실패는 공통 `error.tsx`, 서버 조회 대기는 메뉴별 `loading.tsx`에서 처리합니다.
편집·기간 검색·차트 상세 조작은 각 메뉴의 클라이언트 훅에서 수행합니다.
채권·환율·거래량 검색의 초기 조회일은 서버에서 한국 시간으로 정해 props로 전달하므로
빌드 날짜가 고정되거나 서버·브라우저의 날짜가 달라지는 것을 방지합니다.
공통 갱신 완료 시 클라이언트 본문 재조회와 `router.refresh()`로 서버 초기 데이터도 갱신합니다.
보유종목의 저장된 계좌 탭은 마운트 후 복원하여 서버 렌더링 중 `localStorage`에 접근하지 않습니다.

브라우저의 `/api/**` 요청은 Next.js Route Handler를 거쳐 기존 Spring Boot로 전달됩니다.
쿼리, HTTP 메서드, 요청 본문, 백엔드 상태 코드와 응답을 전달하며 서버 캐시를 사용하지 않습니다.
백엔드 연결 실패는 502로 반환합니다. 데이터 수집·계산 로직은 백엔드에서 실행합니다.
등록·수정·삭제는 연결된 백엔드의 실제 데이터에 반영됩니다.

## 메뉴별 책임 분리 검증 (2026-10-05)

- `npm run test:features`: 국내·해외 매입금액 처리, 매수잠금, 입력 검증, 정기매수
  주기 정규화, 계좌별 표시·추천 데이터, 차트 경계값, API 계약과 화면 계층 분리 검증.
- `npm run build`: TypeScript 검사 및 11개 메뉴의 독립 라우트 생성.
- `npm run test:proxy`: 11개 메뉴 HTTP 200, 초기 HTML의 메뉴별 제목·링크·metadata,
  서버 조회 데이터, 요청별 데이터 갱신과 인증 헤더 전달, 루트 307 이동, 미등록 URL 404 검증.
- 같은 임시 서버에서 기존 API 프록시 회귀 검증. 저장 API 테스트는 임시 응답을 사용합니다.
- 실제 운영 데이터를 등록·수정·삭제하는 검증은 수행하지 않습니다.

## 초기 도입 검증 기록 (2026-10-05, 페이지 분리 이전)

- Windows와 Docker에서 Next.js 프로덕션 빌드 및 TypeScript 검사 통과.
- 메뉴 경로 11개 HTTP 200, 루트 `/dashboard` 이동, 미등록 경로 404 확인.
- V1 직접 호출과 Next.js 경유 호출의 계좌·대시보드·KOFIA 카탈로그 데이터 일치.
  요청별 `timestamp`, `traceId`는 비교에서 제외.
- 임시 백엔드로 JSON POST, multipart 파일 업로드, 쿼리, HEAD,
  422/204 상태 전달, 캐시 금지, 연결 실패 502 검증.
- 브라우저에서 대시보드·보유종목·브리핑 데이터, 메뉴 URL 이동,
  새로고침과 뒤로 가기 확인. 이 확인 과정에서 브라우저 오류 로그 없음.
- 기존 V1/V2 화면 4173/4174 정상 응답 확인.
- 실제 운영 데이터를 등록·수정·삭제하는 기능은 이번 확인에서 실행하지 않음.
