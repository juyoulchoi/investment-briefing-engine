CREATE FUNCTION fn_tb_hold_upsert() RETURNS trigger AS $$
DECLARE
    v_mkt_cd VARCHAR(10);
BEGIN
    v_mkt_cd := CASE WHEN NEW.listing_scope = 'DOMESTIC' THEN 'KO' ELSE 'US' END;

    INSERT INTO "TB_STK"
        ("MKT_CD", "STK_CD", "STK_NM", "LIST_SCOPE", "ASSET_TP",
         "EXCH_NM", "CURR", "PRVDR", "ACTV_YN")
    VALUES
        (v_mkt_cd, NEW.stock_code, NEW.stock_name, NEW.listing_scope,
         NEW.asset_type, NEW.exchange_name, NEW.currency, NEW.provider,
         COALESCE(NEW.active_yn, 'Y'))
    ON CONFLICT ("MKT_CD", "STK_CD") DO UPDATE SET
        "STK_NM" = EXCLUDED."STK_NM",
        "LIST_SCOPE" = EXCLUDED."LIST_SCOPE",
        "ASSET_TP" = EXCLUDED."ASSET_TP",
        "EXCH_NM" = EXCLUDED."EXCH_NM",
        "CURR" = EXCLUDED."CURR",
        "PRVDR" = EXCLUDED."PRVDR",
        "ACTV_YN" = EXCLUDED."ACTV_YN",
        "MOD_DT" = CURRENT_TIMESTAMP;

    IF NEW.market_scope IS NOT NULL THEN
        INSERT INTO "TB_ACCT_STK"
            ("ACCT_TP", "MKT_CD", "STK_CD", "QTY", "AVG_PRC")
        VALUES
            (NEW.market_scope, v_mkt_cd, NEW.stock_code, NEW.qty, NEW.avg_prc)
        ON CONFLICT ("ACCT_TP", "MKT_CD", "STK_CD") DO UPDATE SET
            "QTY" = COALESCE(EXCLUDED."QTY", "TB_ACCT_STK"."QTY"),
            "AVG_PRC" = COALESCE(EXCLUDED."AVG_PRC", "TB_ACCT_STK"."AVG_PRC"),
            "MOD_DT" = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tb_hold_upsert
INSTEAD OF INSERT OR UPDATE ON tb_hold
FOR EACH ROW EXECUTE FUNCTION fn_tb_hold_upsert();

CREATE FUNCTION fn_tb_stk_prc_upsert() RETURNS trigger AS $$
DECLARE
    v_mkt_cd VARCHAR(10);
BEGIN
    v_mkt_cd := CASE WHEN NEW.market_scope = 'DOMESTIC' THEN 'KO' ELSE 'US' END;
    INSERT INTO "TB_STK_PRC"
        ("MKT_CD", "STK_CD", "TRD_DT", "OPEN_PRC", "HIGH_PRC", "LOW_PRC",
         "CLS_PRC", "ADJ_CLS", "VOL", "PRVDR")
    VALUES
        (v_mkt_cd, NEW.symbol, NEW.trading_day, NEW.open_price, NEW.high_price,
         NEW.low_price, NEW.close_price, NEW.adjusted_close, NEW.volume, NEW.provider)
    ON CONFLICT ("MKT_CD", "STK_CD", "TRD_DT") DO UPDATE SET
        "OPEN_PRC" = EXCLUDED."OPEN_PRC", "HIGH_PRC" = EXCLUDED."HIGH_PRC",
        "LOW_PRC" = EXCLUDED."LOW_PRC", "CLS_PRC" = EXCLUDED."CLS_PRC",
        "ADJ_CLS" = EXCLUDED."ADJ_CLS", "VOL" = EXCLUDED."VOL",
        "PRVDR" = EXCLUDED."PRVDR", "MOD_DT" = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tb_stk_prc_upsert
INSTEAD OF INSERT OR UPDATE ON tb_stk_prc
FOR EACH ROW EXECUTE FUNCTION fn_tb_stk_prc_upsert();
