package com.punanito.predator.service;

import com.punanito.predator.model.CurrentCandleData;
import com.punanito.predator.model.PriceRequest;
import com.punanito.predator.model.ScalperDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.punanito.predator.model.CandleColor.GREEN;
import static com.punanito.predator.model.CandleColor.RED;

@Service
public class ScalperService {

    private static final Logger logger = LoggerFactory.getLogger(ScalperService.class);

    private static final BigDecimal MAX_SPREAD = new BigDecimal("0.25");

    private static final BigDecimal MIN_BODY_ABS = new BigDecimal("1.0");
    private static final BigDecimal STRONG_BODY_ABS = new BigDecimal("1.2");

    private static final BigDecimal LONG_POSITION_IN_RANGE = new BigDecimal("0.85");
    private static final BigDecimal SHORT_POSITION_IN_RANGE = new BigDecimal("0.15");

    private static final int MIN_SECOND_OF_MINUTE = 10;

    private static final BigDecimal SL_SPREAD_MULTIPLIER = new BigDecimal("2.0");
    private static final BigDecimal TP_SPREAD_MULTIPLIER = new BigDecimal("1.5");

    private static final BigDecimal MIN_SL_DISTANCE = new BigDecimal("0.4");
    private static final BigDecimal MIN_SL_SHORT_DISTANCE = new BigDecimal("0.4");
    private static final BigDecimal MIN_TP_DISTANCE = new BigDecimal("1.0");
    private static final BigDecimal MIN_TP_SHORT_DISTANCE = new BigDecimal("1.0");

    private final MinuteCandleAggregator minuteCandleAggregator;

    /**
     * Czy strategia jest globalnie włączona.
     */
    private final AtomicBoolean scalperFeatureEnabled = new AtomicBoolean(false);

    /**
     * Czy aktualnie mamy już otwartą pozycję / zajęty slot.
     * false = można otworzyć pozycję
     * true  = pozycja już jest otwarta albo slot został zajęty
     */
    private final AtomicBoolean positionOpen = new AtomicBoolean(false);

    public ScalperService(MinuteCandleAggregator minuteCandleAggregator) {
        this.minuteCandleAggregator = minuteCandleAggregator;
    }

    public ScalperDto fireSignal(PriceRequest priceRequest) {
        CurrentCandleData currentCandleData = minuteCandleAggregator.onTick(priceRequest);
        if (currentCandleData != null) {
            logger.info(currentCandleData + " priceRequest: " + priceRequest);
        }

        if (currentCandleData == null) {
            return new ScalperDto("SKIP");
        }

        if (priceRequest.getSpread().compareTo(MAX_SPREAD) >= 0) {
            return new ScalperDto("SKIP");
        }

        if (currentCandleData.getSecondOfMinute() < MIN_SECOND_OF_MINUTE) {
            return new ScalperDto("SKIP");
        }

        if (currentCandleData.getBodyAbs().compareTo(MIN_BODY_ABS) <= 0) {
            return new ScalperDto("SKIP");
        }

        BigDecimal marketBid = currentCandleData.getCurrentBid();
        BigDecimal marketAsk = currentCandleData.getCurrentAsk();
        BigDecimal spread = priceRequest.getSpread();

        BigDecimal slDistance = spread.multiply(SL_SPREAD_MULTIPLIER).max(MIN_SL_DISTANCE);
        BigDecimal slShortDistance = spread.multiply(SL_SPREAD_MULTIPLIER).max(MIN_SL_SHORT_DISTANCE);
        BigDecimal tpDistance = spread.multiply(TP_SPREAD_MULTIPLIER).max(MIN_TP_DISTANCE);
        BigDecimal tpShortDistance = spread.multiply(TP_SPREAD_MULTIPLIER).max(MIN_TP_SHORT_DISTANCE);

        if (GREEN.equals(currentCandleData.getColor())
                && currentCandleData.getBodySigned().compareTo(MIN_BODY_ABS) > 0
                && currentCandleData.getPositionInRange().compareTo( LONG_POSITION_IN_RANGE) >= 0
                && currentCandleData.isNewHighBreak()) {

            ScalperDto scalperDto = new ScalperDto("LONG");

            BigDecimal sl = marketAsk
                    .subtract(slDistance)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal tp = marketAsk
                    .add(tpDistance)
                    .setScale(2, RoundingMode.HALF_UP);

            scalperDto.setSl(sl);
            scalperDto.setTp(tp);
            return scalperDto;
        }

        if (RED.equals(currentCandleData.getColor())
                && currentCandleData.getBodySigned().compareTo(MIN_BODY_ABS.negate()) < 0
                && currentCandleData.getPositionInRange().compareTo(SHORT_POSITION_IN_RANGE) <= 0
                && currentCandleData.isNewLowBreak()
                && currentCandleData.getBodyAbs().compareTo(STRONG_BODY_ABS) > 0) {

            ScalperDto scalperDto = new ScalperDto("SHORT");

            BigDecimal sl = marketBid
                    .add(slShortDistance)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal tp = marketBid
                    .subtract(tpShortDistance)
                    .setScale(2, RoundingMode.HALF_UP);

            scalperDto.setSl(sl);
            scalperDto.setTp(tp);
            return scalperDto;
        }

        return new ScalperDto("SKIP");
    }

//    public ScalperDto fireSignal(PriceRequest priceRequest) {
//        CurrentCandleData currentCandleData = minuteCandleAggregator.onTick(priceRequest);
//        if (currentCandleData != null) {
//            logger.info(currentCandleData + " priceRequest: " + priceRequest);
//        }
//
//
//        if (currentCandleData != null
//                && priceRequest.getSpread().compareTo(MAX_SPREAD) < 0
//                && currentCandleData.getBodyAbs().compareTo(MIN_BODY_ABS) > 0) {
//
////            if(currentCandleData.getBodyAbs().compareTo(STRONG_BODY_ABS) > 0){
////                if(isStrongCandleAndPriceInProgressZone(
////                        currentCandleData,
////                        priceRequest.getLastBid(),
////                        priceRequest.getLastAsk(),
////                        ENTRY_PROGRESS_RATIO)){
////                }
////                return new ScalperDto("SKIP");
////            }
//
//            BigDecimal marketBid = priceRequest.getLastBid()
//                    .divide(PRICE_SCALE_DIVIDER, 5, RoundingMode.HALF_UP);
//
//            BigDecimal marketAsk = priceRequest.getLastAsk()
//                    .divide(PRICE_SCALE_DIVIDER, 5, RoundingMode.HALF_UP);
//
//            BigDecimal spread = priceRequest.getSpread();
//
//            BigDecimal slDistance = spread.multiply(SL_SPREAD_MULTIPLIER).max(MIN_SL_DISTANCE);
//            BigDecimal slShortDistance = spread.multiply(SL_SPREAD_MULTIPLIER).max(MIN_SL_SHORT_DISTANCE);
//            BigDecimal tpDistance = spread.multiply(TP_SPREAD_MULTIPLIER).max(MIN_TP_DISTANCE);
//            BigDecimal tpShortDistance = spread.multiply(TP_SPREAD_MULTIPLIER).max(MIN_TP_SHORT_DISTANCE);
//
//            if (GREEN.equals(currentCandleData.getColor())) {
//                ScalperDto scalperDto = new ScalperDto("LONG");
//
//                BigDecimal sl = marketAsk
//                        .subtract(slDistance)
//                        .setScale(2, RoundingMode.HALF_UP);
//
//                BigDecimal tp = marketAsk
//                        .add(tpDistance)
//                        .setScale(2, RoundingMode.HALF_UP);
//
//                scalperDto.setSl(sl);
//                scalperDto.setTp(tp);
//                return scalperDto;
//
//            } else if (RED.equals(currentCandleData.getColor())) {
//
//                if(currentCandleData.getBodyAbs().compareTo(STRONG_BODY_ABS) > 0){
//                    ScalperDto scalperDto = new ScalperDto("SHORT");
//
//                    BigDecimal sl = marketBid
//                            .add(slShortDistance)
//                            .setScale(2, RoundingMode.HALF_UP);
//
//                    BigDecimal tp = marketBid
//                            .subtract(tpShortDistance)
//                            .setScale(2, RoundingMode.HALF_UP);
//
//                    scalperDto.setSl(sl);
//                    scalperDto.setTp(tp);
//                    return scalperDto;
//                }
//
//                return new ScalperDto("SKIP");
//            }
//        }
//
//        return new ScalperDto("SKIP");
//    }

