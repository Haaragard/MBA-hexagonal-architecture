package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GetEventByIdUseCaseIT extends IntegrationTest {

    @Autowired
    private GetEventByIdUseCase useCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve obter um evento por id com seu status")
    public void testGetById() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();
        eventRepository.create(anEvent);

        final var expectedId = anEvent.eventId().value();

        // when
        final var output = useCase.execute(new GetEventByIdUseCase.Input(expectedId)).get();

        // then
        Assertions.assertEquals(expectedId, output.id());
        Assertions.assertEquals("Disney on Ice", output.name());
        Assertions.assertEquals("2021-01-01", output.date());
        Assertions.assertEquals(10, output.totalSpots());
        Assertions.assertEquals("CANCELLED", output.status());
    }

    @Test
    @DisplayName("Deve retornar vazio para um evento inexistente")
    public void testGetByIdNotFound() throws Exception {
        // when
        final var output = useCase.execute(new GetEventByIdUseCase.Input(EventId.unique().value()));

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
