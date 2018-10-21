package com.its.springbootactuatordemo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.UUID;

@EnableReactiveMongoRepositories
@SpringBootApplication
public class SpringBootActuatorDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootActuatorDemoApplication.class, args);
	}
}

@Data
@AllArgsConstructor
//@NoArgsConstructor
@Document(collection = "reservation_db")
@JsonIgnoreProperties(ignoreUnknown = true)
class Reservation implements Serializable{
	@NonNull
	@Id
	private String id;

	@NonNull
	@TextIndexed
	private String firstName;

	@NonNull
	@TextIndexed
	private String lastName;

	public Reservation(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}

@Repository
interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {
	Flux<Reservation> findAll();

	Flux<Reservation> findByLastNameIgnoringCase(String lastName);

	Flux<Reservation> findByFirstNameIgnoringCase(String firstName);

	Mono<Reservation> findById(String reservationId);
}

@RestController
@Slf4j
class ReservationController {

	private final ReservationRepository repository;

	@Autowired
	private CreateReservationSource createReservationSource;

	public ReservationController(ReservationRepository repository) {

		log.info("Entering and leaving constructor of ReservationController after setting" +
				"repository");
		this.repository = repository;
	}

	@GetMapping(path = "/reservations")
	public Flux<Reservation> all() {
		log.info("Entering all with no arguments from API request");
		return this.repository.findAll();
	}

	@GetMapping(path = "/reservation/firstName/{firstName}")
	public Flux<Reservation> byFirstName(@PathVariable String firstName) {
		log.info("Entering byFirstName with {} as PathVariable ", firstName);
		return repository
				.findByFirstNameIgnoringCase(firstName)
				.switchIfEmpty(Mono.error(new Exception("No Reservation found with firstName Containing : " + firstName)));
	}

	@GetMapping(path = "/reservation/lastName/{lastName}")
	public Flux<Reservation> byLastName(@PathVariable String lastName) {
		log.info("Entering byLastName with {} as PathVariable ", lastName);
		return repository
				.findByLastNameIgnoringCase(lastName)
				.switchIfEmpty(Mono.error(new Exception("No Reservation found with lastName Containing : " + lastName)));
	}

	@PostMapping(path = "/reservation")
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@RequestBody Reservation aReservation) {
		log.info("Insert Reservation with aReservation : {} ^^^^^  ", aReservation);
		aReservation.setId(UUID.randomUUID().toString());
		createReservationSource.addReservation().send(MessageBuilder.withPayload(aReservation).build());
		log.info("Message sent to consumer");
		//return this.repository.insert(aReservation);
	}
}

@EnableBinding(CreateReservationSource.class)
class ReservationPublisher {

}

interface CreateReservationSource {
	@Output("addReservationChannel")
	MessageChannel addReservation();
}
