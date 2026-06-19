package com.example.commonkafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class DlqReplayToolTest {

  @Test
  void defaultsToSafeDryRunAndDerivesTargetTopic() {
    DlqReplayTool.Options options =
        DlqReplayTool.Options.parse(new String[] {"--source", "order-created.DLQ", "--key", "o-1"});

    assertThat(options.execute()).isFalse();
    assertThat(options.targetTopic()).isEqualTo("order-created");
    assertThat(options.maxMessages()).isEqualTo(1);
  }

  @Test
  void executeRequiresAuditInformation() {
    assertThatThrownBy(
            () ->
                DlqReplayTool.Options.parse(
                    new String[] {"--source", "order-created.DLQ", "--key", "o-1", "--execute"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--operator and --reason");
  }

  @Test
  void rejectsNonDlqTopicsAndBroadReplayLimits() {
    assertThatThrownBy(
            () ->
                DlqReplayTool.Options.parse(
                    new String[] {"--source", "order-created", "--key", "o-1"}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                DlqReplayTool.Options.parse(
                    new String[] {"--source", "order-created.DLQ", "--key", "o-1", "--max", "101"}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void replayRecordPreservesBinaryDataAndAddsAuditHeaders() {
    ConsumerRecord<byte[], byte[]> source =
        new ConsumerRecord<>(
            "order-created.DLQ",
            2,
            42L,
            "o-1".getBytes(StandardCharsets.UTF_8),
            new byte[] {0, 1, 2, 10, 13});
    DlqReplayTool.Options options =
        DlqReplayTool.Options.parse(
            new String[] {
              "--source",
              "order-created.DLQ",
              "--key",
              "o-1",
              "--execute",
              "--operator",
              "alice",
              "--reason",
              "incident-42"
            });

    var replay = DlqReplayTool.toReplayRecord(source, options);

    assertThat(replay.topic()).isEqualTo("order-created");
    assertThat(replay.key()).isEqualTo(source.key());
    assertThat(replay.value()).isEqualTo(source.value());
    assertThat(replay.headers().lastHeader("x-dlq-replay-operator").value())
        .isEqualTo("alice".getBytes(StandardCharsets.UTF_8));
  }
}
