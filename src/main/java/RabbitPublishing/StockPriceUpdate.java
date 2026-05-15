package RabbitPublishing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class StockPriceUpdate {
    private final String stockName;
    private final BigDecimal price;
    private final String currency;
    private final OffsetDateTime collectedAt;

    public StockPriceUpdate(String stockName, BigDecimal price, String currency, OffsetDateTime collectedAt) {
        this.stockName = stockName;
        this.price = price;
        this.currency = currency;
        this.collectedAt = collectedAt;
    }

    public String getStockName() {
        return stockName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getCollectedAt() {
        return collectedAt;
    }
}
