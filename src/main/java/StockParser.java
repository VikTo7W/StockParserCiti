import RabbitPublishing.RabbitMqConfig;
import RabbitPublishing.RabbitMqConnectionProvider;
import RabbitPublishing.RabbitMqPublisher;
import RabbitPublishing.StockPriceUpdate;
import com.jayway.jsonpath.JsonPath;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StockParser extends Application {

    private final XYChart.Series<Number, Number> dataPoints = new XYChart.Series<>();
    private LineChart<Number,Number> stockChart;
    @Override
    public void start(Stage stage) {
        createStockPriceChart();
        Scene scene  = new Scene(stockChart,800,600);
        stage.setScene(scene);
        stage.show();

        Thread parserThread = new Thread(this::parseStockMarketPrices);
        parserThread.setDaemon(true);
        parserThread.start();
    }

    private void parseStockMarketPrices() {
        HttpClient client;
        HttpRequest request;

        try {
            client = HttpClient.newHttpClient();
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://query1.finance.yahoo.com/v7/finance/quote?symbols=%5EDJI&crumb=iUc5aA36X%2F9"))
                    .header("Cookie", "_ebd=YmlkLTY4dW10cXBpb20yMmUmZD0wMDBlOTkyYjA5N2UwMjA0NmNjMjVmZjY5ZjIxNDBhZSZ2PTEmYj0w; _dmieu=CQkJXYAQkJXYAAOABBENCeFgAAAAAAAAACiQAAAXsgDAA4AM-AjwBKoDtgL2AAAA.IMHtB9G7eTXFneTJ2YLskOYwX0VBJ4MAwBgCAAEABzBIUIBwGVmATJEyIICACGAIAIGBBIABtGAhAQEAAIIAVAABIAEkAIBAAIGAAACAIQABACAAAAAAAEAAQgEAXMAQgmAYEBFoIQUhAkgAgAQAAAAAEAIgBCASAEAAAQAAACAAAgCgAggAAAAAAAAAEAFAIEQAAIAECAovkdgAQAAAAAAgIAAYACEABAAAAAIAAAgCAAAAAAAAAAAAAAAAAAABAAIAACAA; A1=d=AQABBE4Ii2UCELN69MevBvtXxfNY3NbdemQFEgABCAEJuWUEavbPb2UBAiAAAAcITgiLZdbdemQ&S=AQAAAqg8gJoCG3kjF6om7Jz-xRo; A3=d=AQABBE4Ii2UCELN69MevBvtXxfNY3NbdemQFEgABCAEJuWUEavbPb2UBAiAAAAcITgiLZdbdemQ&S=AQAAAqg8gJoCG3kjF6om7Jz-xRo; EuConsent=CQkJXYAQkJXYAAOADBENCeFgAAAAAAAAACiQAAAAAAAA.IMHtB9G7eTXFneTJ2YLskOYwX0VBJ4MAwBgCAAEABzBIUIBwGVmATJEyIICACGAIAIGBBIABtGAhAQEAAIIAVAABIAEkAIBAAIGAAACAIQABACAAAAAAAEAAQgEAXIAQgmAYEBFoIQUhAkgAgAQAAAAAEAIgBCASAEAAAQAAACAAAgCgAggAAAAAAAAAEAFAIEQAAIAECAovkdgAQAAAAAAgIAAYACEABAAAAAIAAAgCAAAAAAAAAAAAAAAAAAABAAIAACA; A1S=d=AQABBE4Ii2UCELN69MevBvtXxfNY3NbdemQFEgABCAEJuWUEavbPb2UBAiAAAAcITgiLZdbdemQ&S=AQAAAqg8gJoCG3kjF6om7Jz-xRo; cmp=t=1778931444&j=1&u=1---&v=132; _dmit=BGeILrwPsbfi0UBlYNbAzqUJCrUU0JCrUX6EimyzZAFAEQVABAAAAAAAAAAAAAAAAAAAAASAAEoABNoQHkAAhAAA.bid-68umtqpiom22e.eyJvIjoiYmlkLTY4dW10cXBpb20yMmUifQ==.1778931445856~AMEQCIE8xPe0e0ug1NvCWBfkaGEPJIfWcCauMWbctr6APrjlXAiBeJ17xwIg7VfIyXCajnFtiGABWFHwFaISeqtGn5NmP2w==; PRF=t%3D%255EDJI%26dock-collapsed%3Dtrue%26to%3D")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
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

                updateStockPriceChart(event);
                publisher.publish(event);

                System.out.println("Event published, chart updated");
                Thread.sleep(5000);
            } catch (Exception e) {
                throw new ParsingException("Error sending request or publishing into queue: ", e);
            }
        }
    }

    private void createStockPriceChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setLabel("Timestamp");
        yAxis.setLabel("Price($)");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                long epochMillis = value.longValue();
                return formatter.format(Instant.ofEpochMilli(epochMillis));
            }

            @Override
            public Number fromString(String string) {
                throw new UnsupportedOperationException("Parsing axis labels is not needed.");
            }
        });

        stockChart = new LineChart<>(xAxis, yAxis);
        stockChart.setTitle("Stock price over time($)");
        stockChart.getData().add(dataPoints);
    }

    private void updateStockPriceChart(StockPriceUpdate updateEvent) {
        Platform.runLater(() -> {
            dataPoints.getData().add(new XYChart.Data(updateEvent.getCollectedAt().toInstant().toEpochMilli(), updateEvent.getPrice()));
        });

        NumberAxis xAxis = (NumberAxis) stockChart.getXAxis();

        long latestEpochMillis = Instant.now().toEpochMilli();
        long lowerBound = latestEpochMillis - 60_000;
        long upperBound = latestEpochMillis + 5_000;

        xAxis.setLowerBound(lowerBound);
        xAxis.setUpperBound(upperBound);
        xAxis.setTickUnit(10_000);

        dataPoints.getData().removeIf(data ->
                data.getXValue().longValue() < lowerBound
        );
    }
}
