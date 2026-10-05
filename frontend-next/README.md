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

`src/app/[page]/page.tsx`가 URL을 검증하고 기존 화면을 표시합니다.
`InvestmentClient`는 브라우저 저장소 초기화가 필요한 화면을 클라이언트에서 마운트합니다.
현재 화면은 클라이언트 렌더링이며 SSR로 데이터 화면을 전환하는 작업은 별도 단계입니다.
이후 화면별 파일로 분리할 수 있도록 기존 컴포넌트를 `src/components`에 보관합니다.

브라우저의 `/api/**` 요청은 Next.js Route Handler를 거쳐 기존 Spring Boot로 전달됩니다.
쿼리, HTTP 메서드, 요청 본문, 백엔드 상태 코드와 응답을 전달하며 서버 캐시를 사용하지 않습니다.
백엔드 연결 실패는 502로 반환합니다. 데이터 수집·계산 로직은 백엔드에서 실행합니다.
등록·수정·삭제는 연결된 백엔드의 실제 데이터에 반영됩니다.

## 검증 기록 (2026-10-05)

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
