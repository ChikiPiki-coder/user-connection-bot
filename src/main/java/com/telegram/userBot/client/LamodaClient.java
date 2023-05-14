package com.telegram.userBot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lamodaClient", url = "${clients.lamoda.base-url}")
public interface LamodaClient {

    @GetMapping
    ResponseEntity<Void> getProductData(@RequestParam String sku);
}