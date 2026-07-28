# 투자 브리핑 의사결정 엔진

Java 21 + Spring Boot 3.5.7 예제입니다.

## 기능
- 시장 국면과 시장심리 분석
- AI CAPEX 투자 피로, 실적 신뢰도, 유동성, 뉴스 공포 점수 반영
- 종목별 최소 정기매수 금액 배수 계산
- 목표비중 초과·펀더멘털 훼손 종목 중단
- 감액분을 추가매수 대기 현금으로 적립
- 주말에는 다음 주 매수금액 확정
- 매월 25일 이후 목표비중 리밸런싱 검토
- GPT에 전달할 15개 항목 브리핑 프롬프트 생성

## 실행
```bash
gradle wrapper
gradlew.bat bootRun
```

## API
```bash
curl -X POST http://localhost:8080/api/investment/decision -H "Content-Type: application/json" --data-binary "@sample-request.json"
curl -X POST http://localhost:8080/api/investment/briefing-prompt -H "Content-Type: application/json" --data-binary "@sample-request.json"
```

## 엑셀에서 연결할 컬럼
계좌, 종목코드, 종목명, 최소 투자금액, 최대 투자금액, 목표비중, 현재비중, 손익률, 고점대비 하락률, 펀더멘털 점수, 밸류에이션 점수, 테마 위험 점수, 정기매수 여부.

## 운영 주기
- 매일: 행동신호 계산
- 매주 주말: 다음 주 정기매수 배수와 금액 확정
- 매월 25일 이후: 목표비중·섹터비중·편입종목 검토
