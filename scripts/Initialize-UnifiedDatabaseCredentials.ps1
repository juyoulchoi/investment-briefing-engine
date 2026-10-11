param([string]$Database='investment_unified_candidate')
$ErrorActionPreference='Stop'
if($Database -notmatch '^investment_unified_[a-z0-9_]+$'){throw 'Unified database required'}
$file='F:\source\investment-briefing-engine\.env.unified'
if(Test-Path -LiteralPath $file){throw 'Preserve existing .env.unified; reuse its credentials rather than replacing them'}
$source=docker inspect investment-source-postgres|ConvertFrom-Json
if($LASTEXITCODE -ne 0){throw 'Source inspection failed'}
$ownerPassword=@($source[0].Config.Env|Where-Object {$_ -like 'POSTGRES_PASSWORD=*'})[0].Substring('POSTGRES_PASSWORD='.Length)
$settings=@{UNIFIED_DATABASE=$Database;UNIFIED_MIGRATION_USERNAME='source_owner';UNIFIED_MIGRATION_PASSWORD=$ownerPassword}
foreach($model in @('v1','v2','collector')) {
 $password=[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
 $role="unified_$model"
 "ALTER ROLE $role LOGIN PASSWORD '$password';"|docker exec -i investment-source-postgres psql -X -v ON_ERROR_STOP=1 -U source_owner -d $Database
 if($LASTEXITCODE -ne 0){throw 'Role credential initialization failed'}
 $settings["UNIFIED_$($model.ToUpper())_USERNAME"]=$role
 $settings["UNIFIED_$($model.ToUpper())_PASSWORD"]=$password
}
$settings.GetEnumerator()|Sort-Object Name|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content -LiteralPath $file -Encoding utf8
Write-Output 'Dedicated role credentials saved to the Git-excluded .env.unified.'
