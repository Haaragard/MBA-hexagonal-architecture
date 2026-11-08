package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class EventResolverTest {

    @Autowired
    private HttpGraphQlTester graphQlTester;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    private Partner disney;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();

        disney = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
    }

    @Test
    @DisplayName("Deve criar um evento (regressão do schema)")
    public void testCreateEvent() {
        final var eventId = createEvent();

        Assertions.assertTrue(eventRepository.eventOfId(EventId.with(eventId)).isPresent());
    }

    @Test
    @DisplayName("Deve obter um evento por id com o status")
    public void testEventOfId() {
        final var eventId = createEvent();

        graphQlTester.document("""
                        query ($id: ID!) {
                          eventOfId(id: $id) { id name date totalSpots status }
                        }
                        """)
                .variable("id", eventId)
                .execute()
                .path("eventOfId.id").entity(String.class).isEqualTo(eventId)
                .path("eventOfId.name").entity(String.class).isEqualTo("Disney on Ice")
                .path("eventOfId.date").entity(String.class).isEqualTo("2021-01-01")
                .path("eventOfId.totalSpots").entity(Integer.class).isEqualTo(100)
                .path("eventOfId.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Deve retornar null ao obter um evento inexistente")
    public void testEventOfIdNotFound() {
        graphQlTester.document("""
                        query ($id: ID!) {
                          eventOfId(id: $id) { id status }
                        }
                        """)
                .variable("id", EventId.unique().value())
                .execute()
                .path("eventOfId").valueIsNull();
    }

    @Test
    @DisplayName("Deve cancelar um evento")
    public void testCancelEvent() {
        final var eventId = createEvent();

        graphQlTester.document("""
                        mutation ($id: ID!) {
                          cancelEvent(id: $id) { id status }
                        }
                        """)
                .variable("id", eventId)
                .execute()
                .path("cancelEvent.id").entity(String.class).isEqualTo(eventId)
                .path("cancelEvent.status").entity(String.class).isEqualTo("CANCELLED");

        Assertions.assertTrue(eventRepository.eventOfId(EventId.with(eventId)).get().isCancelled());
    }

    @Test
    @DisplayName("Deve retornar erro ao cancelar um evento já cancelado")
    public void testCancelEventTwice() {
        final var eventId = createEvent();
        final var document = """
                mutation ($id: ID!) {
                  cancelEvent(id: $id) { id status }
                }
                """;

        graphQlTester.document(document).variable("id", eventId).executeAndVerify();

        graphQlTester.document(document)
                .variable("id", eventId)
                .execute()
                .errors()
                .satisfy(errors -> Assertions.assertEquals(1, errors.size()));
    }

    private String createEvent() {
        return graphQlTester.document("""
                        mutation ($input: EventInput) {
                          createEvent(input: $input) { id name date totalSpots }
                        }
                        """)
                .variable("input", Map.of(
                        "name", "Disney on Ice",
                        "date", "2021-01-01",
                        "totalSpots", 100,
                        "partnerId", disney.partnerId().value()
                ))
                .execute()
                .path("createEvent.id").entity(String.class).get();
    }
}
