package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.List;

@RestController
@SpringBootApplication
public class Main {

    @Autowired
    private StatsClient statsClient;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}