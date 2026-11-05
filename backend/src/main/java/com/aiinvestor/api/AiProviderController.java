package com.aiinvestor.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/providers")
public class AiProviderController {

    @GetMapping
    public void providers() {
        throw NotBuiltYet.error();
    }

    @PutMapping
    public void updateProviders() {
        throw NotBuiltYet.error();
    }
}
