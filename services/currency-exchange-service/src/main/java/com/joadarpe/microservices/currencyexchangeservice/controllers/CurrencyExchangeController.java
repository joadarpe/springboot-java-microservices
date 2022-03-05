package com.joadarpe.microservices.currencyexchangeservice.controllers;

import com.joadarpe.microservices.currencyexchangeservice.model.CurrencyExchange;
import com.joadarpe.microservices.currencyexchangeservice.repositories.CurrencyExchangeRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyExchangeController {

    @Autowired
    private CurrencyExchangeRepository repository;

    @Autowired
    private Environment environment;

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    @RateLimiter(name = "currency-exchange", fallbackMethod = "currencyExchangeFallback")
    @Bulkhead(name = "currency-exchange", fallbackMethod = "currencyExchangeFallback")
    public CurrencyExchange retrieveExchangeValue(
            @PathVariable String from,
            @PathVariable String to) {
        CurrencyExchange currencyExchange = repository.findByFromAndTo(from, to);

        if (currencyExchange == null) {
            throw new RuntimeException("Unable to Find data for " + from + " to " + to);
        }

        String port = environment.getProperty("local.server.port");
        currencyExchange.setEnvironment(port);

        return currencyExchange;

    }

    private CurrencyExchange currencyExchangeFallback(String from, String to, RuntimeException e) {
        var result = new CurrencyExchange();
        result.setEnvironment(String.format("To many request to currency-exchange service %s", e.getLocalizedMessage()));
        return result;
    }

}