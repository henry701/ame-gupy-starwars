package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class);

    private HttpServer httpServer;

    @Override
    public void start(Future<Void> future) {
        Router router = createRoutes();
        createServer(future, router);
    }

    private void createServer(Future<Void> future, Router router) {
        int port = config().getInteger("http.port", 9000);
        String host = config().getString("http.host", "0.0.0.0");
        this.httpServer = vertx.createHttpServer().requestHandler(router).listen(
            port,
            host,
            result -> {
                if (result.succeeded()) {
                    future.complete();
                    LOGGER.info("ServerVerticle is up and running on {}:{}", host, port);
                } else {
                    future.fail(result.cause());
                }
            }
        );
    }

    private Router createRoutes() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route("/planet")
                .method(HttpMethod.POST)
                .handler(BodyHandler.create())
                .handler(routingRequest -> {

            JsonObject body = routingRequest.getBodyAsJson();
            Planet planet = body.mapTo(Planet.class);
            LOGGER.info("Received creation request for planet {}", planet);

            vertx.eventBus().send(EventBusAddresses.SWAPI_AGGREGATOR, JsonObject.mapFrom(planet).encode(), newPlanetAr -> {

                Object newPlanet = extractNewPlanet(planet, newPlanetAr);

                vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_INSERT, JsonObject.mapFrom(newPlanet).encode(), dbResponseAr -> {
                    if(dbResponseAr.failed()) {
                        LOGGER.error("Insertion of planet {} failed due to database error!", newPlanet, dbResponseAr.cause());
                        routingRequest.response().setStatusCode(500).end();
                        return;
                    }
                    routingRequest.response().setStatusCode(201).end();
                });
            });
        });

        // TODO: id and name queryParameters. If none are provided, lists the planets.
        // TODO: Separate this?
        // TODO: Use pagination :o?
        router.route("/planet").method(HttpMethod.GET);

        return router;
    }

    private Object extractNewPlanet(Planet planet, AsyncResult<Message<Object>> newPlanetAr) {
        Object newPlanet;
        if(newPlanetAr.failed()) {
            LOGGER.warn("Planet with name {} is being inserted without film quantity due to error", planet.getName(), newPlanetAr.cause());
            newPlanet = planet;
        } else {
            if(newPlanetAr.result() == null) {
                LOGGER.warn("Planet with name {} is being inserted without film quantity due to null response", planet.getName());
                newPlanet = planet;
            } else {
                newPlanet = new JsonObject(newPlanetAr.result().body().toString());
                LOGGER.debug("Planet has been aggregated successfully: {}", newPlanet);
            }
        }
        return newPlanet;
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }
}