$ErrorActionPreference='Stop'
foreach($table in @('TB_ACCT','TB_HOLD','TB_REG_BUY','TB_CASH_HIS','TB_CASH_RSV')){
 $schema=if($table -in @('TB_HOLD','TB_REG_BUY')){'v1'}else{'portfolio'}
 $originalSql='SELECT md5(string_agg(to_jsonb(t)::text,''|'' ORDER BY to_jsonb(t)::text)) FROM public."'+$table+'" t;'
 $candidateSql='SELECT md5(string_agg(to_jsonb(t)::text,''|'' ORDER BY to_jsonb(t)::text)) FROM '+$schema+'."'+$table+'" t;'
 $original=$originalSql|docker exec -i investment-postgres psql -X -At -v ON_ERROR_STOP=1 -U investment -d investment
 if($LASTEXITCODE -ne 0){throw "Original fingerprint failed: $table"}
 $candidate=$candidateSql|docker exec -i investment-source-postgres psql -X -At -v ON_ERROR_STOP=1 -U source_owner -d investment_unified_candidate
 if($LASTEXITCODE -ne 0){throw "Candidate fingerprint failed: $table"}
 if($original -ne $candidate){throw "Live V1 changed or candidate mismatch: $table"}
 Write-Output "$table full rows unchanged and equal"
}
