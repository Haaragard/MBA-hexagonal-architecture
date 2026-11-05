package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEventByIdUseCaseTest {

    @Test
    @DisplayName("Deve obter um evento ativo por id")
    public void testGetById() throws Exception {
        // given
        final var expectedName = "Disney on Ice";
        final var expectedDate = "2021-01-01";
        final var expectedTotalSpots = 10;
        final var expectedStatus = "ACTIVE";

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent(expectedName, expectedDate, expectedTotalSpots, aPartner);

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var expectedId = anEvent.eventId().value();
        final var input = new GetEventByIdUseCase.Input(expectedId);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(input).get();

        // then
        Assertions.assertEquals(expectedId, output.id());
        Assertions.assertEquals(expectedName, output.name());
        Assertions.assertEquals(expectedDate, output.date());
        Assertions.assertEquals(expectedTotalSpots, output.totalSpots());
        Assertions.assertEquals(expectedStatus, output.status());
    }

    @Test
    @DisplayName("Deve obter um evento cancelado por id")
    public void testGetCancelledById() throws Exception {
        // given
        final var expectedStatus = "CANCELLED";

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var input = new GetEventByIdUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(input).get();

        // then
        Assertions.assertEquals(expectedStatus, output.status());
    }

    @Test
    @DisplayName("Deve retornar vazio ao obter um evento não existente por id")
    public void testGetByIdWithInvalidId() throws Exception {
        // given
        final var input = new GetEventByIdUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new GetEventByIdUseCase(new InMemoryEventRepository());
        final var output = useCase.execute(input);

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
