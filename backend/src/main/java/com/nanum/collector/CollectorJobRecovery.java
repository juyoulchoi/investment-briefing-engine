package com.nanum.collector;

import com.nanum.investment.marketdata.application.KofiaCollectionJobRunner;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Recovery is enabled only after a verified copy and exclusive writer handoff. */
@Component
@ConditionalOnProperty(name = "kofia.recovery.enabled", havingValue = "true")
public class CollectorJobRecovery {
  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;
  private final KofiaCollectionJobRunner runner;

  public CollectorJobRecovery(
      JdbcClient jdbc,
      org.springframework.transaction.PlatformTransactionManager manager,
      KofiaCollectionJobRunner runner) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(manager);
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void resume() {
    var jobs =
        transaction.execute(
            status -> {
              jdbc.sql(
                      """
          UPDATE "TB_KOFIA_CLCT_JOB_ITEM" SET "STS"='PENDING', "START_DTTM"=NULL
          WHERE "STS"='RUNNING' AND "JOB_ID" IN
            (SELECT "JOB_ID" FROM "TB_KOFIA_CLCT_JOB" WHERE "STS" IN ('RUNNING','QUEUED'))
          """)
                  .update();
              jdbc.sql("UPDATE \"TB_KOFIA_CLCT_JOB\" SET \"STS\"='QUEUED' WHERE \"STS\"='RUNNING'")
                  .update();
              return jdbc.sql("SELECT \"JOB_ID\" FROM \"TB_KOFIA_CLCT_JOB\" WHERE \"STS\"='QUEUED'")
                  .query(UUID.class)
                  .list();
            });
    if (jobs != null) jobs.forEach(runner::run);
  }
}
