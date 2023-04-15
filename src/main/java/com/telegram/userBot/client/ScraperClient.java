package com.telegram.userBot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.telegram.userBot.dto.TargetRequest;

@FeignClient(name = "scraperClient", url = "${clients.scraper.base-url}")
public interface ScraperClient {

    @GetMapping
    ResponseEntity<Void> getProductData(@RequestParam String sku);

    @PostMapping(value = "/add-target",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> addProduct(@RequestBody TargetRequest targetRequest);



}