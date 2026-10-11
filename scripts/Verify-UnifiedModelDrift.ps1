$ErrorActionPreference='Stop'
foreach($model in @('v1','v2')){
 $original=if($model -eq 'v1'){'investment-postgres'}else{'investment-v2-postgres'}
 $list="SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='$model' AND c.relkind='r' ORDER BY c.relname;"
 $tables=@($list|docker exec -i investment-source-postgres psql -X -At -U source_owner -d investment_unified_candidate)|Where-Object {$_ -notin @('TB_API_LOG','TB_ERR_LOG','TB_EXT_CIRCUIT','TB_RECALC_QUEUE','TB_HOLD_MODEL','TB_REG_BUY_MODEL','flyway_schema_history')}
 $sourceQueries=@();$candidateQueries=@()
 foreach($table in $tables){
  if($table -notmatch '^TB_[A-Z0-9_]+$'){throw 'Invalid table name'}
  $value='to_jsonb(t)'
  if($model -eq 'v2'){
   $value='CASE WHEN to_jsonb(t)->>''STK_ID'' IN (''1063'',''1060'',''1061'',''1058'',''1062'',''1059'') THEN jsonb_set(to_jsonb(t),''{STK_ID}'',to_jsonb(CASE (to_jsonb(t)->>''STK_ID'')::bigint WHEN 1063 THEN 1649 WHEN 1060 THEN 1650 WHEN 1061 THEN 1651 WHEN 1058 THEN 1652 WHEN 1062 THEN 1653 WHEN 1059 THEN 1654 END)) ELSE to_jsonb(t) END'
  }
  $sourceQueries+="SELECT '$table',md5(string_agg(j::text,'|' ORDER BY j::text)) FROM (SELECT $value AS j FROM public.`"$table`" t) x"
  $candidateQueries+="SELECT '$table',md5(string_agg(to_jsonb(t)::text,'|' ORDER BY to_jsonb(t)::text)) FROM $model.`"$table`" t"
 }
 $before=($sourceQueries -join ' UNION ALL ')+';'
 $after=($candidateQueries -join ' UNION ALL ')+';'
 $originalHashes=@($before|docker exec -i $original psql -X -At -v ON_ERROR_STOP=1 -U investment -d investment)
 if($LASTEXITCODE -ne 0){throw "$model original check failed"}
 $candidateHashes=@($after|docker exec -i investment-source-postgres psql -X -At -v ON_ERROR_STOP=1 -U source_owner -d investment_unified_candidate)
 if($LASTEXITCODE -ne 0){throw "$model candidate check failed"}
 $differences=Compare-Object $originalHashes $candidateHashes
 if($differences){$differences|Format-Table;throw "$model model data changed or mapping differs"}
 Write-Output "$model $($tables.Count) model/config tables preserved and unchanged"
}
