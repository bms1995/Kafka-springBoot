package com.example.commonkafka;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

public final class DlqReplayTool {

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);
  private static final int MIN_REPLAY_MESSAGES = 1;
  private static final int MAX_REPLAY_MESSAGES = 100;
  private static final String EXECUTE_ARGUMENT = "--execute";
  private static final String ARGUMENT_PREFIX = "--";

  private DlqReplayTool() {}

  public static void main(String[] args) throws Exception {
    Options options = Options.parse(args);
    int matches = replay(options);
    System.out.printf(
        "%s: matched=%d source=%s target=%s key=%s operator=%s reason=%s%n",
        options.execute() ? "EXECUTED" : "DRY-RUN",
        matches,
        options.sourceTopic(),
        options.targetTopic(),
        options.key(),
        options.operator(),
        options.reason());
  }

  static int replay(Options options) throws Exception {
    Map<String, Object> consumerProperties = new HashMap<>();
    consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers());
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-replay-" + UUID.randomUUID());
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerProperties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    consumerProperties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

    Map<String, Object> producerProperties = new HashMap<>();
    producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers());
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(consumerProperties);
        KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(producerProperties)) {
      List<TopicPartition> partitions =
          consumer.partitionsFor(options.sourceTopic()).stream()
              .map(info -> new TopicPartition(info.topic(), info.partition()))
              .toList();
      if (partitions.isEmpty()) {
        throw new IllegalArgumentException(
            "Source topic has no partitions: " + options.sourceTopic());
      }
      consumer.assign(partitions);
      consumer.seekToBeginning(partitions);
      Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

      int matches = 0;
      while (matches < options.maxMessages() && !finished(consumer, endOffsets)) {
        for (ConsumerRecord<byte[], byte[]> record : consumer.poll(POLL_TIMEOUT)) {
          if (!Arrays.equals(record.key(), options.key().getBytes(StandardCharsets.UTF_8))) {
            continue;
          }
          matches++;
          System.out.printf(
              "match topic=%s partition=%d offset=%d timestamp=%d valueBytes=%d%n",
              record.topic(),
              record.partition(),
              record.offset(),
              record.timestamp(),
              record.value() == null ? 0 : record.value().length);
          if (options.execute()) {
            producer.send(toReplayRecord(record, options)).get();
          }
          if (matches >= options.maxMessages()) {
            break;
          }
        }
      }
      if (options.execute()) {
        producer.flush();
      }
      return matches;
    }
  }

  private static boolean finished(
      KafkaConsumer<byte[], byte[]> consumer, Map<TopicPartition, Long> endOffsets) {
    return endOffsets.entrySet().stream()
        .allMatch(entry -> consumer.position(entry.getKey()) >= entry.getValue());
  }

  static ProducerRecord<byte[], byte[]> toReplayRecord(
      ConsumerRecord<byte[], byte[]> source, Options options) {
    RecordHeaders headers = new RecordHeaders();
    for (Header header : source.headers()) {
      headers.add(header);
    }
    headers.add("x-dlq-source-topic", source.topic().getBytes(StandardCharsets.UTF_8));
    headers.add("x-dlq-source-partition", intBytes(source.partition()));
    headers.add(
        "x-dlq-source-offset", Long.toString(source.offset()).getBytes(StandardCharsets.UTF_8));
    headers.add("x-dlq-replayed-at", Instant.now().toString().getBytes(StandardCharsets.UTF_8));
    headers.add("x-dlq-replay-operator", options.operator().getBytes(StandardCharsets.UTF_8));
    headers.add("x-dlq-replay-reason", options.reason().getBytes(StandardCharsets.UTF_8));
    Long timestamp = source.timestamp() >= 0 ? source.timestamp() : null;
    return new ProducerRecord<>(
        options.targetTopic(), null, timestamp, source.key(), source.value(), headers);
  }

  private static byte[] intBytes(int value) {
    return Integer.toString(value).getBytes(StandardCharsets.UTF_8);
  }

  record Options(
      String bootstrapServers,
      String sourceTopic,
      String targetTopic,
      String key,
      int maxMessages,
      boolean execute,
      String operator,
      String reason) {

    static Options parse(String[] args) {
      Map<String, String> values = new HashMap<>();
      List<String> flags = new ArrayList<>();
      for (int index = 0; index < args.length; index++) {
        String argument = args[index];
        if (EXECUTE_ARGUMENT.equals(argument)) {
          flags.add(argument);
        } else if (argument.startsWith(ARGUMENT_PREFIX) && index + 1 < args.length) {
          values.put(argument, args[++index]);
        } else {
          throw new IllegalArgumentException("Invalid argument: " + argument);
        }
      }

      String source = required(values, "--source");
      if (!source.endsWith(".DLQ")) {
        throw new IllegalArgumentException("Source topic must end with .DLQ");
      }
      String target = source.substring(0, source.length() - ".DLQ".length());
      String key = required(values, "--key");
      int max = Integer.parseInt(values.getOrDefault("--max", "1"));
      if (max < MIN_REPLAY_MESSAGES || max > MAX_REPLAY_MESSAGES) {
        throw new IllegalArgumentException("--max must be between 1 and 100");
      }
      boolean execute = flags.contains(EXECUTE_ARGUMENT);
      String operator =
          values.getOrDefault("--operator", System.getProperty("user.name", "unknown"));
      String reason = values.getOrDefault("--reason", "dry-run");
      if (execute && (!values.containsKey("--operator") || !values.containsKey("--reason"))) {
        throw new IllegalArgumentException("--execute requires --operator and --reason");
      }
      return new Options(
          values.getOrDefault("--bootstrap", "localhost:29092"),
          source,
          target,
          key,
          max,
          execute,
          operator,
          reason);
    }

    private static String required(Map<String, String> values, String name) {
      String value = values.get(name);
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("Missing required argument: " + name);
      }
      return value;
    }
  }
}
