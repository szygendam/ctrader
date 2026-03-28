package com.punanito.predator.service;

import com.punanito.predator.model.CandleColor;
import com.punanito.predator.model.CurrentCandleData;
import com.punanito.predator.model.PriceRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinuteCandleAggregator {

    private final List<PriceRequest> ticks = new ArrayList<>();

    /**
     * Początek aktualnie obsługiwanej minuty, np. 12:34:00.000
     */
    private long currentMinuteStart = -1L;

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
        double open = ticks.get(0).getLastBid();
        double close = ticks.get(ticks.size() - 1).getLastBid();

        double high = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;

        for (PriceRequest tick : ticks) {
            double price = tick.getLastBid();
            if (price > high) {
                high = price;
            }
            if (price < low) {
                low = price;
            }
        }

        double bodyAbs = Math.abs(close - open);
        double lowVsHighAbs = Math.abs(high - low);

        double upperWick = high - Math.max(open, close);
        double lowerWick = Math.min(open, close) - low;

        CandleColor color;
        if (close > open) {
            color = CandleColor.GREEN;
        } else if (close < open) {
            color = CandleColor.RED;
        } else {
            color = CandleColor.DOJI;
        }

        CurrentCandleData candle = new CurrentCandleData();
        candle.setMinuteStartTime(currentMinuteStart);
        candle.setTicksCount(ticks.size());
        candle.setPriceOpen(open);
        candle.setPriceClose(close);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setBodyAbs(bodyAbs);
        candle.setLowVsHighAbs(lowVsHighAbs);
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