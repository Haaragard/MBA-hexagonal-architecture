# MBA Hexagonal Architecture — Plataforma de Ingressos

Plataforma de ingressos do MBA Full Cycle: parceiros criam eventos, clientes se inscrevem neles e os ingressos são gerados de forma assíncrona. O projeto segue Arquitetura Hexagonal e Clean Architecture e é dividido em três módulos Gradle:

- `domain`: agregados, objetos de valor e eventos de domínio, sem framework.
- `application`: casos de uso, que dependem apenas de portas (interfaces).
- `infrastructure`: Spring Boot, com REST, GraphQL, JPA, outbox e fila.

## Pré-requisitos

- Docker e Docker Compose

Não é preciso instalar Java nem Gradle na máquina: o build, a execução e os testes rodam em contêineres com JDK 17.

## Subindo o projeto

```bash
docker compose up --build
```

Esse comando sobe o MySQL 8 (banco `events`, usuário `root`/`root`) e a aplicação na porta `8080`. A aplicação só inicia depois que o MySQL passa no healthcheck. O schema é criado automaticamente (`ddl-auto=update`).

### Endpoints

- REST: `http://localhost:8080`. Exemplos: `POST /events/{id}/cancel`, e `GET /events/{id}`, que aceita o header opcional `X-Public: true` para devolver a representação reduzida `{id, status}`.
- GraphQL: `http://localhost:8080/graphql`.
- GraphiQL: `http://localhost:8080/graphiql`.

## Rodando os testes

O build da imagem da aplicação pula os testes (`-x test` no `Dockerfile`). Para rodar a suíte, use a mesma imagem do estágio de build:

```bash
docker run --rm -u "$(id -u):$(id -g)" -e GRADLE_USER_HOME=/tmp/gradle \
  -v "$PWD":/app -w /app gradle:8.2.1-jdk17 gradle test --no-daemon
```

As opções `-u` e `GRADLE_USER_HOME` fazem o contêiner rodar com o seu usuário. Sem elas, os arquivos gerados em `build/` ficam com dono `root`.

O comando roda a suíte dos três módulos:

- `domain`: testes de unidade das entidades e regras de domínio.
- `application`: testes de caso de uso com repositórios in-memory (`application/src/test/.../repository/`).
- `infrastructure`: testes de integração (`*IT`) e de controllers/resolvers. Usam o profile `test`, com H2 em memória no modo MySQL, e por isso **não precisam do MySQL do `docker compose`**.

Para rodar só um módulo, só uma classe ou gerar o relatório de cobertura agregado (JaCoCo, em `build/reports/jacoco/test/`), troque `gradle test` no fim do comando por:

```bash
gradle :domain:test --no-daemon
gradle :infrastructure:test --tests "br.com.fullcycle.infrastructure.gateways.EventCancelledFlowIT" --no-daemon
gradle test testCodeCoverageReport --no-daemon
```

## Onde acontece a cascata de cancelamento

Quando um parceiro cancela um evento, o `CancelEventUseCase` chama `Event.cancel()`. Esse método muda o status para `CANCELLED` e registra o evento de domínio **`EventCancelled`** (type `event.cancelled`) na lista interna do agregado. O caso de uso não toca em nenhum ingresso. Ao persistir o agregado, o `EventDatabaseRepository` grava os eventos de `allDomainEvents()` como JSON na tabela **outbox**, na mesma transação. Depois, o **`OutboxRelay`**, um job `@Scheduled` que roda a cada 2s, lê as mensagens ainda não publicadas e as envia à fila pelo `QueueGateway`. Na ponta consumidora, o **`ConsumerQueueGateway`** roteia pelo type `event.cancelled` e dispara o **`CancelEventTicketsUseCase`**. Esse caso de uso busca os ingressos do evento via `TicketRepository.ticketsByEventId` e chama `Ticket.cancel()` em cada um. O `Ticket.cancel()` é idempotente, então reprocessar a mensagem não quebra nada. É o mesmo molde do fluxo `EventTicketReserved` → `CreateTicketForCustomerUseCase`, e os agregados `Event` e `Ticket` continuam desacoplados. O teste `EventCancelledFlowIT` cobre esse caminho de ponta a ponta.
