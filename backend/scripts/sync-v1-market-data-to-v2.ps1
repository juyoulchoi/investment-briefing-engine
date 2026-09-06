[CmdletBinding()]
param(
    [datetime]$FromDate = [datetime]::ParseExact('2024-01-01', 'yyyy-MM-dd', $null),
    [datetime]$ToDate = [datetime]::Today,
    [switch]$Apply,
    [string]$SourceHost = 'host.docker.internal',
    [int]$SourcePort = 5432,
    [string]$SourceDatabase = 'investment',
    [string]$SourceUser = 'investment',
    [string]$SourcePassword = 'investment',
    [string]$SourceContainer = 'investment-postgres',
    [string]$TargetContainer = 'investment-v2-postgres',
    [string]$TargetDatabase = 'investment',
    [string]$TargetUser = 'investment'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($FromDate.Date -gt $ToDate.Date) {
    throw 'FromDate must be less than or equal to ToDate.'
}

if ($TargetContainer -ne 'investment-v2-postgres') {
    throw "Refusing to write to unexpected target container: $TargetContainer"
}

if ($SourceContainer -ne 'investment-postgres') {
    throw "Refusing to read from unexpected source container: $SourceContainer"
}

$from = $FromDate.ToString('yyyy-MM-dd')
$to = $ToDate.ToString('yyyy-MM-dd')
$sourceConnection = "host=$SourceHost port=$SourcePort dbname=$SourceDatabase user=$SourceUser password=$SourcePassword"

function Invoke-TargetSql {
    param(
        [Parameter(Mandatory)]
        [string]$Sql,
        [switch]$TuplesOnly
    )

    $arguments = @('exec', '-i', $TargetContainer, 'psql', '-v', 'ON_ERROR_STOP=1')
    if ($TuplesOnly) {
        $arguments += @('-At')
    }
    $arguments += @('-U', $TargetUser, '-d', $TargetDatabase, '-c', $Sql)
    $result = & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Target SQL failed with exit code $LASTEXITCODE."
    }
    return $result
}

function Invoke-SourceSql {
    param(
        [Parameter(Mandatory)]
        [string]$Sql
    )

    $result = & docker exec $SourceContainer psql -v ON_ERROR_STOP=1 -At -U $SourceUser -d $SourceDatabase -c $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Source SQL failed with exit code $LASTEXITCODE."
    }
    return $result
}

$containerState = (& docker inspect --format '{{.State.Running}}' $TargetContainer 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or $containerState -ne 'true') {
    throw "Target container is not running: $TargetContainer"
}

$sourceCountsSql = @"
SELECT 'TB_IDX|' || count(*) FROM "TB_IDX";
SELECT 'TB_IDX_DAY|' || count(*) FROM "TB_IDX_DAY" WHERE "TRADE_DT" BETWEEN '$from' AND '$to';
SELECT 'TB_EXCH_DAY|' || count(*) FROM "TB_EXCH_DAY" WHERE "BASE_DT" BETWEEN '$from' AND '$to';
"@

Write-Host "V1 -> V2 market-data synchronization ($from through $to)"
Write-Host "Target: $TargetContainer/$TargetDatabase"
Write-Host 'Source rows:'
Invoke-SourceSql -Sql $sourceCountsSql | ForEach-Object { Write-Host "  $_" }

if (-not $Apply) {
    Write-Host 'DRY-RUN complete. No market data was changed. Use -Apply to insert missing rows into V2.'
    exit 0
}

$syncSql = @"
BEGIN;

CREATE EXTENSION IF NOT EXISTS dblink;

INSERT INTO "TB_IDX" (
    "IDX_CD", "IDX_NM", "IDX_NM_EN", "IDX_TP", "MKT_CD", "CNTRY_CD", "CURR_CD",
    "DATA_SRC_CD", "SRC_SYMBOL", "DFLT_YN", "USE_YN", "DEL_YN",
    "CRT_DTTM", "CRT_USR_ID", "UPD_DTTM", "UPD_USR_ID"
)
SELECT *
FROM dblink('$sourceConnection',
    'SELECT "IDX_CD", "IDX_NM", "IDX_NM_EN", "IDX_TP", "MKT_CD", "CNTRY_CD", "CURR_CD", "DATA_SRC_CD", "SRC_SYMBOL", "DFLT_YN", "USE_YN", "DEL_YN", "CRT_DTTM", "CRT_USR_ID", "UPD_DTTM", "UPD_USR_ID" FROM "TB_IDX"')
AS s(
    "IDX_CD" varchar(30), "IDX_NM" varchar(150), "IDX_NM_EN" varchar(200), "IDX_TP" varchar(30),
    "MKT_CD" varchar(30), "CNTRY_CD" varchar(10), "CURR_CD" varchar(10), "DATA_SRC_CD" varchar(30),
    "SRC_SYMBOL" varchar(50), "DFLT_YN" char(1), "USE_YN" char(1), "DEL_YN" char(1),
    "CRT_DTTM" timestamptz, "CRT_USR_ID" varchar(50), "UPD_DTTM" timestamptz, "UPD_USR_ID" varchar(50)
)
ON CONFLICT ("IDX_CD") DO NOTHING;

