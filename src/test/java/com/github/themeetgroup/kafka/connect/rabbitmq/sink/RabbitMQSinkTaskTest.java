package com.github.themeetgroup.kafka.connect.rabbitmq.sink;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RabbitMQSinkTaskTest {

    @Test
    public void testStart() throws IOException, TimeoutException {

        RabbitMQSinkTask sinkTask = new RabbitMQSinkTask();
        createExchangeAndQueue();
        sinkTask.start(createSettings());
        assertTrue(getBindings().stream()
                .filter(n -> n.get("source").equals("data"))
                .map(v -> v.get("routing_key"))
                .anyMatch(t -> t.equals("test_routing_key")));
    }

    private List<Map<String, Object>> inputStreamToList(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(is, List.class);
    }

    private void createExchangeAndQueue() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("django");
        factory.setPassword("django");
        factory.setVirtualHost("depop-local");
        factory.setHost("localhost");
        factory.setPort(5672);

        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare("data", "direct");
        channel.queueDeclare("test_queue", true, false, false, null);
        channel.close();
        conn.close();

    }

    private HashMap<String, String> createSettings() {
        HashMap<String, String> settings = new HashMap<>();
        settings.put("connector.class", "com.github.themeetgroup.kafka.connect.rabbitmq.sink.RabbitMQSinkConnector");
        settings.put("tasks.max", "3");
        settings.put("heartbeat.interval.ms", "2000");
        settings.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        settings.put("format.class", "io.confluent.connect.s3.format.json.JsonFormat");
        settings.put("rabbitmq.host", "localhost");
        settings.put("rabbitmq.port", "5672");
        settings.put("rabbitmq.username", "django");
        settings.put("rabbitmq.exchange", "data");
        settings.put("rabbitmq.virtual.host", "depop-local");
        settings.put("rabbitmq.ssl", "false");
        settings.put("rabbitmq.delivery.mode", "PERSISTENT");
        settings.put("rabbitmq.format", "json");
        settings.put("errors.tolerance", "all");
        settings.put("errors.log.enable", "true");
        settings.put("errors.deadletterqueue.topic.name", "test_dlq");
        settings.put("errors.deadletterqueue.topic.replication.factor", "3");
        settings.put("topics", "test_topic");
        settings.put("rabbitmq.password", "django");
        settings.put("rabbitmq.routing.key", "test_routing_key");
        settings.put("rabbitmq.queue.name", "test_queue");
        return settings;
    }

    private List<Map<String, Object>> getBindings() throws IOException {
        URL url = new URL("http://localhost:15672/api/queues/depop-local/test_queue/bindings/?username=django&password=django");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic ZGphbmdvOmRqYW5nbw==");
        InputStream responseStream = connection.getInputStream();
        return inputStreamToList(responseStream);
    }

}

