package podcastService.outbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMigrationContractTest {

    @Test
    void migrationCreatesOutboxEventsStorageOnlySchema() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V8__create_outbox_events.sql"
        ));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS outbox_events");
        assertThat(migration).contains("id uuid PRIMARY KEY");
        assertThat(migration).contains("aggregate_type varchar(64) NOT NULL");
        assertThat(migration).contains("aggregate_id uuid");
        assertThat(migration).contains("event_type varchar(128) NOT NULL");
        assertThat(migration).contains("event_version int NOT NULL DEFAULT 1");
        assertThat(migration).contains("event_key varchar(128)");
        assertThat(migration).contains("payload jsonb NOT NULL");
        assertThat(migration).contains("headers jsonb");
        assertThat(migration).contains("status varchar(32) NOT NULL DEFAULT 'NEW'");
        assertThat(migration).contains("retry_count int NOT NULL DEFAULT 0");
        assertThat(migration).contains("last_error text");
        assertThat(migration).contains("created_at timestamptz NOT NULL DEFAULT now()");
        assertThat(migration).contains("available_at timestamptz NOT NULL DEFAULT now()");
        assertThat(migration).contains("sent_at timestamptz");
        assertThat(migration).contains("idx_outbox_events_status_available_at");
        assertThat(migration).contains("ON outbox_events (status, available_at)");
        assertThat(migration).contains("idx_outbox_events_created_at");
        assertThat(migration).contains("ON outbox_events (created_at)");
        assertThat(migration).contains("idx_outbox_events_event_type");
        assertThat(migration).contains("ON outbox_events (event_type)");
    }
}
