# 현재 운영 구성

2026-10-11부터 V1 단일 백엔드로 전환했다. 현재 실행 명령·컨테이너·권한은 [single-backend-operation.md](single-backend-operation.md)를 따른다. 아래의 V1/V2/수집기 분리 실행 설명은 이전 통합 검증과 복구를 위한 기록이다.

# V1/V2 공통 DB 전환

## 운영 전환 완료: 2026-10-11

V1(8081), V2(8082), 공통 수집기(8083)는 모두 `investment-source-postgres`의
`investment_unified_live` DB를 사용한다. 기존 프론트(4175)는 그대로 유지한다.
실제 계좌·보유수량·평균단가·현금은 V1 기준이다.
운영 Compose는 **`docker-compose.unified.yml`**이다.

```powershell
docker compose -f docker-compose.unified.yml up -d --no-deps
docker compose -f docker-compose.unified.yml ps
```

기존 Main/V2/collector Compose로 backend/collector를 재생성하면 이전 DB 연결로
되돌아가므로 앞으로 해당 세 서비스는 통합 Compose로 관리한다.
런타임 환경은 `.env.unified` 및 `backups/unified-db/runtime/*.env`에서 생성하며
Git에서 제외한다. 비밀번호를 문서나 출력에 복사하지 않는다.

기존 원본 DB 세 개와 볼륨은 보존했다. 이전 앱 컨테이너는 정지 상태로
`investment-backend-pre-unified`, `investment-v2-backend-pre-unified`,
`investment-kofia-collector-pre-unified`라는 이름으로 보존했다.
원본 DB 컨테이너는 기존 이름으로 계속 남아 있으며 운영 앱은 연결하지 않는다.

검증 기록: `backups/unified-db/20261010-221518/api-checks-live.json`.
최초 dump의 SHA256는 `manifest.json`, 쓰기 중지 후 최종 dump는
`final-v1.dump`, `final-v2.dump`, `final-source.dump`다.
최종 원본 백업의 크기/SHA256는 `manifest-final-originals.json`에 기록한다.
통합 운영 DB 체크포인트는 `unified-live.dump`로 별도 보관한다.
백업 이후 새 V1 주간 브리핑이 생긴 것을 전체 행 비교로 발견하여
`Sync-UnifiedFinalV1Models.ps1`로 최신 설정/브리핑/상세와 생성키 시퀀스를 반영했다.
V1의 31개, V2의 34개 모델/설정 테이블은 업무키 ID 매핑을 반영한 전체 행 해시로
원본과 동등함을 확인했다. 포트폴리오 다섯 테이블도 원본 V1 전체 행과 일치했다.
공통 KOFIA 원천/카탈로그 22개 테이블도 최종 원본 Source와 행 수 및 전체 행
체크섬이 일치했다(`source-kofia-final-check.txt`).

운영 HTTP 검증: 국내 72개, 해외 36개, S&P 500 7일, AAPL 7일,
금리 24개 응답 전체가 V1/V2에서 일치한다. 보유 입력 108개도 일치한다.
V2의 하루치 지수 수집 호출이 공통 수집기로 전달되어 1건 저장했고,
기존 화면 대시보드 HTTP 200과 프록시 보유 조회 108건을 확인했다.
Flyway source V4, V1 V153, V2 V147 및 모든 FK 검증을 확인했다.
SQL 입력 공유/모델 쓰기 격리/역할 경계/보정 알림 테스트는 트랜잭션 롤백으로 검증했다.
후보 JPA 보유 생성키 111의 생성과 정기매수 생성도 성공했으며,
검증 행 제거 후 전체 보유와 모델별 비중이 검증 전 값과 일치함을 확인했다.

목표비중·추천금액·판단·예측·브리핑은 V1/V2의 모델 결과이므로 각각 유지한다.
V2의 과거 8월 판단/브리핑은 이력을 보존한 것이다. 공통 DB 전환이 과거 판단을
현재 시점 결과로 재작성하거나 과거 브리핑 본문의 현금을 수정하지 않는다.
새 결과 생성은 각 모델의 계산/발행 경로를 사용한다.

### 복구

전환 직후 원본으로 돌아갈 때는 새 앱 세 개를 먼저 정지하고 이름을 별도 보존한 뒤,
`*-pre-unified` 컨테이너를 기존 이름으로 되돌려 시작한다. 프론트/DB/볼륨은 건드리지 않는다.
전환 이후 새 거래나 입력이 발생했다면 통합 DB를 먼저 백업하고 변경분을 대조해야 한다.
원본을 단순 재시작하면 새 입력이 빠질 수 있으므로 먼저 데이터를 조정한다.
통합 DB 복구는 dump를 새 DB에 복원한 뒤, 세 런타임의 JDBC DB명을 함께 바꾸어 수행한다.

사용자 승인: 공통 원천과 실제 포트폴리오는 하나의 DB를 기준으로 통일한다.
실제 계좌·보유·현금의 기준은 V1이다. V2 원본 및 모델 결과 이력은 보존한다.

## 데이터 경계

