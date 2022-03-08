package com.joadarpe.microservices.currencyexchangeservice.controllers;

import com.joadarpe.microservices.currencyexchangeservice.model.CurrencyExchange;
import com.joadarpe.microservices.currencyexchangeservice.repositories.CurrencyExchangeRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Logger logger = LoggerFactory.getLogger(CurrencyExchangeRepository.class);

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    @RateLimiter(name = "currency-exchange", fallbackMethod = "currencyExchangeFallback")
    @Bulkhead(name = "currency-exchange", fallbackMethod = "currencyExchangeFallback")
    public CurrencyExchange retrieveExchangeValue(
            @PathVariable String from,
            @PathVariable String to) {

        logger.info("retrieveExchangeValue called with from {} to {}", from, to);

        CurrencyExchange currencyExchange = repository.findByFromAndTo(from, to);

        if (currencyExchange == null) {
            throw new RuntimeException("Unable to Find data for " + from + " to " + to);
        }

        String port = environment.getProperty("local.server.port");
        String host = environment.getProperty("HOSTNAME");
        String version = "v2";
        currencyExchange.setEnvironment(String.format("%s %s %s", port, host, version));

        return currencyExchange;

    }

    private CurrencyExchange currencyExchangeFallback(String from, String to, RuntimeException e) {
        var result = new CurrencyExchange();
        result.setEnvironment(String.format("To many request to currency-exchange service %s", e.getLocalizedMessage()));
        return result;
    }

}