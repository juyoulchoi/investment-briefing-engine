param(
  [string]$CandidateDatabase = 'investment_unified_candidate',
  [string]$BackupRoot = 'F:\source\investment-briefing-engine\backups\unified-db'
)
$ErrorActionPreference = 'Stop'
if ($CandidateDatabase -notmatch '^investment_unified_[a-z0-9_]+$') { throw 'Use an isolated investment_unified_ database.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = Join-Path $BackupRoot $stamp
New-Item -ItemType Directory -Path $backup -Force | Out-Null
function Run-Docker([string[]]$Arguments) {
  & docker @Arguments
  if ($LASTEXITCODE -ne 0) { throw "Docker failed: $($Arguments[0..2] -join ' ')" }
}
function Sql([string]$Statement) {
  $Statement | docker exec -i investment-source-postgres psql -X -v ON_ERROR_STOP=1 -U source_owner -d $CandidateDatabase
  if ($LASTEXITCODE -ne 0) { throw 'Candidate SQL failed.' }
}
$exists = & docker exec investment-source-postgres psql -X -U source_owner -d investment_source -At -c "SELECT 1 FROM pg_database WHERE datname='$CandidateDatabase'"
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect source database.' }
if ($exists -eq '1') { throw 'Candidate already exists; preserve it and choose a new candidate name.' }
$inputs = @(
  @{Name='source';Container='investment-source-postgres';User='source_owner';Database='investment_source'},
  @{Name='v1';Container='investment-postgres';User='investment';Database='investment'},
  @{Name='v2';Container='investment-v2-postgres';User='investment';Database='investment'}
)
$manifest = @()
foreach ($inputDb in $inputs) {
  $remote = "/tmp/unified-$stamp-$($inputDb.Name).dump"
  $local = Join-Path $backup "$($inputDb.Name).dump"
  Run-Docker @('exec',$inputDb.Container,'pg_dump','-Fc','--no-owner','--no-acl','-U',$inputDb.User,'-d',$inputDb.Database,'-f',$remote)
  Run-Docker @('cp',"$($inputDb.Container):$remote",$local)
  $manifest += [pscustomobject]@{Schema=$inputDb.Name;Container=$inputDb.Container;Database=$inputDb.Database;File=$local;SHA256=(Get-FileHash -LiteralPath $local -Algorithm SHA256).Hash;Bytes=(Get-Item -LiteralPath $local).Length}
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'manifest.json') -Encoding utf8
Run-Docker @('exec','investment-source-postgres','createdb','-U','source_owner',$CandidateDatabase)
foreach ($inputDb in $inputs) {
  $targetDump = "/tmp/unified-$stamp-$($inputDb.Name)-restore.dump"
  Run-Docker @('cp',(Join-Path $backup "$($inputDb.Name).dump"),"investment-source-postgres:$targetDump")
  Run-Docker @('exec','investment-source-postgres','pg_restore','--exit-on-error','--no-owner','--no-acl','-U','source_owner','-d',$CandidateDatabase,$targetDump)
  Sql "ALTER SCHEMA public RENAME TO $($inputDb.Name); CREATE SCHEMA public AUTHORIZATION source_owner;"
}
Sql 'REVOKE CREATE ON SCHEMA public FROM PUBLIC; CREATE SCHEMA portfolio AUTHORIZATION source_owner;'
$manifest | Format-Table Schema,Bytes,SHA256 -AutoSize
Write-Output "Candidate: $CandidateDatabase; backups: $backup"
Write-Output 'All existing application databases and connections remain intact. No candidate data has been merged.'
