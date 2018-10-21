package com.its.springbootactuatordemo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.core.MongoOperations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Dhaval on 13-10-2018.
 */

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BulkDataLoader {

    @Bean
    public CommandLineRunner initData(MongoOperations mongo) {
        log.info("Entering and leaving initData");
        return (String... args) -> {
            mongo.dropCollection(Reservation.class);
            log.info("Reservation collection dropped");
            mongo.createCollection(Reservation.class);
            log.info("Reservation collection created");
            loadReservationNames("/first-last-names-list-4000.txt").forEach(mongo::save);
            log.info("Bulk reservation data successfully saved in db");
        };
    }

    private List<Reservation> loadReservationNames(String resource) {
        log.info("!!!!!Entering and leaving loadReservationNames from resource {} " , resource);
        try {
            return IOUtils.readLines(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)
                    .stream()
                    .map(line -> {
                            String[] person = line.split("\\s");
                            Reservation aReservation = new Reservation(person[0], person[1]);
                            return aReservation;
                        }
                    )
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error occurred whilst reading file - Error Message : {} - Stack Trace : " +
                    "{}", e.getMessage(), e.getStackTrace());
            throw new RuntimeException(e);
        }
    }
}
