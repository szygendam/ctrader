package com.punanito.predator.service;

import com.punanito.predator.model.CandleColor;
import com.punanito.predator.model.CurrentCandleData;
import com.punanito.predator.model.PriceRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MinuteCandleAggregator {

    private final List<PriceRequest> ticks = new ArrayList<>();
    private AtomicBoolean javaScalperSleeping = new AtomicBoolean(false);

    /**
     * Początek aktualnie obsługiwanej minuty, np. 12:34:00.000
     */
    private long currentMinuteStart = -1L;

    public boolean isJavaScalperSleeping() {
        return javaScalperSleeping.get();
    }

    public void setJavaScalperSleeping(boolean javaScalperSleeping) {
        this.javaScalperSleeping.set(javaScalperSleeping);
    }

    /**
     * Przetwarza nowy tick.
     * - na pierwszym ticku nowej minuty czyści listę
     * - dodaje bieżący tick
     * - dopiero od 3 sekundy liczy parametry świecy
     *
     * @return CurrentCandleData jeśli sekunda >= 3, w przeciwnym razie null
     */
    public CurrentCandleData onTick(PriceRequest tick) {
        long tickTime = tick.getCurrentTime();
        long tickMinuteStart = truncateToMinute(tickTime);

        // Pierwszy tick nowej minuty
        if (tickMinuteStart != currentMinuteStart) {
            currentMinuteStart = tickMinuteStart;
            ticks.clear();
            javaScalperSleeping.set(false);
            ticks.add(tick);
            return null; // sekundy 0..2 i tak jeszcze nie liczysz
        }

        // Ten sam przedział minutowy - dokładamy tick
        ticks.add(tick);

        int secondOfMinute = getSecondOfMinute(tickTime);

        // Liczenie dopiero od 3 sekundy
        if (secondOfMinute < 3) {
            return null;
        }

        return calculateCurrentCandle();
    }

    private CurrentCandleData calculateCurrentCandle() {
        if (ticks.isEmpty()) {
            return null;
        }

        // Zakładam świecę z BID
        double rawOpen = ticks.get(0).getLastBid();
        double rawClose = ticks.get(ticks.size() - 1).getLastBid();

        double rawHigh = Double.NEGATIVE_INFINITY;
        double rawLow = Double.POSITIVE_INFINITY;

        for (PriceRequest tick : ticks) {
            double price = tick.getLastBid();
            if (price > rawHigh) {
                rawHigh = price;
            }
            if (price < rawLow) {
                rawLow = price;
            }
        }



        CandleColor color;
        if (rawClose > rawOpen) {
            color = CandleColor.GREEN;
        } else if (rawClose < rawOpen) {
            color = CandleColor.RED;
        } else {
            color = CandleColor.DOJI;
        }

        BigDecimal scale = new BigDecimal("100000");

        BigDecimal open = new BigDecimal(rawOpen).divide(scale);
        BigDecimal close = new BigDecimal(rawClose).divide(scale);
        BigDecimal high = new BigDecimal(rawHigh).divide(scale);
        BigDecimal low = new BigDecimal(rawLow).divide(scale);

        BigDecimal bodyAbs = open.subtract(close).abs();
        BigDecimal rangeAbs = high.subtract(low);
        BigDecimal upperWick = high.subtract(open.max(close));
        BigDecimal lowerWick = open.min(close).subtract(low);

        CurrentCandleData candle = new CurrentCandleData();
        candle.setMinuteStartTime(currentMinuteStart);
        candle.setTicksCount(ticks.size());
        candle.setPriceOpen(open);
        candle.setPriceClose(close);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setBodyAbs(bodyAbs);
        candle.setLowVsHighAbs(rangeAbs);
        candle.setUpperWick(upperWick);
        candle.setLowerWick(lowerWick);
        candle.setColor(color);

        return candle;
    }

    private long truncateToMinute(long epochMillis) {
        return (epochMillis / 60000L) * 60000L;
    }

    private int getSecondOfMinute(long epochMillis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault());
        return zdt.getSecond();
    }

    public List<PriceRequest> getTicks() {
        return ticks;
    }

    public long getCurrentMinuteStart() {
        return currentMinuteStart;
    }
}