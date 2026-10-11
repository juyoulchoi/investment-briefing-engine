param([string]$CandidateDatabase='investment_unified_candidate')
$ErrorActionPreference='Stop'
if($CandidateDatabase -notmatch '^investment_unified_[a-z0-9_]+$'){throw 'Candidate database required'}
$source=docker inspect investment-source-postgres|ConvertFrom-Json
$collector=docker inspect investment-kofia-collector|ConvertFrom-Json
$backend=docker inspect investment-backend|ConvertFrom-Json
if($LASTEXITCODE -ne 0){throw 'Container inspection failed'}
$environment=@{}
foreach($entry in $collector[0].Config.Env){$parts=$entry.Split('=',2);$environment[$parts[0]]=$parts[1]}
foreach($entry in $backend[0].Config.Env){
 $parts=$entry.Split('=',2)
 if($parts[0] -match '^(KRX_|YAHOO_|FRED_)'){$environment[$parts[0]]=$parts[1]}
}
$password=@($source[0].Config.Env|Where-Object {$_ -like 'POSTGRES_PASSWORD=*'})[0].Substring('POSTGRES_PASSWORD='.Length)
$environment['SPRING_DATASOURCE_URL']="jdbc:postgresql://source-postgres:5432/$CandidateDatabase`?currentSchema=source,portfolio"
$credentials=@{}
foreach($line in Get-Content F:\source\investment-briefing-engine\.env.unified){$parts=$line.Split('=',2);$credentials[$parts[0]]=$parts[1]}
$environment['SPRING_DATASOURCE_USERNAME']=$credentials['UNIFIED_COLLECTOR_USERNAME']
$environment['SPRING_DATASOURCE_PASSWORD']=$credentials['UNIFIED_COLLECTOR_PASSWORD']
$environment['SPRING_FLYWAY_ENABLED']='true'
$environment['SPRING_FLYWAY_USER']='source_owner'
$environment['SPRING_FLYWAY_PASSWORD']=$password
$environment['SPRING_FLYWAY_DEFAULT_SCHEMA']='source'
$environment['SPRING_FLYWAY_LOCATIONS']='classpath:db/collector,classpath:db/unified/source'
$environment['EXCHANGE_SOURCE_ENABLED']='false'
$environment['KOFIA_SHARED_COLLECTION_ENABLED']='false'
$environment['EXCHANGE_SHARED_COLLECTION_ENABLED']='false'
$environment['MARKET_SOURCE_ENABLED']='true'
$environment['MARKET_SHARED_COLLECTION_ENABLED']='true'
foreach($key in @('KOFIA_SCHEDULER_ENABLED','KOFIA_FUND_FLOW_SCHEDULER_ENABLED','KOFIA_CUSTOMER_SCALE_SCHEDULER_ENABLED','KOFIA_COMPANY_FUND_FLOW_SCHEDULER_ENABLED','KOFIA_RECOVERY_ENABLED','EXCHANGE_SHARED_COLLECTION_ENABLED')){$environment[$key]='false'}
$file='F:\source\investment-briefing-engine\backups\unified-db\candidate-env\collector.env'
$environment.GetEnumerator()|Sort-Object Name|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content -LiteralPath $file -Encoding utf8
docker run -d --name investment-unified-collector-candidate --network investment-source-read --network-alias unified-collector-candidate --env-file $file -p '127.0.0.1:18083:8080' investment-unified-collector:local
if($LASTEXITCODE -ne 0){throw 'Candidate collector start failed'}
