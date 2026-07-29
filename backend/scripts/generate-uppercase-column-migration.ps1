$ErrorActionPreference = 'Stop'

$exact = @{
    'created_at' = 'REG_DT'
    'updated_at' = 'MOD_DT'
    'published_at' = 'PUB_DT'
    'started_at' = 'START_DT'
    'completed_at' = 'END_DT'
    'collected_at' = 'CLCT_DT'
    'base_date' = 'BASE_DT'
    'trade_date' = 'TRD_DT'
    'trading_day' = 'TRD_DT'
    'decision_date' = 'DEC_DT'
    'execution_date' = 'EXEC_DT'
    'analysis_date' = 'ANLYS_DT'
    'analysis_dt' = 'ANLYS_DT'
    'day_of_week' = 'WEEK_DAY'
    'day_of_month' = 'MONTH_DAY'
}

$token = @{
    'account' = 'ACCT'; 'briefing' = 'BRF'; 'investment' = 'INV'; 'stock' = 'STK'
    'market' = 'MKT'; 'decision' = 'DEC'; 'execution' = 'EXEC'; 'strategy' = 'STRG'
    'indicator' = 'IND'; 'analysis' = 'ANLYS'; 'collection' = 'CLCT'; 'dataset' = 'DATA'
    'evidence' = 'EVDC'; 'setting' = 'SET'; 'calendar' = 'CAL'; 'holding' = 'HOLD'
    'code' = 'CD'; 'group' = 'GRP'; 'key' = 'KEY'; 'name' = 'NM'; 'type' = 'TP'
    'status' = 'ST'; 'amount' = 'AMT'; 'quantity' = 'QTY'; 'price' = 'PRC'
    'value' = 'VAL'; 'rate' = 'RT'; 'ratio' = 'RT'; 'weight' = 'WGT'
    'summary' = 'SUM'; 'content' = 'CONT'; 'description' = 'DESC'; 'message' = 'MSG'
    'signal' = 'SIG'; 'action' = 'ACT'; 'required' = 'REQ'; 'regular' = 'REG'
    'additional' = 'ADD'; 'recommended' = 'RCM'; 'minimum' = 'MIN'; 'maximum' = 'MAX'
    'current' = 'CUR'; 'target' = 'TGT'; 'available' = 'AVAIL'; 'total' = 'TOT'
    'foreign' = 'FRGN'; 'institution' = 'INST'; 'individual' = 'INDV'
    'program' = 'PRGM'; 'futures' = 'FUT'; 'exchange' = 'EXCH'; 'trading' = 'TRD'
    'source' = 'SRC'; 'related' = 'REL'; 'direction' = 'DIR'; 'level' = 'LVL'
    'display' = 'DSP'; 'order' = 'ORD'; 'active' = 'ACTV'; 'reason' = 'RSN'
    'payload' = 'PAYLOAD'; 'result' = 'RSLT'; 'request' = 'REQ'; 'response' = 'RES'
    'newly' = 'NEW'; 'reserved' = 'RSV'; 'cash' = 'CASH'
    'sentiment' = 'SENT'; 'phase' = 'PHASE'; 'score' = 'SCORE'; 'regime' = 'REGIME'
    'close' = 'CLS'; 'open' = 'OPEN'; 'high' = 'HIGH'; 'low' = 'LOW'
    'previous' = 'PREV'; 'change' = 'CHG'; 'volume' = 'VOL'; 'provider' = 'PRVDR'
    'currency' = 'CURR'; 'company' = 'CO'; 'listing' = 'LIST'; 'scope' = 'SCOPE'
    'asset' = 'ASSET'; 'grade' = 'GRD'; 'benchmark' = 'BM'; 'rule' = 'RULE'
    'drop' = 'DROP'; 'input' = 'IN'; 'profit' = 'PFT'; 'purchase' = 'PUR'
    'calculation' = 'CALC'; 'calc' = 'CALC'; 'final' = 'FINAL'; 'risk' = 'RISK'
    'buy' = 'BUY'; 'rebuy' = 'REBUY'; 'sell' = 'SELL'; 'light' = 'LIGHT'
    'memo' = 'MEMO'; 'title' = 'TTL'; 'item' = 'ITEM'; 'number' = 'NO'
    'count' = 'CNT'; 'received' = 'RCV'; 'stored' = 'STRD'; 'error' = 'ERR'
    'multiplier' = 'MULT'; 'reasons' = 'RSNS'; 'impact' = 'IMP'; 'return' = 'RTN'
    'balance' = 'BAL'; 'adjusted' = 'ADJ'; 'symbol' = 'SYM'; 'holiday' = 'HLDY'
    'sort' = 'SORT'; 'use' = 'USE'; 'date' = 'DT'; 'time' = 'TM'
    'id' = 'ID'; 'yn' = 'YN'; 'no' = 'NO'; 'rt' = 'RT'; 'dt' = 'DT'
    'cd' = 'CD'; 'nm' = 'NM'; 'tp' = 'TP'; 'st' = 'ST'; 'amt' = 'AMT'
    'qty' = 'QTY'; 'prc' = 'PRC'; 'cont' = 'CONT'; 'sum' = 'SUM'
    'sig' = 'SIG'; 'act' = 'ACT'; 'reg' = 'REG'; 'mod' = 'MOD'
    'acct' = 'ACCT'; 'brf' = 'BRF'; 'inv' = 'INV'; 'stk' = 'STK'
    'mkt' = 'MKT'; 'ind' = 'IND'; 'exec' = 'EXEC'; 'strg' = 'STRG'
    'evdc' = 'EVDC'; 'clct' = 'CLCT'; 'data' = 'DATA'; 'dtl' = 'DTL'
}

