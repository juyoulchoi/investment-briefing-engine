param([string]$Database='investment_unified_live')
$ErrorActionPreference='Stop'
if($Database -notmatch '^investment_unified_[a-z0-9_]+$'){throw 'Invalid database name'}
$root='F:\source\investment-briefing-engine'
$runtime=Join-Path $root 'backups\unified-db\runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$credentials=@{}
foreach($line in Get-Content (Join-Path $root '.env.unified')){$p=$line.Split('=',2);$credentials[$p[0]]=$p[1]}
$source=docker inspect investment-source-postgres | ConvertFrom-Json
if($LASTEXITCODE -ne 0){throw 'Source inspection failed'}
$ownerPassword=@($source[0].Config.Env|Where-Object {$_ -like 'POSTGRES_PASSWORD=*'})[0].Substring(18)
foreach($model in @('v1','v2','collector')){
 $original=switch($model){'v1'{'investment-backend'}'v2'{'investment-v2-backend'}'collector'{'investment-kofia-collector'}}
 $info=docker inspect $original|ConvertFrom-Json
 if($LASTEXITCODE -ne 0){throw 'Original inspection failed'}
 $envValues=@{}
 foreach($entry in $info[0].Config.Env){$p=$entry.Split('=',2);$envValues[$p[0]]=$p[1]}
 $schemas=if($model -eq 'collector'){'source,portfolio'}else{"$model,portfolio,source"}
 $envValues['SPRING_DATASOURCE_URL']="jdbc:postgresql://source-postgres:5432/$Database`?currentSchema=$schemas"
 $key=$model.ToUpper()
 $envValues['SPRING_DATASOURCE_USERNAME']=$credentials["UNIFIED_${key}_USERNAME"]
 $envValues['SPRING_DATASOURCE_PASSWORD']=$credentials["UNIFIED_${key}_PASSWORD"]
 $envValues['SPRING_FLYWAY_ENABLED']='true'
 $envValues['SPRING_FLYWAY_USER']='source_owner'
 $envValues['SPRING_FLYWAY_PASSWORD']=$ownerPassword
 if($model -eq 'collector'){
  foreach($line in Get-Content (Join-Path $root 'backups\unified-db\candidate-env\collector.env')){
   $p=$line.Split('=',2);if($p[0] -match '^(KRX_|FRED_|YAHOO_)'){$envValues[$p[0]]=$p[1]}
  }
  $envValues['SPRING_FLYWAY_DEFAULT_SCHEMA']='source'
  $envValues['SPRING_FLYWAY_LOCATIONS']='classpath:db/collector,classpath:db/unified/source'
  $envValues['MARKET_SOURCE_ENABLED']='true'
  $envValues['MARKET_SHARED_COLLECTION_ENABLED']='true'
  $envValues['MARKET_RECOVERY_ENABLED']='true'
 }else{
  $envValues['SPRING_FLYWAY_DEFAULT_SCHEMA']=$model
  $envValues['SPRING_FLYWAY_LOCATIONS']="classpath:db/migration,classpath:db/unified/$model"
  $envValues['UNIFIED_DATABASE_ENABLED']='true'
  $envValues['UNIFIED_DATABASE_CANDIDATE']='false'
  $envValues['UNIFIED_DATABASE_MODEL']=$model
  $envValues['UNIFIED_MARKET_SOURCE_CONSUMER']='true'
  $envValues['UNIFIED_MARKET_SOURCE_BASE_URL']='http://kofia-collector:8080'
  $envValues['EXCHANGE_SOURCE_URL']="jdbc:postgresql://source-postgres:5432/$Database`?currentSchema=source"
  $envValues['EXCHANGE_SOURCE_USERNAME']=$envValues['SPRING_DATASOURCE_USERNAME']
  $envValues['EXCHANGE_SOURCE_PASSWORD']=$envValues['SPRING_DATASOURCE_PASSWORD']
 }
 $envValues.GetEnumerator()|Sort-Object Name|ForEach-Object {"$($_.Key)=$($_.Value)"}|Set-Content -LiteralPath (Join-Path $runtime "$model.env") -Encoding utf8
}
Write-Output 'Prepared unified runtime environment without printing credentials.'
