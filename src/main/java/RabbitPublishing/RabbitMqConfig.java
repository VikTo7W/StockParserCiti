package RabbitPublishing;

public class RabbitMqConfig {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String virtualHost;
    private final String queueName;

    public RabbitMqConfig(
            String host,
            int port,
            String username,
            String password,
            String virtualHost,
            String queueName
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
        this.queueName = queueName;
    }

    public static RabbitMqConfig fromEnvironment() {
        return new RabbitMqConfig(
                getenvOrDefault("RABBITMQ_HOST", "localhost"),
                Integer.parseInt(getenvOrDefault("RABBITMQ_PORT", "5672")),
                getenvOrDefault("RABBITMQ_USERNAME", "guest"),
                getenvOrDefault("RABBITMQ_PASSWORD", "guest"),
                getenvOrDefault("RABBITMQ_VHOST", "/"),
                getenvOrDefault("RABBITMQ_QUEUE", "rabbitmq.StockParser.queues.stockUpdates")
        );
    }

    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String virtualHost() {
        return virtualHost;
    }

    public String queueName() {
        return queueName;
    }
}
