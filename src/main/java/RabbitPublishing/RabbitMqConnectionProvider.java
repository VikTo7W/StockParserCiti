package RabbitPublishing;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMqConnectionProvider implements AutoCloseable {
    private final Connection connection;

    public RabbitMqConnectionProvider(RabbitMqConfig config) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.host());
            factory.setPort(config.port());
            factory.setUsername(config.username());
            factory.setPassword(config.password());
            factory.setVirtualHost(config.virtualHost());

            // Helpful for simple apps.
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);

            this.connection = factory.newConnection("default-java-rabbitmq-publisher");
        } catch (IOException | TimeoutException e) {
            throw new EventPublishException("Failed to create RabbitMQ connection", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new EventPublishException("Failed to close RabbitMQ connection", e);
        }
    }
}
