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
import io.vertx.ext.web.RoutingContext;
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
                .handler(this::productCreationHandler);

        router.route("/planet")
                .method(HttpMethod.GET)
                .handler(routingRequest -> {
                    JsonObject queryMap = new JsonObject()
                            .put("id", routingRequest.queryParams().get("id"))
                            .put("name", routingRequest.queryParams().get("name"));
                    vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_SEARCH, queryMap.encode(), planetsAr -> {
                        // TODO: Reply the list of products from the database
                    });
                });

        router.route("/planet/:id")
                .method(HttpMethod.DELETE)
                .handler(routingRequest -> {
                    JsonObject queryMap = new JsonObject()
                            .put("id", routingRequest.pathParam("id"));
                    vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_DELETE, queryMap.encode(), planetsAr -> {
                        // TODO: Reply success no content 204
                    });
                });

        return router;
    }

    private void productCreationHandler(RoutingContext routingRequest) {

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
                routingRequest.response().setStatusCode(201).end(JsonObject.mapFrom(newPlanet).encodePrettily());
            });
        });

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