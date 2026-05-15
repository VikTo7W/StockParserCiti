package RabbitPublishing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RabbitMqPublisher {
    private static final String DEFAULT_EXCHANGE = "";

    private final RabbitMqConnectionProvider connectionProvider;
    private final RabbitMqConfig config;
    private final ObjectMapper objectMapper;

    public RabbitMqPublisher(
            RabbitMqConnectionProvider connectionProvider,
            RabbitMqConfig config
    ) {
        this.connectionProvider = connectionProvider;
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publish(Object event) {
        try (Channel channel = connectionProvider.getConnection().createChannel()) {
            declareQueue(channel);

            // Enables publisher confirms for this channel.
            channel.confirmSelect();

            byte[] body = toJsonBytes(event);

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding("UTF-8")
                    .deliveryMode(2) // 2 = persistent message
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(new Date())
                    .type(event.getClass().getSimpleName())
                    .build();

            channel.basicPublish(
                    DEFAULT_EXCHANGE,
                    config.queueName(),
                    properties,
                    body
            );

            channel.waitForConfirmsOrDie(5000);
        } catch (IOException | TimeoutException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new EventPublishException("Failed to publish event to RabbitMQ", e);
        }
    }

    private void declareQueue(Channel channel) throws IOException {
        boolean durable = true;
        boolean exclusive = false;
        boolean autoDelete = false;

        channel.queueDeclare(
                config.queueName(),
                durable,
                exclusive,
                autoDelete,
                null
        );
    }

    private byte[] toJsonBytes(Object event) {
        try {
            return objectMapper
                    .writeValueAsString(event)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to serialize event as JSON", e);
        }
    }
}
