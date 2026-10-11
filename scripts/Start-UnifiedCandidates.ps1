param([string]$CandidateDatabase='investment_unified_candidate',
      [string]$V1Image='investment-unified-v1:local',
      [string]$V2Image='investment-unified-v2:local')
$ErrorActionPreference='Stop'
if ($CandidateDatabase -notmatch '^investment_unified_[a-z0-9_]+$') {throw 'Candidate database required'}
$root='F:\source\investment-briefing-engine'
$envRoot=Join-Path $root 'backups\unified-db\candidate-env'
New-Item -ItemType Directory -Path $envRoot -Force | Out-Null
$sourceInfo=docker inspect investment-source-postgres | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) {throw 'Source inspection failed'}
$passwordEntry=@($sourceInfo[0].Config.Env | Where-Object {$_ -like 'POSTGRES_PASSWORD=*'})
if ($passwordEntry.Count -ne 1) {throw 'Source credential unavailable'}
$password=$passwordEntry[0].Substring('POSTGRES_PASSWORD='.Length)
$credentials=@{}
foreach($line in Get-Content (Join-Path $root '.env.unified')){$parts=$line.Split('=',2);$credentials[$parts[0]]=$parts[1]}
foreach($model in @('v1','v2')) {
 $original=if($model -eq 'v1'){'investment-backend'}else{'investment-v2-backend'}
 $port=if($model -eq 'v1'){18081}else{18082}
 $info=docker inspect $original | ConvertFrom-Json
 if($LASTEXITCODE -ne 0){throw 'Application inspection failed'}
 $environment=@{}
 foreach($entry in $info[0].Config.Env){$parts=$entry.Split('=',2);$environment[$parts[0]]=$parts[1]}
 $environment['SPRING_DATASOURCE_URL']="jdbc:postgresql://source-postgres:5432/$CandidateDatabase`?currentSchema=$model,portfolio,source"
 $environment['SPRING_DATASOURCE_USERNAME']=$credentials["UNIFIED_$($model.ToUpper())_USERNAME"]
 $environment['SPRING_DATASOURCE_PASSWORD']=$credentials["UNIFIED_$($model.ToUpper())_PASSWORD"]
 $environment['SPRING_FLYWAY_ENABLED']='true'
 $environment['SPRING_FLYWAY_USER']='source_owner'
 $environment['SPRING_FLYWAY_PASSWORD']=$password
 $environment['SPRING_FLYWAY_DEFAULT_SCHEMA']=$model
 $environment['SPRING_FLYWAY_LOCATIONS']="classpath:db/migration,classpath:db/unified/$model"
 $environment['UNIFIED_DATABASE_ENABLED']='true'
 $environment['UNIFIED_DATABASE_CANDIDATE']='true'
 $environment['UNIFIED_DATABASE_MODEL']=$model
 $environment['EXCHANGE_SOURCE_URL']="jdbc:postgresql://source-postgres:5432/$CandidateDatabase`?currentSchema=source"
 $environment['EXCHANGE_SOURCE_USERNAME']=$environment['SPRING_DATASOURCE_USERNAME']
 $environment['EXCHANGE_SOURCE_PASSWORD']=$environment['SPRING_DATASOURCE_PASSWORD']
 $environment['UNIFIED_MARKET_SOURCE_CONSUMER']='true'
 $environment['UNIFIED_MARKET_SOURCE_BASE_URL']='http://unified-collector-candidate:8080'
 foreach($key in @('INVESTMENT_BRIEFING_SCHEDULER_CRON','INVESTMENT_BRIEFING_WEEKLY_DECISION_SCHEDULER_CRON','INVESTMENT_BRIEFING_WEEKLY_SCHEDULER_CRON')) {$environment[$key]='-'}
 $environment['KOFIA_CONSUMER_ENABLED']='true'
 $environment['KOFIA_RECOVERY_ENABLED']='false'
 $file=Join-Path $envRoot "$model.env"
 $environment.GetEnumerator() | Sort-Object Name | ForEach-Object {"$($_.Key)=$($_.Value)"} | Set-Content -LiteralPath $file -Encoding utf8
 $image=if($model -eq 'v1'){$V1Image}else{$V2Image}
 docker run -d --name "investment-unified-$model-candidate" --network investment-source-read --env-file $file -p "127.0.0.1:${port}:8080" $image
 if($LASTEXITCODE -ne 0){throw 'Candidate application startup failed; preserve containers for diagnosis'}
}
Write-Output 'Read candidate APIs at localhost:18081 and localhost:18082. Existing application ports are unchanged.'
