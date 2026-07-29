DROP VIEW tb_brf_item;

ALTER TABLE "TB_BRF_ITEM"
    DROP COLUMN "ITEM_NM";

CREATE VIEW tb_brf_item AS
SELECT
    i."ITEM_CD" AS "item_cd",
    c."CD_NM" AS "item_nm",
    i."SORT_NO" AS "sort_no",
    i."ITEM_TP" AS "item_tp",
    i."USE_YN" AS "use_yn",
    i."ITEM_DESC" AS "item_desc",
    i."BRF_ITEM_ID" AS "brf_item_id",
    i."BRF_ID" AS "brf_id",
    i."ITEM_SUM" AS "item_sum",
    i."ITEM_CONT" AS "item_cont",
    i."SIG_CD" AS "signal_cd",
    i."ACT_YN" AS "act_yn",
    i."REG_DT" AS "reg_dt",
    i."MOD_DT" AS "mod_dt"
FROM "TB_BRF_ITEM" i
JOIN "TB_COM_CD" c
  ON c."CD_GRP" = i."ITEM_GRP"
 AND c."CD_KEY" = i."ITEM_CD";

COMMENT ON VIEW tb_brf_item
    IS '브리핑 항목명은 TB_COM_CD.BRIEFING_ITEM 그룹에서 조회하는 호환 뷰';
