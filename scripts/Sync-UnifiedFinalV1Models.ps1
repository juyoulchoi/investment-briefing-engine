$ErrorActionPreference='Stop'
$statements=@('BEGIN;')
foreach($table in @('TB_STK_SET','TB_BRF','TB_BRF_DTL')){
 $metaSql=@'
SELECT jsonb_build_object('keys',(SELECT string_agg(quote_ident(a.attname),',' ORDER BY a.attnum) FROM pg_index i JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey) WHERE i.indisprimary AND i.indrelid='public."TABLE"'::regclass),'updates',(SELECT string_agg(format('%I=EXCLUDED.%I',attname,attname),',' ORDER BY attnum) FROM pg_attribute WHERE attrelid='public."TABLE"'::regclass AND attnum>0 AND NOT attisdropped AND attidentity=''));
'@
 $meta=($metaSql.Replace('TABLE',$table)|docker exec -i investment-postgres psql -X -At -v ON_ERROR_STOP=1 -U investment -d investment)|ConvertFrom-Json
 if($LASTEXITCODE -ne 0 -or -not $meta.keys){throw 'Primary key metadata required'}
 $template='INSERT INTO v1."'+$table+'" SELECT * FROM jsonb_populate_record(NULL::v1."'+$table+'",%L::jsonb) ON CONFLICT ('+$meta.keys+') DO UPDATE SET '+$meta.updates+';'
 $query="SELECT format('"+$template.Replace("'","''")+"',to_jsonb(t)::text) FROM public.`"$table`" t;"
 $rows=@($query|docker exec -i investment-postgres psql -X -At -v ON_ERROR_STOP=1 -U investment -d investment)
 if($LASTEXITCODE -ne 0){throw 'Latest model read failed'}
 $statements+=$rows
}
$statements+='COMMIT;'
$statements -join "`n" | docker exec -i investment-source-postgres psql -X -v ON_ERROR_STOP=1 -U source_owner -d investment_unified_candidate | Out-Null
if($LASTEXITCODE -ne 0){throw 'Latest model synchronization failed'}
@'
DO $$ DECLARE r record; latest bigint; BEGIN
 FOR r IN SELECT c.relname,a.attname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 WHERE n.nspname='v1' AND c.relname IN ('TB_BRF','TB_BRF_DTL') AND pg_get_serial_sequence(format('%I.%I',n.nspname,c.relname),a.attname) IS NOT NULL LOOP
  EXECUTE format('SELECT max(%I) FROM v1.%I',r.attname,r.relname) INTO latest;
  IF latest IS NOT NULL THEN PERFORM setval(pg_get_serial_sequence(format('v1.%I',r.relname),r.attname),latest,true); END IF;
 END LOOP;
END $$;
'@|docker exec -i investment-source-postgres psql -X -v ON_ERROR_STOP=1 -U source_owner -d investment_unified_candidate
if($LASTEXITCODE -ne 0){throw 'Latest model sequence alignment failed'}
Write-Output 'Final V1 settings and briefing rows synchronized with original IDs and content.'
