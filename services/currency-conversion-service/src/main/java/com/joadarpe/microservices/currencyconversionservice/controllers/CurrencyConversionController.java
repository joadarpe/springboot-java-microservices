package com.joadarpe.microservices.currencyconversionservice.controllers;

import com.joadarpe.microservices.currencyconversionservice.model.CurrencyConversion;
import com.joadarpe.microservices.currencyconversionservice.proxies.CurrencyExchangeProxy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;

@RestController
public class CurrencyConversionController {

    @Autowired
    private CurrencyExchangeProxy proxy;

    private Logger logger = LoggerFactory.getLogger(CurrencyConversionController.class);

    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    @Retry(name = "currency-conversion", fallbackMethod = "currencyConversionFallback")
    public CurrencyConversion calculateCurrencyConversionRestTemplate(
            @PathVariable String from,
            @PathVariable String to,
            @PathVariable BigDecimal quantity
    ) {

        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        logger.info("Calling currency-exchange at 8000");
        ResponseEntity<CurrencyConversion> responseEntity = new RestTemplate()
                .getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                        CurrencyConversion.class, uriVariables);

        CurrencyConversion currencyConversion = responseEntity.getBody();

        return new CurrencyConversion(currencyConversion.getId(),
                from, to, quantity,
                currencyConversion.getConversionMultiple(),
                currencyConversion.getEnvironment() + " " + "rest template");

    }

    public CurrencyConversion currencyConversionFallback(String from, String to, BigDecimal quantity, RuntimeException e) {
        var result = new CurrencyConversion();
        result.setEnvironment(String.format("Cannot get the currency-exchange service %s", e.getLocalizedMessage()));
        return result;
    }

    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
    @CircuitBreaker(name = "currency-conversion", fallbackMethod = "currencyConversionFallback")
    public CurrencyConversion calculateCurrencyConversionFeign(
            @PathVariable String from,
            @PathVariable String to,
            @PathVariable BigDecimal quantity
    ) {

        logger.info("Calling currency-exchange wit feign at load balancer");
        CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);

        return new CurrencyConversion(currencyConversion.getId(),
                from, to, quantity,
                currencyConversion.getConversionMultiple(),
                currencyConversion.getEnvironment() + " " + "feign");

    }

}