- `source`: 공통 KOFIA/환율, 종목/지수 마스터, 가격/지수/금리, KRX/FRED 수집과 투자자 수급 원천.
- `portfolio`: V1 기준 실제 계좌·보유·정기매수 설정·현금 이력/예약.
- `v1`, `v2`: 각각의 스냅샷·심리·투자 판단·종목 판단·예측·추천·브리핑·재계산 결과 및 Flyway 이력.
- `archive_v1`, `archive_v2`: 이관으로 대체된 원본 테이블. 삭제하지 않는다.
- `migration`: 종목 ID 업무키 매핑과 이관 감사 정보.

`TB_HOLD`와 `TB_REG_BUY`는 입력과 계산 필드가 혼합돼 있으므로 단순 테이블 공유를 금지한다.
각 모델 스키마의 호환 뷰는 공통 포트폴리오와 모델별 `_MODEL` 테이블을 결합한다.
뷰 쓰기는 트리거를 통해 실제 입력과 모델 계산 필드를 각각 저장한다.
보유 목표비중/비중차이/비중상태/위험등급, 정기매수 배수/추천/계산일은 모델별로 분리한다.
조회 결과, JPA 생성키/수정, 직접 SQL 수정, 거래/현금 FK 계약은 후보 DB에서 확인해야 한다.

원천 보정 이력 `TB_DATA_CORR_HIS`도 source에 통합했다. V2 기존 보정 ID는
`migration.correction_id_map`으로 재연결하고 원본 source key와 출처를 보존했다.
원천 정정 트리거는 변경 사실을 한 번 기록하고 각 모델의 재계산 큐에 네 종류의
알림을 각각 넣는다. SECURITY DEFINER 함수의 search_path는 pg_catalog,source로 고정하며
함수 자체는 모델 계산을 수행하지 않는다.

원천 수집은 독립 수집기가 소유한다. V1/V2의 KRX/FRED/Yahoo 수집 호출은
허용한 서비스와 메서드 서명을 사용해 내부 RPC로 전달한다.
공통 종목/지수 마스터 관리와 V2 Excel 수급 import는 명시적으로 허용한 입력 경로다.
앱 역할은 자기 모델+공통 포트폴리오를 수정할 수 있지만 공통 가격 및 다른 모델은
수정할 수 없다. 수집 역할은 원천/평가금액 갱신을 할 수 있지만 실제 보유수량과
모델 판단은 수정할 수 없다. Flyway DDL 역할은 런타임 역할과 분리했다.

## 준비 및 검증

`scripts/Prepare-UnifiedDatabase.ps1`는 운영 DB를 변경하지 않고 세 환경을 custom dump로 백업한다.
각 dump의 SHA256와 크기를 manifest에 저장하고 새로운 `investment_unified_candidate` DB에 복원한다.
각 dump의 독립적인 트랜잭션 스냅샷을 사용하므로 운영 전환 시에는 쓰기 중지 후
최종 백업과 변경분 반영 및 동등성 검증이 필요하다.
기존 후보가 있으면 실패하며 덮어쓰지 않는다.

`scripts/Audit-UnifiedDatabase.sql`은 복원 후 ID/업무키/실제 수량/현금/FK/Flyway 차이를 확인한다.
`scripts/Unify-CandidateDatabase.sql`은 후보 DB에서만 실행 가능한 트랜잭션이다.
V1 우선 원천에 V2의 누락 업무키를 추가하며, 기존 공통 KOFIA/환율 원천은 그대로 사용한다.
서로 다른 기존 원천 값은 archive와 원본 dump에 보존한다.
V2 결과의 종목 FK는 시장/종목코드 업무키로 매핑 후 공통 마스터를 참조하도록 검증한다.

통합 후 `scripts/Align-UnifiedSourceColumns.sql`, `scripts/Rebind-UnifiedLegacyViews.sql`을
순서대로 반드시 실행한다. V2 전용 수집 재시도 메타데이터 컬럼은 공통 원천의 nullable
추가 컬럼으로 보존한다. 기존 소문자 호환 뷰는
테이블을 스키마로 이동해도 과거 테이블 참조를 유지하므로 공통 원천/포트폴리오와
모델별 호환 뷰로 다시 연결해야 한다. 이를 누락하면 일부 종목 API만 과거 데이터를 읽는다.

## 운영 전환 조건

1. 모든 원본 dump의 복원 성공과 외래키 검증.
2. 실제 포트폴리오 V1 동등성, V2 전체 입력의 공통 조회, 모델 계산 쓰기 격리.
3. 각 버전의 실제 애플리케이션으로 조회/JPA/SQL/거래 계약 검증.
4. 공통 수집기가 모든 원천 수집을 소유하고 소비 환경의 중복 수집을 차단.
5. 각 모델 DB 역할은 다른 모델 스키마 쓰기 금지. 원천 쓰기는 수집 역할만 허용.
6. Flyway를 source/portfolio/model 전용으로 분리한 새로운 migration 경로 확정.
7. 쓰기 중지 후 최종 백업/변경분 반영, 서비스 연결 변경, HTTP/DB 검증 및 롤백 절차 확인.

`application-unified.yml`은 후보 검증용 opt-in 프로파일이다. 기존 Flyway를 새 공유 뷰에
무조건 적용하지 않는다. 운영 런타임은 검증된 스키마/경로/별도 DDL 역할을 명시한다.
기존 운영 Compose와 연결은 검증 완료 후 위 운영 전환 절차로 대체했다.
완료 판정은 백업/통합 SQL 작성이나 후보 복원만으로 하지 않는다.
