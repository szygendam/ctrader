package com.punanito.predator.controller;

import com.punanito.predator.service.ScalperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/scalper")
public class ScalperController {

    private static final Logger logger = LoggerFactory.getLogger(ScalperController.class);

    private final ScalperService scalperService;

    public ScalperController(ScalperService scalperService) {
        this.scalperService = scalperService;
    }

    @PostMapping("/enable")
    public ResponseEntity<Void> enable() {
        logger.info("Received scalper enable");
        scalperService.enable();
        scalperService.releasePositionSlot();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enableV2")
    public ResponseEntity<Void> enableV2() {
        logger.info("Received scalper V2 enable");
        scalperService.enable();
        scalperService.releasePositionSlotV2();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enableV3")
    public ResponseEntity<Void> enableV3() {
        logger.info("Received scalper V3 enable");
        scalperService.enable();
        scalperService.releasePositionSlotV3();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable() {
        logger.info("Received scalper disable");
        scalperService.disable();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sleep")
    public ResponseEntity<Void> sleep() {
        logger.info("Received scalper sleep");
        scalperService.sleep();
        return ResponseEntity.ok().build();
    }

}
