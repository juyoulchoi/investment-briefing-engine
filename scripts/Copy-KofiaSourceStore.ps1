[CmdletBinding()]
param(
    [ValidateSet('investment-postgres', 'investment-v2-postgres')]
    [string]$SourceContainer = 'investment-postgres',
    [string]$BackupDirectory = (Join-Path $PSScriptRoot '../backups/kofia-source'),
    [switch]$Apply
)
$ErrorActionPreference = 'Stop'
$destination = 'investment-source-postgres'

function Query([string]$container, [string]$sql) {
    $dbUser = if ($container -eq $destination) { 'source_owner' } else { 'investment' }
    $dbName = if ($container -eq $destination) { 'investment_source' } else { 'investment' }
    $result = $sql | docker exec -i $container psql -X -v ON_ERROR_STOP=1 -U $dbUser -d $dbName -At
    if ($LASTEXITCODE -ne 0) { throw "SQL failed: $container" }
    return $result
}

$tables = @(Query $SourceContainer "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'TB_KOFIA_%' ORDER BY tablename")
$targetTables = @(Query $destination "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'TB_KOFIA_%' ORDER BY tablename")
if ($tables.Count -eq 0) { throw 'No source KOFIA tables found.' }
if (Compare-Object $tables $targetTables) { throw 'KOFIA table sets differ; reconcile schema before transfer.' }
foreach ($table in $tables) {
    if ($table -notmatch '^TB_KOFIA_[A-Z0-9_]+$') { throw "Unexpected table name: $table" }
    $columnsSql = "SELECT column_name || ':' || data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='$table' ORDER BY ordinal_position"
    if (Compare-Object @(Query $SourceContainer $columnsSql) @(Query $destination $columnsSql)) {
        throw "Column contracts differ: $table"
    }
    $count = Query $destination ('SELECT count(*) FROM "' + $table + '"')
    if ([long]$count -ne 0) { throw "Destination must be empty: $table. Existing data will never be deleted by this script." }
}
if (!$Apply) {
    Write-Output "Validated $($tables.Count) empty destination tables. No data written. Use -Apply after consumer cutover."
    return
}

# A consistent copy must not race either legacy writer or the destination collector.
foreach ($backend in @('investment-backend', 'investment-v2-backend')) {
    $inspection = docker inspect $backend | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw "Cannot inspect $backend" }
    if ($inspection[0].State.Running -and $inspection[0].Config.Env -notcontains 'KOFIA_CONSUMER_ENABLED=true') {
        throw "$backend must be stopped or deployed with the tested KOFIA consumer adapter."
    }
}
$collector = docker inspect investment-kofia-collector | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect collector.' }
if ($collector[0].State.Running -and $collector[0].Config.Env -contains 'KOFIA_SHARED_COLLECTION_ENABLED=true') {
    throw 'Destination collector must remain read-only during copy.'
}

$backupRoot = [IO.Path]::GetFullPath($BackupDirectory)
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
Write-Host "Creating consistent KOFIA dump from $SourceContainer..."
$remoteDump = "/tmp/kofia-$stamp.dump"
$localDump = Join-Path $backupRoot "kofia-$stamp.dump"
# pg_dump custom format preserves UUIDs, source timestamps, payloads, job state and sequence values.
$dumpArgs = @('exec', $SourceContainer, 'pg_dump', '-U', 'investment', '-d', 'investment', '-Fc', '--data-only', '--no-owner', '--no-privileges', '-f', $remoteDump)
foreach ($table in $tables) { $dumpArgs += @('-t', ('public."' + $table + '"')) }
& docker @dumpArgs
if ($LASTEXITCODE -ne 0) { throw 'Source dump failed.' }
docker cp "${SourceContainer}:$remoteDump" $localDump
if ($LASTEXITCODE -ne 0) { throw 'Backup download failed.' }
docker cp $localDump "${destination}:$remoteDump"
if ($LASTEXITCODE -ne 0) { throw 'Backup upload failed.' }
Write-Host 'Restoring into the empty source store in one transaction...'
docker exec $destination pg_restore -U source_owner -d investment_source --data-only --single-transaction --exit-on-error --no-owner --no-privileges $remoteDump
if ($LASTEXITCODE -ne 0) { throw 'Atomic restore failed; source unchanged. Inspect destination before retry.' }

$results = foreach ($table in $tables) {
    Write-Host "Verifying $table..."
    # Order independent checksum over every column, including raw JSON and provenance.
    $fingerprint = 'SELECT count(*)::text || '':'' || COALESCE(sum((''x'' || substr(md5(to_jsonb(t)::text),1,16))::bit(64)::bigint::numeric),0)::text || '':'' || COALESCE(sum((''x'' || substr(md5(to_jsonb(t)::text),17,16))::bit(64)::bigint::numeric),0)::text FROM "' + $table + '" t'
    $sourceFingerprint = Query $SourceContainer $fingerprint
    $targetFingerprint = Query $destination $fingerprint
    [pscustomobject]@{ Table=$table; Source=$sourceFingerprint; Destination=$targetFingerprint; Match=($sourceFingerprint -eq $targetFingerprint) }
}
$reportPath = Join-Path $backupRoot "verification-$stamp.json"
$results | ConvertTo-Json | Set-Content -LiteralPath $reportPath -Encoding utf8
if ($results.Match -contains $false) { throw "Data verification failed; keep collector disabled. Report: $reportPath" }
Write-Output "Verified all $($tables.Count) KOFIA tables. Source retained. Backup: $localDump. Report: $reportPath"
