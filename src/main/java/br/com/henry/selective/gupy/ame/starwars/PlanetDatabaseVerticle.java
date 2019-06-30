package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.SneakyThrows;
import org.jooq.Query;
import org.jooq.conf.ParamType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class PlanetDatabaseVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetDatabaseVerticle.class);

    private static final EntityManagerFactory factory = Persistence.createEntityManagerFactory("PlanetPersistenceUnit");
    private static final EntityManager entityManager = factory.createEntityManager();

    @Override
    public void start(Future<Void> future) {
        createConsumers();
        future.complete();
    }

    private void createConsumers() {
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_INSERT, this::onInsert);
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_SEARCH, this::onSearch);
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_DELETE, this::onDelete);
    }

    private void onInsert(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildInsertQuery(entityManager, planet, SwapiPlanet.class);
        LOGGER.trace("Generated query for Planet insertion: {}", query.getSQL(ParamType.INLINED));
        // TODO: Run the query on the database and handle the response
    }

    @SneakyThrows
    private void onSearch(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildSelectByExample(entityManager, planet, Planet.class);
        LOGGER.trace("Generated query for Planet search: {}", query.getSQL(ParamType.INLINED));
        // TODO: Run the query on the database and handle the response
    }

    @SneakyThrows
    private void onDelete(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildDeleteByExample(entityManager, planet, Planet.class);
        LOGGER.trace("Generated query for Planet deletion: {}", query.getSQL(ParamType.INLINED));
        // TODO: Run the query on the database and handle the response
    }

}