# 단일 V1 운영 구성

2026-10-11 사용자 선택에 따라 V1을 유일한 실행 모델로 운영한다. V2 비교용 백엔드와 별도 수집기 프로세스는 종료하고, V2 코드 브랜치와 DB 이력은 보존한다.

## 실행 구성

`docker-compose.yml`의 `investment` 프로젝트가 다음 세 서비스를 관리한다.

| 서비스 | 컨테이너 | 역할 |
|---|---|---|
| postgres | investment-source-postgres | 기존 통합 PostgreSQL 저장소 |
| backend | investment-backend | V1 판단·브리핑·포트폴리오 API와 KRX/Yahoo/FRED/KOFIA/환율 수집 |
| frontend | investment-frontend | 기존 Next 프론트 이미지, 단일 백엔드 연결 |

백엔드 주소는 `http://localhost:8081`, 프론트 주소는 `http://localhost:4175`다. 이전 비교용 8082와 수집기 8083 포트는 사용하지 않는다. 프론트 이미지는 기존 검증된 이미지를 재사용하며 작업 중인 프론트 소스 변경은 이 전환에 포함하지 않는다.

`docker-compose.unified.yml`과 `docker-compose.collector.yml`은 별도 프로젝트를 만들지 않고 기본 Compose를 포함하는 호환 진입점이다. 세 파일을 함께 `-f`로 지정할 필요가 없다.

```powershell
docker compose up -d
docker compose ps
docker compose logs --tail 100 backend
```

## DB와 데이터 보존

DB는 기존 `investment_unified_live` 하나이며, 외부 볼륨 `investment-source-postgres-data`를 그대로 연결한다. 새 빈 DB로 초기화하지 않는다.

- `source`: 공통 원천 데이터와 수집 작업 이력.
- `portfolio`: V1 기준 실제 계좌·보유수량·현금.
- `v1`: 현재 운영하는 판단·추천·브리핑 결과와 포트폴리오 뷰.
- `v2`, `archive_v1`, `archive_v2`, `migration`: 기존 비교 결과, 원본 사본, 통합 매핑 이력. 삭제하지 않는다.

기존 V1/V2 PostgreSQL 컨테이너는 종료하지만 `backend_investment-postgres-data`와 `investment-v2-postgres-data` 볼륨은 복구용으로 보존한다. 운영 DB는 하나이고, 보존 볼륨은 운영에서 분리된 과거 사본이다. 스키마를 물리적으로 한곳으로 평탄화하면 기존 참조와 모델별 이력 구분이 깨질 수 있으므로 이번 전환에서 수행하지 않는다.

## 수집·권한·마이그레이션

단일 런타임 역할 `unified_backend`는 기존 `unified_v1`과 `unified_collector`의 권한을 상속한다. 원천 수집, 실제 포트폴리오, V1 결과를 처리할 수 있고 V2 모델·아카이브 스키마 접근 및 DDL 권한은 없다. DDL은 기존 `source_owner` Flyway 계정이 수행한다.

KOFIA/원천/환율의 원격 소비자 플래그는 끄고 로컬 수집을 활성화한다. 기존 KOFIA 스케줄과 복구 설정을 수집기에서 이관하며, 환율 08:00 Asia/Seoul 스케줄은 단일 백엔드에 등록한다. 원천 수정 감사·재처리 알림은 기존 통합 트리거를 유지한다.

`UNIFIED_SINGLE_BACKEND=true`일 때 `SingleBackendConfiguration`은 원천 Flyway(`source.flyway_collector_history`)를 먼저 실행하고 V1 Flyway(`v1.flyway_schema_history`)를 실행한다. 향후 원천 DDL도 별도 수집기 없이 적용된다.

기존 전환에서 생성한 비밀 설정을 기반으로 준비한다:

```powershell
./scripts/Prepare-SingleBackend.ps1
```

이 스크립트는 통합 DB가 존재하는 환경의 전환용이다. `backups/unified-db/runtime/v1.env`, `collector.env`와 현재 PostgreSQL 컨테이너 설정이 필요하다. 자격증명은 Git 제외 파일 `.env.single-backend`, `runtime/backend.env`, `runtime/postgres.env`에 저장한다. 이 파일들은 백업·복구 시 별도로 보관해야 한다.

권한 검증은 `scripts/Test-SingleBackendRole.sql`을 `source_owner`로 실행한다. 트랜잭션은 롤백하여 운영 입력을 남기지 않는다.

## 복구 자료

전체 통합 DB 백업은 `backups/unified-db/20261010-221518/unified-live.dump`이며, 전환 직전 포트폴리오와 V1/V2 모델 백업 및 검증 결과는 `backups/unified-db/single-backend-20261011`에 보관한다. 이전 V1/V2/수집기 이미지는 정리하며, 필요하면 Git 커밋 `46b6d57`(V1/수집기), `2afce5b`(V2)로 다시 빌드한다. 복구 시 수집·판단 프로세스를 동시에 중복 실행하지 않는다.
