package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var anEventId = EventId.unique();
        final var anotherEventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        final var anotherTicket = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anotherEventId));

        final var expectedCancelledTickets = 2;
        final var input = new CancelEventTicketsUseCase.Input(anEventId.value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(anEventId.value(), output.eventId());
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);
        Assertions.assertEquals(expectedCancelledTickets, actualTickets.size());
        actualTickets.forEach(it -> Assertions.assertEquals(TicketStatus.CANCELLED, it.status()));

        Assertions.assertEquals(TicketStatus.PENDING, ticketRepository.ticketOfId(anotherTicket.ticketId()).get().status());
    }

    @Test
    @DisplayName("Reprocessar o cancelamento dos tickets deve ser idempotente")
    public void testCancelEventTicketsTwice() throws Exception {
        // given
        final var anEventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));

        final var input = new CancelEventTicketsUseCase.Input(anEventId.value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(input);

        // when
        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // then
        Assertions.assertEquals(2, output.cancelledTickets());
        ticketRepository.ticketsByEventId(anEventId)
                .forEach(it -> Assertions.assertEquals(TicketStatus.CANCELLED, it.status()));
    }

    @Test
    @DisplayName("Deve funcionar quando o evento não possui tickets")
    public void testCancelEventWithoutTickets() throws Exception {
        // given
        final var input = new CancelEventTicketsUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(new InMemoryTicketRepository());
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(0, output.cancelledTickets());
    }
}
