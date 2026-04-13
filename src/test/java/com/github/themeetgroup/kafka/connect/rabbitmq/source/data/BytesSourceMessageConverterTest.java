package com.github.themeetgroup.kafka.connect.rabbitmq.source.data;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BytesSourceMessageConverterTest {

  private BytesSourceMessageConverter converter;
  private Envelope envelope;

  @BeforeEach
  void setUp() {
    converter = new BytesSourceMessageConverter();
    envelope = new Envelope(1L, false, "exchange", "routingKey");
  }

  @Test
  public void headersContainsAmqpStruct() {
    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().build();

    Headers headers = converter.headers("consumerTag", envelope, props, new byte[0]);

    Header amqpHeader = headers.lastWithName("amqp");
    assertNotNull(amqpHeader, "amqp struct header should be present");
  }

  @Test
  public void headersPromotesIndividualAmqpHeaders() {
    Map<String, Object> amqpHeaders = new HashMap<>();
    amqpHeaders.put("x-b3-traceid", "382ff905bed13c44");
    amqpHeaders.put("x-b3-spanid", "c203ee8ae949655c");
    amqpHeaders.put("x-b3-sampled", "0");
    amqpHeaders.put("x-datadog-trace-id", "4048728393100901444");
    amqpHeaders.put("x-datadog-parent-id", "9876543210987654321");

    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .headers(amqpHeaders)
        .build();

    Headers headers = converter.headers("consumerTag", envelope, props, new byte[0]);

    // amqp struct should still be present
    assertNotNull(headers.lastWithName("amqp"), "amqp struct header should be present");

    // Each AMQP header should be promoted to a top-level Kafka header
    for (Map.Entry<String, Object> entry : amqpHeaders.entrySet()) {
      Header header = headers.lastWithName(entry.getKey());
      assertNotNull(header, "Header '" + entry.getKey() + "' should be present");
      assertEquals(entry.getValue().toString(), header.value(),
          "Header '" + entry.getKey() + "' value should match");
    }
  }

  @Test
  public void headersHandlesNullHeadersMap() {
    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .headers(null)
        .build();

    Headers headers = converter.headers("consumerTag", envelope, props, new byte[0]);

    assertNotNull(headers, "headers should not be null");
    assertNotNull(headers.lastWithName("amqp"), "amqp struct header should be present");
  }

  @Test
  public void headersOnlyPromotesProvidedHeaders() {
    Map<String, Object> amqpHeaders = new HashMap<>();
    amqpHeaders.put("x-b3-traceid", "382ff905bed13c44");

    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .headers(amqpHeaders)
        .build();

    Headers headers = converter.headers("consumerTag", envelope, props, new byte[0]);

    assertNotNull(headers.lastWithName("x-b3-traceid"), "Provided header should be present");
    assertNull(headers.lastWithName("x-not-present"), "Non-existent header should not be present");
  }
}