INSERT INTO "TB_IDX_DAY" (
    "TRADE_DT", "IND_CD", "IND_NM", "CLS_VAL", "CHG_VAL", "CHG_RT", "SRC_NM", "COLLECT_DTTM",
    "OPEN_VAL", "HIGH_VAL", "LOW_VAL", "TRD_VOL", "TRD_VAL", "MKT_CAP", "PREV_CLS_VAL",
    "HIGH_52W_VAL", "DD_52W_RT", "ALL_HIGH_VAL", "DD_HIGH_RT", "DATA_SRC_CD", "DATA_STS", "IDX_CD"
)
SELECT *
FROM dblink('$sourceConnection',
    'SELECT "TRADE_DT", "IND_CD", "IND_NM", "CLS_VAL", "CHG_VAL", "CHG_RT", "SRC_NM", "COLLECT_DTTM", "OPEN_VAL", "HIGH_VAL", "LOW_VAL", "TRD_VOL", "TRD_VAL", "MKT_CAP", "PREV_CLS_VAL", "HIGH_52W_VAL", "DD_52W_RT", "ALL_HIGH_VAL", "DD_HIGH_RT", "DATA_SRC_CD", "DATA_STS", "IDX_CD" FROM "TB_IDX_DAY" WHERE "TRADE_DT" BETWEEN ''$from'' AND ''$to''')
AS s(
    "TRADE_DT" date, "IND_CD" varchar(50), "IND_NM" varchar(100), "CLS_VAL" numeric(20,6),
    "CHG_VAL" numeric(20,6), "CHG_RT" numeric(10,4), "SRC_NM" varchar(100), "COLLECT_DTTM" timestamptz,
    "OPEN_VAL" numeric(20,6), "HIGH_VAL" numeric(20,6), "LOW_VAL" numeric(20,6), "TRD_VOL" numeric(20,2),
    "TRD_VAL" numeric(20,2), "MKT_CAP" numeric(20,2), "PREV_CLS_VAL" numeric(20,6),
    "HIGH_52W_VAL" numeric(20,6), "DD_52W_RT" numeric(10,4), "ALL_HIGH_VAL" numeric(20,6),
    "DD_HIGH_RT" numeric(10,4), "DATA_SRC_CD" varchar(30), "DATA_STS" varchar(20), "IDX_CD" varchar(30)
)
ON CONFLICT ("IDX_CD", "TRADE_DT") DO NOTHING;

INSERT INTO "TB_EXCH_DAY" (
    "BASE_DT", "BASE_CURR_CD", "QUOTE_CURR_CD", "EXCH_RT", "PREV_EXCH_RT", "CHG_AMT", "CHG_RT",
    "HIGH_52W_RT", "LOW_52W_RT", "PRESS_SCR", "DATA_SRC_CD", "DATA_STS", "COLLECT_DTTM"
)
SELECT *
FROM dblink('$sourceConnection',
    'SELECT "BASE_DT", "BASE_CURR_CD", "QUOTE_CURR_CD", "EXCH_RT", "PREV_EXCH_RT", "CHG_AMT", "CHG_RT", "HIGH_52W_RT", "LOW_52W_RT", "PRESS_SCR", "DATA_SRC_CD", "DATA_STS", "COLLECT_DTTM" FROM "TB_EXCH_DAY" WHERE "BASE_DT" BETWEEN ''$from'' AND ''$to''')
AS s(
    "BASE_DT" date, "BASE_CURR_CD" varchar(10), "QUOTE_CURR_CD" varchar(10), "EXCH_RT" numeric(20,6),
    "PREV_EXCH_RT" numeric(20,6), "CHG_AMT" numeric(20,6), "CHG_RT" numeric(10,4),
    "HIGH_52W_RT" numeric(20,6), "LOW_52W_RT" numeric(20,6), "PRESS_SCR" numeric(10,4),
    "DATA_SRC_CD" varchar(30), "DATA_STS" varchar(20), "COLLECT_DTTM" timestamptz
)
ON CONFLICT ("BASE_DT", "BASE_CURR_CD", "QUOTE_CURR_CD") DO NOTHING;

COMMIT;
"@

Invoke-TargetSql -Sql $syncSql | Out-Host

$validationSql = @"
SELECT 'TB_IDX_DAY|' || count(*) || '|' || min("TRADE_DT") || '|' || max("TRADE_DT")
FROM "TB_IDX_DAY" WHERE "TRADE_DT" BETWEEN '$from' AND '$to';
SELECT 'TB_EXCH_DAY|' || count(*) || '|' || min("BASE_DT") || '|' || max("BASE_DT")
FROM "TB_EXCH_DAY" WHERE "BASE_DT" BETWEEN '$from' AND '$to';
"@

Write-Host 'V2 rows after synchronization:'
Invoke-TargetSql -Sql $validationSql -TuplesOnly | ForEach-Object { Write-Host "  $_" }