function Convert-ColumnName([string]$name) {
    if ($exact.ContainsKey($name)) {
        return $exact[$name]
    }
    return (($name -split '_') | ForEach-Object {
        if ($token.ContainsKey($_)) { $token[$_] } else { $_.ToUpperInvariant() }
    }) -join '_'
}

$query = @"
SELECT table_name || '|' || ordinal_position || '|' || column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name LIKE 'TB\_%' ESCAPE '\'
ORDER BY table_name, ordinal_position
"@

$rows = docker exec investment-postgres psql -U investment -d investment -At -c $query
$tables = [ordered]@{}
foreach ($row in $rows) {
    $parts = $row -split '\|', 3
    if (-not $tables.Contains($parts[0])) {
        $tables[$parts[0]] = [System.Collections.Generic.List[object]]::new()
    }
    $tables[$parts[0]].Add([pscustomobject]@{
        Old = $parts[2]
        New = Convert-ColumnName $parts[2]
    })
}

$sql = [System.Collections.Generic.List[string]]::new()
foreach ($table in $tables.Keys) {
    $duplicates = $tables[$table] | Group-Object New | Where-Object Count -gt 1
    if ($duplicates) {
        throw "Duplicate abbreviated column in ${table}: $($duplicates.Name -join ', ')"
    }

    foreach ($column in $tables[$table]) {
        if ($column.Old -cne $column.New) {
            $sql.Add("ALTER TABLE `"$table`" RENAME COLUMN `"$($column.Old)`" TO `"$($column.New)`";")
        }
    }
    $sql.Add('')
}

foreach ($table in $tables.Keys) {
    $view = $table.ToLowerInvariant()
    $selectColumns = ($tables[$table] | ForEach-Object {
        "    `"$($_.New)`" AS `"$($_.Old)`""
    }) -join ",`r`n"
    $sql.Add("CREATE OR REPLACE VIEW $view AS")
    $sql.Add("SELECT")
    $sql.Add($selectColumns)
    $sql.Add("FROM `"$table`";")
    $sql.Add('')
}

$target = Join-Path $PSScriptRoot '..\src\main\resources\db\migration\V42__abbreviate_and_uppercase_columns.sql'
Set-Content -LiteralPath $target -Value $sql -Encoding utf8
Write-Host "Generated $target"
