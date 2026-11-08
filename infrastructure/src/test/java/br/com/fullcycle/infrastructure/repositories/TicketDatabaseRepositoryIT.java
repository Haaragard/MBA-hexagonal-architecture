package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar somente os tickets de um evento")
    public void testTicketsByEventId() throws Exception {
        // given
        final var anEventId = EventId.unique();
        final var t1 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        final var t2 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), EventId.unique()));

        final var expectedIds = Set.of(t1.ticketId(), t2.ticketId());

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);

        // then
        Assertions.assertEquals(expectedIds, actualTickets.stream().map(Ticket::ticketId).collect(Collectors.toSet()));
        actualTickets.forEach(it -> Assertions.assertEquals(anEventId, it.eventId()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o evento não possui tickets")
    public void testTicketsByEventIdEmpty() throws Exception {
        // when
        final var actualTickets = ticketRepository.ticketsByEventId(EventId.unique());

        // then
        Assertions.assertTrue(actualTickets.isEmpty());
    }

    @Test
    @DisplayName("Deve persistir o status CANCELLED de um ticket")
    public void testUpdateCancelledTicket() throws Exception {
        // given
        final var aTicket = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), EventId.unique()));
        aTicket.cancel();

        // when
        ticketRepository.update(aTicket);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(aTicket.ticketId()).get().status());
    }
}
