$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$runtime=Join-Path $root 'backups/unified-db/runtime'
function Read-EnvironmentFile([string]$Path) {
 $values=@{}
 foreach($line in Get-Content -LiteralPath $Path){if($line -match '^([^#=]+)=(.*)$'){$values[$matches[1]]=$matches[2]}}
 return $values
}
$v1=Read-EnvironmentFile (Join-Path $runtime 'v1.env')
$collector=Read-EnvironmentFile (Join-Path $runtime 'collector.env')
$pg=docker inspect investment-source-postgres | ConvertFrom-Json
if($LASTEXITCODE -ne 0){throw 'Unified PostgreSQL inspection failed'}
$dbEnv=@{}
foreach($entry in $pg[0].Config.Env){$p=$entry.Split('=',2);$dbEnv[$p[0]]=$p[1]}
if($v1['SPRING_DATASOURCE_URL'] -notlike '*investment_unified_live*'){throw 'Expected live unified database'}
$credentialPath=Join-Path $root '.env.single-backend'
if(Test-Path -LiteralPath $credentialPath){$credentials=Read-EnvironmentFile $credentialPath}else{
 $bytes=New-Object byte[] 32
 [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
 $credentials=@{BACKEND_PASSWORD=[Convert]::ToHexString($bytes);POSTGRES_PASSWORD=$dbEnv['POSTGRES_PASSWORD']}
 $credentials.GetEnumerator()|Sort-Object Name|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content -LiteralPath $credentialPath -Encoding utf8
}
$sql=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'Grant-SingleBackendRole.sql') -Raw
$sql | docker exec -i investment-source-postgres psql -U source_owner -d investment_unified_live
if($LASTEXITCODE -ne 0){throw 'Single backend role provisioning failed'}
if($credentials['BACKEND_PASSWORD'] -notmatch '^[A-F0-9]{64}$'){throw 'Unexpected backend password format'}
"ALTER ROLE unified_backend LOGIN PASSWORD '$($credentials['BACKEND_PASSWORD'])';" | docker exec -i investment-source-postgres psql -U source_owner -d investment_unified_live
if($LASTEXITCODE -ne 0){throw 'Role login configuration failed'}
foreach($key in @($collector.Keys)){
 if($key -match '^(KRX_|FRED_|YAHOO_|KOFIA_)' -and $key -notmatch '^KOFIA_CONSUMER'){$v1[$key]=$collector[$key]}
}
$v1['SPRING_DATASOURCE_USERNAME']='unified_backend'
$v1['SPRING_DATASOURCE_PASSWORD']=$credentials['BACKEND_PASSWORD']
$v1['UNIFIED_SINGLE_BACKEND']='true'
$v1['UNIFIED_MARKET_SOURCE_CONSUMER']='false'
$v1['KOFIA_CONSUMER_ENABLED']='false'
$v1['EXCHANGE_SOURCE_ENABLED']='false'
$v1['EXCHANGE_SHARED_COLLECTION_ENABLED']=$collector['EXCHANGE_SHARED_COLLECTION_ENABLED']
$v1['UNIFIED_DATABASE_CANDIDATE']='false'
foreach($key in @('EXCHANGE_SOURCE_URL','EXCHANGE_SOURCE_USERNAME','EXCHANGE_SOURCE_PASSWORD','UNIFIED_MARKET_SOURCE_BASE_URL','KOFIA_CONSUMER_BASE_URL')){$v1.Remove($key)}
$v1.GetEnumerator()|Sort-Object Name|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content -LiteralPath (Join-Path $runtime 'backend.env') -Encoding utf8
@("POSTGRES_DB=investment_unified_live","POSTGRES_USER=source_owner","POSTGRES_PASSWORD=$($credentials['POSTGRES_PASSWORD'])")|Set-Content -LiteralPath (Join-Path $runtime 'postgres.env') -Encoding utf8
Write-Output 'Prepared single backend runtime without printing credentials.'
