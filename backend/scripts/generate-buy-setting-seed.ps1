$ErrorActionPreference = 'Stop'

$source = 'C:\Users\juyou\.codex\attachments\575cb13a-17c4-4f70-b819-14c1a83332bc\pasted-text.txt'
$target = Join-Path $PSScriptRoot '..\src\main\resources\db\migration\V44__seed_buy_management.sql'

function Sql-Text([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Trim() -eq '-') { return 'NULL' }
    return "'" + $value.Trim().Replace("'", "''") + "'"
}

function Sql-Number([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Trim() -eq '-') { return 'NULL' }
    $digits = $value -replace '[^0-9.]', ''
    if ([string]::IsNullOrWhiteSpace($digits)) { return 'NULL' }
    return $digits
}

function Parse-Cycle([string]$value) {
    $result = [ordered]@{ Type = 'PAUSED'; Week = $null; Month = $null }
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq '일시정지') { return $result }
    if ($value -match '^매월\s*(\d+)일$') {
        $result.Type = 'MONTHLY'
        $result.Month = $Matches[1]
        return $result
    }
    if ($value.StartsWith('매주 ')) {
        $result.Type = 'WEEKLY'
        $days = $value.Substring(3) -replace '요일', ''
        $dayMap = @{ '월' = 'MON'; '화' = 'TUE'; '수' = 'WED'; '목' = 'THU'; '금' = 'FRI' }
        $result.Week = (($days -split ',') | ForEach-Object { $dayMap[$_.Trim()] }) -join ','
        return $result
    }
    throw "지원하지 않는 모으기 주기: $value"
}

function Parse-Value([string]$value) {
    $result = [ordered]@{ Amount = 'NULL'; Quantity = 'NULL' }
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Trim() -eq '-') { return $result }
    if ($value.Contains('주')) {
        $result.Quantity = Sql-Number $value
    } else {
        $result.Amount = Sql-Number $value
    }
    return $result
}

$accountMap = @{
    '국내' = 'DOMESTIC'
    '해외' = 'GENERAL'
    'ISA' = 'ISA'
    '연금' = 'PENSION'
}

$rows = Import-Csv -LiteralPath $source -Delimiter "`t"
$sql = [System.Collections.Generic.List[string]]::new()

foreach ($row in $rows) {
    $baseCycle = Parse-Cycle $row.'기본 모으기주기'
    $applyCycle = Parse-Cycle $row.'적용 모으기주기'
    $baseValue = Parse-Value $row.'기본금액'
    $applyValue = Parse-Value $row.'적용금액'
    $activeYn = if ($applyCycle.Type -eq 'PAUSED') { 'N' } else { 'Y' }
    $execStatus = switch ($row.'오늘 실행') {
        '중지' { 'PAUSED' }
        '대기' { 'WAIT' }
        default { $row.'오늘 실행' }
    }

    $values = @(
        (Sql-Text $accountMap[$row.'계좌']),
        (Sql-Text $row.'시장'),
        (Sql-Text $row.'종목코드'),
        (Sql-Text $row.'종목명'),
        (Sql-Text $baseCycle.Type),
        (Sql-Text $baseCycle.Week),
        $(if ($baseCycle.Month) { $baseCycle.Month } else { 'NULL' }),
        $baseValue.Amount,
        $baseValue.Quantity,
        (Sql-Text $row.'중단이유'),
        (Sql-Number $row.'우선순위'),
        (Sql-Text $applyCycle.Type),
        (Sql-Text $applyCycle.Week),
        $(if ($applyCycle.Month) { $applyCycle.Month } else { 'NULL' }),
        $applyValue.Amount,
        $applyValue.Quantity,
        (Sql-Text $activeYn),
        (Sql-Text $row.'중지사유'),
        (Sql-Text $execStatus),
        (Sql-Number $row.'월 예상금액'),
        (Sql-Number $row.'실행순번'),
        (Sql-Text $row.'비고')
    ) -join ', '

    $sql.Add(@"
INSERT INTO "TB_REG_BUY" (
    "ACCT_TP", "MKT_CD", "STK_CD", "STK_NM",
    "BASE_CYCLE_TP", "BASE_WEEK_DAY", "BASE_MONTH_DAY", "BASE_AMT", "BASE_QTY",
    "BASE_PAUSE_RSN", "PRIORITY",
    "CYCLE_TP", "WEEK_DAY", "MONTH_DAY", "AMT", "QTY",
    "ACTV_YN", "PAUSE_RSN", "EXEC_ST", "MONTH_EST_AMT", "EXEC_NO", "MEMO"
) VALUES ($values)
ON CONFLICT ("ACCT_TP", "STK_CD") DO UPDATE SET
    "MKT_CD" = EXCLUDED."MKT_CD",
    "STK_NM" = EXCLUDED."STK_NM",
    "BASE_CYCLE_TP" = EXCLUDED."BASE_CYCLE_TP",
    "BASE_WEEK_DAY" = EXCLUDED."BASE_WEEK_DAY",
    "BASE_MONTH_DAY" = EXCLUDED."BASE_MONTH_DAY",
    "BASE_AMT" = EXCLUDED."BASE_AMT",
    "BASE_QTY" = EXCLUDED."BASE_QTY",
    "BASE_PAUSE_RSN" = EXCLUDED."BASE_PAUSE_RSN",
    "PRIORITY" = EXCLUDED."PRIORITY",
    "CYCLE_TP" = EXCLUDED."CYCLE_TP",
    "WEEK_DAY" = EXCLUDED."WEEK_DAY",
    "MONTH_DAY" = EXCLUDED."MONTH_DAY",
    "AMT" = EXCLUDED."AMT",
    "QTY" = EXCLUDED."QTY",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "PAUSE_RSN" = EXCLUDED."PAUSE_RSN",
    "EXEC_ST" = EXCLUDED."EXEC_ST",
    "MONTH_EST_AMT" = EXCLUDED."MONTH_EST_AMT",
    "EXEC_NO" = EXCLUDED."EXEC_NO",
    "MEMO" = EXCLUDED."MEMO",
    "MOD_DT" = CURRENT_TIMESTAMP;
"@)
}

Set-Content -LiteralPath $target -Value $sql -Encoding utf8
Write-Host "Generated $($rows.Count) rows in $target"

