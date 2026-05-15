import RabbitPublishing.RabbitMqConfig;
import RabbitPublishing.RabbitMqConnectionProvider;
import RabbitPublishing.RabbitMqPublisher;
import RabbitPublishing.StockPriceUpdate;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;

public class StockParser {

    private static void parseStockMarketPrices() {
        HttpClient client;
        HttpRequest request;

        try {
            client = HttpClient.newHttpClient();
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://query1.finance.yahoo.com/v7/finance/quote?symbols=%5EDJI&crumb=iUc5aA36X%2F9"))
                    .header("Cookie", "")
                    .header("user-agent", "")
                    .GET()
                    .build();
        } catch (Exception e) {
            throw new ParsingException("Error setting up http client or request: ", e);
        }

        RabbitMqConfig config = RabbitMqConfig.fromEnvironment();

        while (true) {
            try (RabbitMqConnectionProvider connectionProvider = new RabbitMqConnectionProvider(config)) {

                HttpResponse<String> djiQuote = client.send(request, HttpResponse.BodyHandlers.ofString());

                RabbitMqPublisher publisher = new RabbitMqPublisher(connectionProvider, config);

                StockPriceUpdate event = new StockPriceUpdate(
                        JsonPath.read(djiQuote.body(), "$.quoteResponse.result[0].longName").toString(),
                        new BigDecimal(JsonPath.read(djiQuote.body(), "$.quoteResponse.result[0].regularMarketPrice").toString()),
                        JsonPath.read(djiQuote.body(), "$.quoteResponse.result[0].currency").toString(),
                        OffsetDateTime.now()
                );

                publisher.publish(event);

                System.out.println("Event published.");
                Thread.sleep(5000);
            } catch (Exception e) {
                throw new ParsingException("Error sending request or publishing into queue: ", e);
            }
        }
    }
    public static void main(String[] args) {
        parseStockMarketPrices();
    }
}
