package com.punanito.predator.service;

import com.punanito.predator.model.CurrentCandleData;
import com.punanito.predator.model.PriceRequest;
import com.punanito.predator.model.ScalperDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.punanito.predator.model.CandleColor.GREEN;
import static com.punanito.predator.model.CandleColor.RED;

@Service
public class ScalperService {

    private final MinuteCandleAggregator minuteCandleAggregator;

    private AtomicBoolean javaScalperEnabled = new AtomicBoolean(false);

    List<PriceRequest> ticks = new ArrayList<>();

    public ScalperService(MinuteCandleAggregator minuteCandleAggregator) {
        this.minuteCandleAggregator = minuteCandleAggregator;
    }

    public ScalperDto fireSignal(PriceRequest priceRequest) {
        CurrentCandleData currentCandleData = minuteCandleAggregator.onTick(priceRequest);

        if (currentCandleData != null && currentCandleData.getBodyAbs() > 0.3 && priceRequest.getSpread() < 0.5) {

            if (GREEN.equals(currentCandleData.getColor())) {
                ScalperDto scalperDto = new ScalperDto("LONG");
                scalperDto.setSl(priceRequest.getLastAsk() - 1.6);
                scalperDto.setTp(priceRequest.getLastAsk() + 0.6);
                return scalperDto;
            } else if (RED.equals(currentCandleData.getColor())) {
                ScalperDto scalperDto = new ScalperDto("SHORT");
                scalperDto.setSl(priceRequest.getLastBid() + 1.6);
                scalperDto.setTp(priceRequest.getLastBid() - 0.6);
                return scalperDto;
            }
        }
         return new ScalperDto("SKIP");
    }

    public boolean isEnabled(){
        return javaScalperEnabled.get() && !minuteCandleAggregator.isJavaScalperSleeping();
    }

    public void enable() {
        javaScalperEnabled.set(true);
    }

    public void disable() {
        javaScalperEnabled.set(false);
    }

    public void sleep() {
        minuteCandleAggregator.setJavaScalperSleeping(true);
    }

    public static double calcProgressPercent(
            String operation,
            double priceOpen,
            double high,
            double low,
            double lastBid
    ) {
        double percent;

        switch (operation) {
            case "LONG":
                double longRange = high - priceOpen;
                if (longRange <= 0.0) {
                    return 0.0;
                }
                percent = ((lastBid - priceOpen) / longRange) * 100.0;
                break;

            case "SHORT":
                double shortRange = priceOpen - low;
                if (shortRange <= 0.0) {
                    return 0.0;
                }
                percent = ((priceOpen - lastBid) / shortRange) * 100.0;
                break;

            default:
                throw new IllegalArgumentException("Unsupported side: " + operation);
        }

        return Math.max(0.0, Math.min(100.0, percent));
    }
}