    private boolean isStrongCandleAndPriceInProgressZone(CurrentCandleData candle,
                                                         BigDecimal marketBid,
                                                         BigDecimal marketAsk,
                                                         BigDecimal progressRatio) {
        if (candle.getBodyAbs().compareTo(STRONG_BODY_ABS) <= 0) {
            return false;
        }

        if (GREEN.equals(candle.getColor())) {
            BigDecimal open = candle.getPriceOpen();
            BigDecimal high = candle.getHigh();
            BigDecimal currentPrice = marketAsk;

            BigDecimal range = high.subtract(open);
            if (range.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }

            BigDecimal thresholdPrice = open.add(range.multiply(progressRatio));
            return currentPrice.compareTo(thresholdPrice) >= 0;
        }

        if (RED.equals(candle.getColor())) {
            BigDecimal open = candle.getPriceOpen();
            BigDecimal low = candle.getLow();
            BigDecimal currentPrice = marketBid;

            BigDecimal range = open.subtract(low);
            if (range.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }

            BigDecimal thresholdPrice = open.subtract(range.multiply(progressRatio));
            return currentPrice.compareTo(thresholdPrice) <= 0;
        }

        return false;
    }

    /**
     * Czy strategia może w ogóle pracować.
     */
    public boolean isEnabled() {
        return scalperFeatureEnabled.get() && !minuteCandleAggregator.isJavaScalperSleeping();
    }

    /**
     * Atomowa próba zajęcia slotu na nową pozycję.
     *
     * Zwraca true tylko jednemu wątkowi:
     * - jeśli strategia jest włączona
     * - jeśli nie ma sleep
     * - jeśli nie ma już otwartej pozycji
     */
    public boolean tryAcquirePositionSlot() {
        if (!scalperFeatureEnabled.get()) {
            return false;
        }

        if (minuteCandleAggregator.isJavaScalperSleeping()) {
            return false;
        }

        return positionOpen.compareAndSet(false, true);
    }

    /**
     * Zwolnienie slotu po zamknięciu pozycji albo błędzie otwarcia.
     */
    public void releasePositionSlot() {
        positionOpen.set(false);
    }

    public void acquirePositionSlot() {
        positionOpen.set(true);
    }

    /**
     * Dodatkowy helper diagnostyczny.
     */
    public boolean isPositionOpen() {
        return positionOpen.get();
    }

    public void enable() {
        scalperFeatureEnabled.set(true);
    }

    public void disable() {
        scalperFeatureEnabled.set(false);
    }

    public void sleep() {
        minuteCandleAggregator.setJavaScalperSleeping(true);
    }
}