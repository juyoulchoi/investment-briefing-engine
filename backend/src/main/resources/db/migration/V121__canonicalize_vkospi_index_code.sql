-- KRX drvprod_dd_trd 원본의 옵션지수|코스피 200 변동성지수를 내부 표준코드 VKOSPI로 통합한다.
DO $$
DECLARE
  old_code CONSTANT VARCHAR(30) := 'KRX_6d70ce7589b1ac26edf9ed814f';
BEGIN
  IF EXISTS (SELECT 1 FROM "TB_IDX" WHERE "IDX_CD"=old_code) THEN
    INSERT INTO "TB_IDX"(
      "IDX_CD","IDX_NM","IDX_NM_EN","IDX_TP","MKT_CD","CNTRY_CD","CURR_CD",
      "DATA_SRC_CD","SRC_SYMBOL","DFLT_YN","USE_YN","DEL_YN","CRT_USR_ID","UPD_USR_ID")
    SELECT 'VKOSPI','코스피 200 변동성지수',COALESCE("IDX_NM_EN",'KOSPI 200 Volatility Index'),
      "IDX_TP","MKT_CD","CNTRY_CD","CURR_CD",'KRX','옵션지수|코스피 200 변동성지수',
      "DFLT_YN",'Y','N','SYSTEM','SYSTEM'
    FROM "TB_IDX" WHERE "IDX_CD"=old_code
    ON CONFLICT ("IDX_CD") DO UPDATE SET
      "IDX_NM"='코스피 200 변동성지수',
      "IDX_NM_EN"=COALESCE("TB_IDX"."IDX_NM_EN",EXCLUDED."IDX_NM_EN"),
      "DATA_SRC_CD"='KRX',
      "SRC_SYMBOL"='옵션지수|코스피 200 변동성지수',
      "USE_YN"='Y',"DEL_YN"='N',"UPD_DTTM"=CURRENT_TIMESTAMP,"UPD_USR_ID"='SYSTEM';

    -- 이미 VKOSPI 날짜 데이터가 있으면 표준코드 행을 보존하고 해시코드 중복을 제거한다.
    DELETE FROM "TB_IDX_DAY" old
    USING "TB_IDX_DAY" canonical
    WHERE old."IDX_CD"=old_code AND canonical."IDX_CD"='VKOSPI'
      AND old."TRADE_DT"=canonical."TRADE_DT";

    UPDATE "TB_IDX_DAY" SET "IDX_CD"='VKOSPI' WHERE "IDX_CD"=old_code;
    UPDATE "TB_STK" SET "BASE_IDX_CD"='VKOSPI' WHERE "BASE_IDX_CD"=old_code;
    UPDATE "TB_MKT_SNAP" SET "MAIN_IDX_CD"='VKOSPI' WHERE "MAIN_IDX_CD"=old_code;
    UPDATE "TB_MKT_SNAP" SET "SUB_IDX_CD"='VKOSPI' WHERE "SUB_IDX_CD"=old_code;
    DELETE FROM "TB_IDX" WHERE "IDX_CD"=old_code;
  END IF;
END $$;

COMMENT ON COLUMN "TB_IDX"."SRC_SYMBOL" IS
  '공급자 원천 식별자. KRX 지수는 IDX_CLSS|IDX_NM 원본 ROW_KEY를 저장';
