package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.SneakyThrows;

public class PlanetDatabaseVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetDatabaseVerticle.class);

    @Override
    public void start(Future<Void> future) {
        createConsumer();
        future.complete();
    }

    private void createConsumer() {
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_INSERT, this::onInsert);
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_SEARCH, this::onSearch);
    }

    private void onInsert(Message<Object> message) {
        JsonObject planet = new JsonObject(message.body().toString());
        String planetName = planet.getString("name");
    }

    @SneakyThrows
    private void onSearch(Message<Object> message) {
        JsonObject planet = new JsonObject(message.body().toString());
        String planetName = planet.getString("name");
        Integer planetId = planet.getInteger("id");
    }

}