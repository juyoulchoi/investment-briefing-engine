# KOFIA 공통 수집 서비스

V1과 V2의 계좌·보유종목·브리핑·모델 DB는 각각 유지한다. KOFIA FreeSIS의 수집,
원본 응답, 정규화 데이터, 수집 조합, Job 이력만 별도 공통 서비스와 DB가 소유한다.
KRX·Yahoo·FRED 수집의 실행 위치는 이번 변경 범위에 포함하지 않는다.

```text
KOFIA FreeSIS
    ↓ 수집·정규화·재시도 (공통 서비스만 실행)
investment-kofia-collector :8083 (호스트 localhost 전용)
    ↓
investment-source-postgres / investment_source (호스트 포트 공개 없음)
    ↑ 공통 서비스의 GET API
    ├─ V1 :8081 /api/v1/kofia/**
    └─ V2 :8082 /api/v1/kofia/**
```

## 코드와 실행 경계

- KOFIA 구현의 기준 코드는 Main이다. `collectorBootJar`는 같은 소스에서 별도
  실행 JAR을 만든다. 수집기를 복제하지 않으며 V2에는 조회 어댑터만 추가한다.
- `KofiaCollectorApplication`은 KOFIA 컴포넌트와 공급자 호출 공통 코드만 스캔한다.
  계좌·보유종목·브리핑 계산기 및 해당 스케줄은 기동하지 않는다.
- 공통 DB는 독립된 `db/collector` Flyway 경로와 `flyway_collector_history`를 쓴다.
  Main/V2의 이미 적용된 migration을 변경하거나 전체 운영 DB를 복제하지 않는다.
- 기존 `TB_KOFIA_*` 원본·정규화·기준정보·Job 30개 테이블의 계약을 유지한다.
  `TB_CD_DTL`은 필요한 KOFIA 참조 코드만 초기화한다. 공급자 로그와 circuit 상태는
  공통 서비스에 별도로 저장한다. 모델 재계산 큐는 이 DB에 포함하지 않는다.
- V1/V2의 KOFIA GET 경로는 쿼리와 upstream 상태를 보존하여 공통 API로 전달한다.
  응답에 `X-Kofia-Source: shared-collector`를 붙인다. V2가 예전 코드이더라도
  공통 서비스의 신규 KOFIA 조회 경로를 사용할 수 있다.
- 소비자에서 수집·등록·재처리 요청은 HTTP 409를 반환한다. 수집 관리 POST는
  `http://localhost:8083/api/v1/kofia/...`에서 실행한다. 일반 공급자 재처리 API의
  KOFIA 경로도 차단하며 KRX/FRED 동작은 유지한다.
- 소비자는 스케줄·재시작 복구 Bean을 제거하고 로컬 KOFIA 서비스/저장소/실행기
  호출도 차단한다. 모델에서 KOFIA 입력을 추가할 때도 공통 조회 API를 사용해야 한다.
- 공통 API 장애는 503으로 반환한다. 과거 로컬 DB로 자동 fallback하지 않는다.
- 두 소비자는 `investment-source-read` 네트워크로 서비스에만 접근한다.
  공통 PostgreSQL은 별도 내부 네트워크에 있으며 소비자에 DB 계정을 제공하지 않는다.

## 최초 설치와 전환

1. Git에서 제외된 `.env.collector`에 `KOFIA_SOURCE_DB_PASSWORD`를 설정한다.
   `KOFIA_SHARED_COLLECTION_ENABLED=false`로 시작한다. 이 모드에서는 스케줄,
   자동 재개와 수동 POST가 모두 차단된다.
2. `docker compose --env-file .env.collector -f docker-compose.collector.yml up -d --build`
   후 `/api/v1/collector/status`, Flyway, KOFIA 조회 API를 확인한다.
3. 양쪽의 전체 테스트를 수행하고 기존 이미지 태그를 복구용으로 보관한다.
   V1/V2 기본 Compose는 소비자 모드와 공통 네트워크 연결을 포함한다.
   `docker compose up -d --build --no-deps backend`로 각 백엔드만 전환한다.
   전환 중 공통 DB 복사가 끝나기 전까지 KOFIA 조회 결과는 비어 있을 수 있다.
4. `./scripts/Copy-KofiaSourceStore.ps1`로 계약을 검증한 뒤 `-Apply`로 복사한다.
   스크립트는 두 기존 백엔드가 중지 또는 소비자 모드인지 확인하고, 공통 서비스가
   수집 비활성인지 확인한다. 대상이 비어 있지 않으면 중단하며 데이터를 지우지 않는다.
5. Main의 KOFIA 데이터만 custom-format dump로 복사한다. UUID, 원본 JSON,
   수집 시각, 해시, Job 상태와 sequence를 보존한다. 복구는 단일 트랜잭션이다.
   각 테이블의 건수와 전체 행 JSON의 두 부분 해시 합계를 대조한다.
   덤프·검증 보고서는 `backups/kofia-source`에 보존한다(`clean` 대상 밖).
6. 두 소비자와 공통 API의 결과를 대조한 뒤 `.env.collector`의
   `KOFIA_SHARED_COLLECTION_ENABLED=true`로 변경하고 공통 서비스만 재생성한다.
   기존 진행 중/대기 Job을 공통 DB에서 재개하며 이미 완료된 항목은 유지한다.
7. 정상 소규모 실제 수집을 확인하고 공통 DB에만 반영되는지 검증한다.

이번 전환의 원천 기준은 Main이다. V2의 신용공여 데이터 647개 기준일
(2024-01-02~2026-08-26)은 전환 전 Main의 671개 기준일 안에 모두 포함된 것을
확인했다. V2 로컬 원본과 V1 로컬 원본은 삭제하지 않아 과거 이력 비교가 가능하다.

