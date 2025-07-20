package com.bmt.MyApp.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Value("${SPRING_DATASOURCE_URL:NOT_SET}")
    private String dbUrl;

    @GetMapping("/db-config")
    public String getDbConfig() {
        return "DB URL: " + dbUrl.replaceAll("password=[^&]*", "password=***");
    }
}
