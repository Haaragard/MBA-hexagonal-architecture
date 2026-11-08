package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;

public class EventTest {

    @Test
    @DisplayName("Deve criar um evento")
    public void testCreateEvent() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var expectedDate = "2021-01-01";
        final var expectedName = "Disney on Ice";
        final var expectedTotalSpots = 10;
        final var expectedPartnerId = aPartner.partnerId().value();
        final var expectedTickets = 0;

        // when
        final var actualEvent = Event.newEvent(expectedName, expectedDate, expectedTotalSpots, aPartner);

        // then
        Assertions.assertNotNull(actualEvent.eventId());
        Assertions.assertEquals(expectedDate, actualEvent.date().format(DateTimeFormatter.ISO_LOCAL_DATE));
        Assertions.assertEquals(expectedName, actualEvent.name().value());
        Assertions.assertEquals(expectedTotalSpots, actualEvent.totalSpots());
        Assertions.assertEquals(expectedPartnerId, actualEvent.partnerId().value());
        Assertions.assertEquals(expectedTickets, actualEvent.allTickets().size());
    }

    @Test
    @DisplayName("Não deve criar um evento com nome inválido")
    public void testCreateEventWithInvalidName() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var expectedError = "Invalid value for Name";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> Event.newEvent(null, "2021-01-01", 10, aPartner)
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve criar um evento com data inválido")
    public void testCreateEventWithInvalidDate() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var expectedError = "Invalid date for Event";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> Event.newEvent("Disney", "20210101", 10, aPartner)
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Deve reservar um ticket quando é possível")
    public void testReserveTicket() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var expectedCustomerId = aCustomer.customerId();
        final var expectedDate = "2021-01-01";
        final var expectedName = "Disney on Ice";
        final var expectedTotalSpots = 10;
        final var expectedPartnerId = aPartner.partnerId().value();
        final var expectedTickets = 1;
        final var expectedTicketOrder = 1;
        final var expectedDomainEvent = "event-ticket.reserved";

        final var actualEvent = Event.newEvent(expectedName, expectedDate, expectedTotalSpots, aPartner);

        final var expectedEventId = actualEvent.eventId();

        // when
        final var actualTicket = actualEvent.reserveTicket(aCustomer.customerId());

        // then
        Assertions.assertNotNull(actualTicket.eventTicketId());
        Assertions.assertNull(actualTicket.ticketId());
        Assertions.assertEquals(expectedEventId, actualTicket.eventId());
        Assertions.assertEquals(expectedCustomerId, actualTicket.customerId());

        Assertions.assertEquals(expectedDate, actualEvent.date().format(DateTimeFormatter.ISO_LOCAL_DATE));
        Assertions.assertEquals(expectedName, actualEvent.name().value());
        Assertions.assertEquals(expectedTotalSpots, actualEvent.totalSpots());
        Assertions.assertEquals(expectedPartnerId, actualEvent.partnerId().value());
        Assertions.assertEquals(expectedTickets, actualEvent.allTickets().size());

        final var actualEventTicket = actualEvent.allTickets().iterator().next();
        Assertions.assertEquals(expectedTicketOrder, actualEventTicket.ordering());
        Assertions.assertEquals(expectedEventId, actualEventTicket.eventId());
        Assertions.assertEquals(expectedCustomerId, actualEventTicket.customerId());
        Assertions.assertEquals(actualTicket.ticketId(), actualEventTicket.ticketId());

        final var actualDomainEvents = actualEvent.allDomainEvents().iterator().next();
        Assertions.assertEquals(expectedDomainEvent, actualDomainEvents.type());
    }

    @Test
    @DisplayName("Não deve reservar um ticket quando o evento está esgotado")
    public void testReserveTicketWhenEventIsSoldOut() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var aCustomer2 =
                Customer.newCustomer("John1 Doe", "111.456.789-01", "john1.doe@gmail.com");


        final var expectedTotalSpots = 1;
        final var expectedError = "Event sold out";

        final var actualEvent = Event.newEvent("Disney on Ice", "2021-01-01", expectedTotalSpots, aPartner);

        actualEvent.reserveTicket(aCustomer.customerId());

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.reserveTicket(aCustomer2.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve reservar dois tickets para um mesmo cliente")
    public void testReserveTwoTicketsForTheSameClient() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var expectedTotalSpots = 1;
        final var expectedError = "Email already registered";

        final var actualEvent = Event.newEvent("Disney on Ice", "2021-01-01", expectedTotalSpots, aPartner);

        actualEvent.reserveTicket(aCustomer.customerId());

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.reserveTicket(aCustomer.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Um evento novo deve nascer ativo")
    public void testNewEventIsActive() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        // when
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        // then
        Assertions.assertEquals(EventStatus.ACTIVE, anEvent.status());
        Assertions.assertFalse(anEvent.isCancelled());
        Assertions.assertTrue(anEvent.allDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Deve cancelar um evento e registrar o EventCancelled")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedDomainEventType = "event.cancelled";
        final var expectedEventId = anEvent.eventId().value();

        // when
        anEvent.cancel();

        // then
        Assertions.assertEquals(EventStatus.CANCELLED, anEvent.status());
        Assertions.assertTrue(anEvent.isCancelled());
        Assertions.assertEquals(1, anEvent.allDomainEvents().size());

        final var actualDomainEvent = (EventCancelled) anEvent.allDomainEvents().iterator().next();
        Assertions.assertNotNull(actualDomainEvent.domainEventId());
        Assertions.assertNotNull(actualDomainEvent.occurredOn());
        Assertions.assertEquals(expectedDomainEventType, actualDomainEvent.type());
        Assertions.assertEquals(expectedEventId, actualDomainEvent.eventId());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelEventTwice() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();

        final var expectedError = "Event already cancelled";

        // when
        final var actualError = Assertions.assertThrows(ValidationException.class, anEvent::cancel);

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
        Assertions.assertEquals(1, anEvent.allDomainEvents().size());
    }

    @Test
    @DisplayName("Não deve reservar ticket em um evento cancelado")
    public void testReserveTicketOnCancelledEvent() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();

        final var expectedError = "Event is cancelled";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> anEvent.reserveTicket(aCustomer.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
        Assertions.assertTrue(anEvent.allTickets().isEmpty());
    }

    @Test
    @DisplayName("Deve restaurar um evento cancelado")
    public void testRestoreCancelledEvent() throws Exception {
        // given
        final var expectedId = EventId.unique().value();

        // when
        final var anEvent = Event.restore(
                expectedId, "Disney on Ice", "2021-01-01", 10,
                PartnerId.unique().value(), "CANCELLED", null
        );

        // then
        Assertions.assertEquals(EventStatus.CANCELLED, anEvent.status());
        Assertions.assertTrue(anEvent.allDomainEvents().isEmpty());
    }
}
