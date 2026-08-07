-- 계산 원천데이터만 생성되고 AI 브리핑 문안이 생성되지 않은 빈 이력을 제거한다.
-- 발행되었거나 요약/본문/상세 항목/AI 생성 이력이 있는 브리핑은 보존한다.
DELETE FROM "TB_BRF" b
 WHERE b."BRF_STS" = 'READY'
   AND b."PUBL_YN" = 'N'
   AND b."AI_GEN_DTTM" IS NULL
   AND NULLIF(BTRIM(b."SUMMARY_TXT"), '') IS NULL
   AND NULLIF(BTRIM(b."BODY_TXT"), '') IS NULL
   AND NOT EXISTS (
       SELECT 1
         FROM "TB_BRF_DTL" d
        WHERE d."BRF_ID" = b."BRF_ID"
   );
