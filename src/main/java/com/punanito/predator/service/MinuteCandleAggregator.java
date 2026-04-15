package com.punanito.predator.service;

import com.punanito.predator.model.CandleColor;
import com.punanito.predator.model.CurrentCandleData;
import com.punanito.predator.model.PriceRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MinuteCandleAggregator {

    private static final BigDecimal PRICE_SCALE = new BigDecimal("100000");
    private static final int PRICE_SCALE_DIGITS = 5;

    private final List<PriceRequest> ticks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean javaScalperSleeping = new AtomicBoolean(false);

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

    public CurrentCandleData onTick(PriceRequest tick) {
        long tickTime = tick.getCurrentTime();
        long tickMinuteStart = truncateToMinute(tickTime);

        if (tickMinuteStart != currentMinuteStart) {
            currentMinuteStart = tickMinuteStart;
            ticks.clear();
            javaScalperSleeping.set(false);
            ticks.add(tick);
            return null;
        }

        ticks.add(tick);

        int secondOfMinute = getSecondOfMinute(tickTime);
        if (secondOfMinute < 3) {
            return null;
        }

        return calculateCurrentCandle();
    }

    private CurrentCandleData calculateCurrentCandle() {
        if (ticks.isEmpty()) {
            return null;
        }

        PriceRequest firstTick = ticks.get(0);
        PriceRequest lastTick = ticks.get(ticks.size() - 1);

        BigDecimal rawOpenBid = firstTick.getLastBid();
        BigDecimal rawCloseBid = lastTick.getLastBid();

        BigDecimal rawHighBid = null;
        BigDecimal rawLowBid = null;

        BigDecimal spreadMin = null;
        BigDecimal spreadMax = null;
        BigDecimal spreadSum = BigDecimal.ZERO;

        for (PriceRequest tick : ticks) {
            BigDecimal bid = tick.getLastBid();

            if (rawHighBid == null || bid.compareTo(rawHighBid) > 0) {
                rawHighBid = bid;
            }
            if (rawLowBid == null || bid.compareTo(rawLowBid) < 0) {
                rawLowBid = bid;
            }

            BigDecimal spread = tick.getSpread();
            if (spread != null) {
                if (spreadMin == null || spread.compareTo(spreadMin) < 0) {
                    spreadMin = spread;
                }
                if (spreadMax == null || spread.compareTo(spreadMax) > 0) {
                    spreadMax = spread;
                }
                spreadSum = spreadSum.add(spread);
            }
        }

        CandleColor color;
        if (rawCloseBid.compareTo(rawOpenBid) > 0) {
            color = CandleColor.GREEN;
        } else if (rawCloseBid.compareTo(rawOpenBid) < 0) {
            color = CandleColor.RED;
        } else {
            color = CandleColor.DOJI;
        }

        BigDecimal open = rawOpenBid.divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);
        BigDecimal close = rawCloseBid.divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);
        BigDecimal high = rawHighBid.divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);
        BigDecimal low = rawLowBid.divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);

        BigDecimal bodyAbs = open.subtract(close).abs();
        BigDecimal bodySigned = close.subtract(open);
        BigDecimal rangeAbs = high.subtract(low);
        BigDecimal upperWick = high.subtract(open.max(close));
        BigDecimal lowerWick = open.min(close).subtract(low);

        BigDecimal currentBid = lastTick.getLastBid().divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);
        BigDecimal currentAsk = lastTick.getLastAsk().divide(PRICE_SCALE, PRICE_SCALE_DIGITS, RoundingMode.HALF_UP);
        BigDecimal currentSpread = lastTick.getSpread();

        BigDecimal positionInRange = BigDecimal.ZERO;
        if (rangeAbs.compareTo(BigDecimal.ZERO) > 0) {
            positionInRange = close.subtract(low)
                    .divide(rangeAbs, 5, RoundingMode.HALF_UP);
        }

        BigDecimal spreadAvg = ticks.isEmpty()
                ? BigDecimal.ZERO
                : spreadSum.divide(BigDecimal.valueOf(ticks.size()), 5, RoundingMode.HALF_UP);

        boolean newHighBreak = rawCloseBid.compareTo(rawHighBid) == 0;
        boolean newLowBreak = rawCloseBid.compareTo(rawLowBid) == 0;

        int secondOfMinute = getSecondOfMinute(lastTick.getCurrentTime());

        CurrentCandleData candle = new CurrentCandleData();
        candle.setMinuteStartTime(currentMinuteStart);
        candle.setTicksCount(ticks.size());

        candle.setPriceOpen(open);
        candle.setPriceClose(close);
        candle.setHigh(high);
        candle.setLow(low);

        candle.setBodyAbs(bodyAbs);
        candle.setBodySigned(bodySigned);
        candle.setLowVsHighAbs(rangeAbs);
        candle.setUpperWick(upperWick);
        candle.setLowerWick(lowerWick);
        candle.setColor(color);

        candle.setPositionInRange(positionInRange);
        candle.setSecondOfMinute(secondOfMinute);

        candle.setCurrentBid(currentBid);
        candle.setCurrentAsk(currentAsk);
        candle.setCurrentSpread(currentSpread);

        candle.setSpreadMin(spreadMin);
        candle.setSpreadMax(spreadMax);
        candle.setSpreadAvg(spreadAvg);

        candle.setNewHighBreak(newHighBreak);
        candle.setNewLowBreak(newLowBreak);

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