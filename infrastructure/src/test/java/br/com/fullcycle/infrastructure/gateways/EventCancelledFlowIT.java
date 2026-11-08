package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;

/**
 * Prova de ponta a ponta do fluxo assíncrono:
 * CancelEventUseCase -> EventCancelled na outbox -> fila (ConsumerQueueGateway) -> CancelEventTicketsUseCase.
 */
class EventCancelledFlowIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase cancelEventUseCase;

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("O EventCancelled gravado na outbox, publicado no ConsumerQueueGateway, deve cancelar os tickets do evento")
    public void testEventCancelledPublishedOnQueueCancelsTickets() throws Exception {
        // given
        final var anEvent = createEvent();
        final var t1 = createTicket(anEvent.eventId());
        final var t2 = createTicket(anEvent.eventId());
        final var t3 = createTicket(anEvent.eventId());
        final var anotherEventTicket = createTicket(EventId.unique());

        cancelEventUseCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        final var eventCancelledJson = StreamSupport.stream(outboxJpaRepository.findAll().spliterator(), false)
                .map(OutboxEntity::content)
                .filter(it -> it.contains("event.cancelled") && it.contains(anEvent.eventId().value()))
                .findFirst()
                .orElseThrow();

        // when
        consumerQueueGateway.publish(eventCancelledJson);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, statusOf(t1));
        Assertions.assertEquals(TicketStatus.CANCELLED, statusOf(t2));
        Assertions.assertEquals(TicketStatus.CANCELLED, statusOf(t3));
        Assertions.assertEquals(TicketStatus.PENDING, statusOf(anotherEventTicket));

        // reprocessar a mesma mensagem é idempotente
        Assertions.assertDoesNotThrow(() -> consumerQueueGateway.publish(eventCancelledJson));
        ticketRepository.ticketsByEventId(anEvent.eventId())
                .forEach(it -> Assertions.assertEquals(TicketStatus.CANCELLED, it.status()));
        Assertions.assertEquals(TicketStatus.PENDING, statusOf(anotherEventTicket));
    }

    @Test
    @DisplayName("O OutboxRelay deve publicar o EventCancelled e a cascata deve cancelar os tickets do evento")
    public void testOutboxRelayCancelsTickets() throws Exception {
        // given
        final var anEvent = createEvent();
        final var t1 = createTicket(anEvent.eventId());
        final var t2 = createTicket(anEvent.eventId());
        final var anotherEventTicket = createTicket(EventId.unique());

        // when
        cancelEventUseCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        final var cascaded = waitUntil(Duration.ofSeconds(15),
                () -> statusOf(t1) == TicketStatus.CANCELLED && statusOf(t2) == TicketStatus.CANCELLED);

        Assertions.assertTrue(cascaded, "Os tickets do evento deveriam terminar CANCELLED via outbox + relay");
        Assertions.assertEquals(TicketStatus.PENDING, statusOf(anotherEventTicket));
    }

    private Event createEvent() {
        final var aPartner = partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));
        return eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));
    }

    private Ticket createTicket(final EventId anEventId) {
        return ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
    }

    private TicketStatus statusOf(final Ticket aTicket) {
        return ticketRepository.ticketOfId(aTicket.ticketId()).orElseThrow().status();
    }

    private static boolean waitUntil(final Duration timeout, final BooleanSupplier condition) throws InterruptedException {
        final var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(200);
        }
        return condition.getAsBoolean();
    }
}
