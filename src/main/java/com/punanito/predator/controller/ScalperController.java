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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable() {
        logger.info("Received scalper disable");
        scalperService.disable();
        return ResponseEntity.ok().build();
    }

}