## 운영

### 추가 데이터의 개발 위치와 실행 위치

- 추가 KOFIA Dataset, 조건 조합, 과거 백필, 누락일 재수집은 공통 수집 서비스에서
  실행하고 공통 DB에 저장한다. 수집기 코드는 Main에서 관리하지만 V1 백엔드에서
  실행하는 것을 의미하지 않는다. 관리 API는 `localhost:8083/api/v1/kofia/...`다.
- V2만 사용하는 원천 데이터도 재사용 가능한 공급자 원본이라면 공통 계층에 수집한다.
  V2 Factor/Metric, 이동 구간 통계, Robust-Z, 점수화, 백테스트는 V2에서 수행하고
  V2 DB에 저장한다. 모델 가공 결과를 공통 원본 데이터로 덮어쓰지 않는다.
- KRX/Yahoo/FRED 등의 기존 수집기는 아직 공통 서비스에 이전하지 않았다.
  해당 수집은 현재 구성된 실행 위치를 유지한다. 신규 공급자 수집기를 공통 서비스로
  추가할 때에는 클라이언트·스키마·조회 API·기존 수집 중지를 함께 구현하고 검증한다.

공통 수집 스케줄은 한국 시간 월~토 08:20이며, 일별 데이터는 기존 21일 중첩,
고객유형별규모는 기존 월말 정책을 유지한다. 운영 호출 예:

```powershell
docker compose --env-file .env.collector -f docker-compose.collector.yml ps
curl.exe -s http://localhost:8083/api/v1/collector/status
curl.exe -s http://localhost:8081/api/v1/kofia/datasets
curl.exe -s http://localhost:8082/api/v1/kofia/datasets
```

Main과 V2의 배포는 각각 자신의 경로에서 실행한다. 공통 서비스가 먼저 있어야
외부 네트워크가 존재한다. KOFIA 소비자 설정은 Main과 V2 기본 Compose에 통합되어 별도 override 파일이 필요하지 않다.
실제 수집 관리자 API는 호스트 loopback에만 공개한다. 인터넷에 직접 노출하지 않는다.

## 장애와 복구

- 공통 서비스만 장애: 소비자 조회는 503. DB 볼륨을 유지한 채 서비스만 복구한다.
- 이전 검증 실패: 공통 수집을 활성화하지 않는다. 기존 DB를 삭제하거나 덮어쓰지 않는다.
- 구형 서비스로 복귀해야 할 경우: 먼저 공통 수집을 중지하고, 전환 이후 신규 데이터와
  Job 진행 상황을 대조한다. 구형 이미지 태그와 당시 Compose 설정을 복구한다.
  공통 DB 활성화 후에는 단순히 이전 소비자 설정을 해제하면 안 된다. 최신 데이터가
  기존 DB에 없을 수 있으므로 먼저 역이전 범위와 단일 수집 주체를 정한다.
- `docker compose down -v`, 운영 DB 스키마 초기화 및 과거 Flyway checksum 변경은
  전환 절차에 포함하지 않는다.

KOFIA FreeSIS 웹사이트 내부 API를 사용하는 기존 수집 계약을 유지한다.
공식 인증형 OpenAPI로 전환한 것은 아니다.

## 2026-10-04 운영 전환 검증 결과

- 공통 서비스와 전용 PostgreSQL 신규 기동, V1/V2 백엔드 소비자 모드 재배포 완료.
- Main 테스트 133개 중 131개 통과/2개 skip, V2 93개 중 92개 통과/1개 skip.
  실패 0개. 두 버전의 조회 어댑터 소스와 테스트 파일 해시 일치.
- KOFIA 30개 테이블 1,865,722행 이전. 테이블별 건수와 전체 행의 양쪽 해시 합계
  30/30 일치. 원본 응답, 정규화 값, 조합과 Job 이력을 함께 검증했다.
- 신용공여 671행, 공통 원본 행 796,682행, 회사별 자금 흐름 668,290행,
  고객유형별 규모 113,878행 포함. 건수는 최초 이전 검증 시점 기준이다.
- 공통/V1/V2의 13개 KOFIA 조회 응답 전체 내용 일치. V1/V2 수집 POST는 409.
- 실제 신용공여 하루치 Job `1c0cc5bb-dd37-4385-bbf4-835dae6b450a`가
  `COMPLETED`, 성공 1개/실패 0개/저장 1행으로 완료.
- 실제 수집 후 `TB_KOFIA_RAW_RSP`: Main 3,750→3,750, V2 11→11,
  공통 DB 3,750→3,751. 새로 수집한 값도 V1/V2에서 공통 API와 동일하게 조회.
- 공통 수집·자동 재개 활성화, V1/V2 수집·자동 재개 비활성화 확인.
- V1/V2 계좌·대시보드 API 및 프런트엔드 4173/4174 모두 HTTP 200.
- 공통 서비스의 계좌 API는 404이며 운영 애플리케이션을 함께 기동하지 않음.
- 이전 백엔드 이미지 태그: `investment-backend:before-kofia-shared-20261004`,
  `investment-v2-backend:before-kofia-shared-20261004`.
- 덤프: `backups/kofia-source/kofia-20261004-213433.dump` (183,612,940 bytes).
  데이터 검증: `verification-20261004-213433.json`, 조회 검증: `api-verification.json`,
  실제 수집 검증: `live-collection-before.json`, `live-collection-after.json`,
  `live-collection-job.json` (모두 같은 백업 디렉터리).

자동 스케줄의 다음 예약 시각 실행은 아직 도래하지 않았다. 수동 실제 수집 경로와
운영 설정을 검증한 것이며 다음 예약 실행까지 관찰했다고 의미하지 않는다.
