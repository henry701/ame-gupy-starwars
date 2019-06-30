package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import br.com.henry.selective.gupy.ame.starwars.model.Planet;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
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
        try {
            Router router = createRoutes();
            createServer(future, router);
        } catch (Exception e) {
            future.fail(e);
        }
    }

    @Override
    public void stop(Future<Void> future) {
        httpServer.close(future);
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
                .handler(this::applicationJsonResponse)
                .handler(this::productCreationHandler);

        router.route("/planet/list")
                .method(HttpMethod.GET)
                .handler(this::applicationJsonResponse)
                .handler(this::planetListHandler);

        router.route("/planet")
                .method(HttpMethod.GET)
                .handler(this::applicationJsonResponse)
                .handler(this::planetQueryHandler);

        router.route("/planet/:id")
                .method(HttpMethod.DELETE)
                .handler(this::planetDeletionHandler);

        return router;
    }

    private void applicationJsonResponse(RoutingContext routingRequest) {
        routingRequest.response().headers().add("Content-Type", "application/json");
        routingRequest.next();
    }

    private void planetListHandler(RoutingContext routingRequest) {
        vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_SEARCH, "{}", planetsAr -> {
            if(planetsAr.failed()) {
                routingRequest.response().setStatusCode(500).end();
                return;
            }
            JsonArray response = planetsAr.result().body() == null ? new JsonArray() : new JsonArray(planetsAr.result().body().toString());
            routingRequest.response().setStatusCode(200).end(response.encodePrettily());
        });
    }

    private void planetQueryHandler(RoutingContext routingRequest) {
        JsonObject queryMap = new JsonObject()
                .put("id", routingRequest.queryParams().get("id"))
                .put("name", routingRequest.queryParams().get("name"));
        if(queryMap.isEmpty()) {
            finishWithError(routingRequest, 400, "Either 'id' or 'name' query parameters should be present on the request!");
            return;
        }
        vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_SEARCH, queryMap.encode(), planetsAr -> {
            if(planetsAr.failed()) {
                finishWithError(routingRequest, 500, planetsAr.cause().getMessage());
                return;
            }
            JsonArray list = planetsAr.result().body() == null ? new JsonArray() : new JsonArray(planetsAr.result().body().toString());
            JsonObject response = list.isEmpty() ? new JsonObject() : list.getJsonObject(0);
            routingRequest.response().setStatusCode(200).end(response.encodePrettily());
        });
    }

    private JsonObject getMessageError(String message) {
        return new JsonObject().put("message", message);
    }

    private void planetDeletionHandler(RoutingContext routingRequest) {
        String planetId = routingRequest.pathParam("id");
        JsonObject queryMap = new JsonObject().put("id", planetId);
        vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_DELETE, queryMap.encode(), deletionAr -> {
            if(deletionAr.failed()) {
                LOGGER.error("Deletion of planet with ID {} failed due to database error!", planetId, deletionAr.cause());
                finishWithError(routingRequest, 500, deletionAr.cause().getMessage());
                return;
            }
            routingRequest.response().setStatusCode(204).end();
        });
    }

    private void productCreationHandler(RoutingContext routingRequest) {

        JsonObject body = routingRequest.getBodyAsJson();
        Planet planet = body.mapTo(Planet.class);
        LOGGER.info("Received creation request for planet {}", planet);

        aggregateWithSwapi(
                planet,
                newPlanetAr -> insertInDatabase(routingRequest, planet, newPlanetAr)
        );

    }

    private <T> void aggregateWithSwapi(Planet planet, Handler<AsyncResult<Message<T>>> replyHandler) {
        vertx.eventBus().send(EventBusAddresses.SWAPI_AGGREGATOR, JsonObject.mapFrom(planet).encode(), replyHandler);
    }

    private void insertInDatabase(RoutingContext routingRequest, Planet planet, AsyncResult<Message<Object>> newPlanetAr) {
        if (newPlanetAr.failed()) {
            LOGGER.warn("Planet with name {} aggregation has failed!", planet.getName(), newPlanetAr.cause());
            finishWithError(routingRequest, 500, newPlanetAr.cause().getMessage());
            return;
        }
        JsonObject newPlanet = new JsonObject(newPlanetAr.result().body().toString());
        LOGGER.debug("Planet has been aggregated successfully: {}", newPlanet);
        vertx.eventBus().send(EventBusAddresses.DATABASE_HANDLER_INSERT, JsonObject.mapFrom(newPlanet).encode(), dbResponseAr -> afterInsert(routingRequest, newPlanet, dbResponseAr));
    }

    private void finishWithError(RoutingContext routingRequest, int statusCode, String message) {
        routingRequest.response().setStatusCode(statusCode).end(getMessageError(message).encodePrettily());
    }

    private void afterInsert(RoutingContext routingRequest, Object newPlanet, AsyncResult<Message<Object>> dbResponseAr) {
        if (dbResponseAr.failed()) {
            LOGGER.error("Insertion of planet {} failed due to database error!", newPlanet, dbResponseAr.cause());
            finishWithError(routingRequest, 500, dbResponseAr.cause().getMessage());
            return;
        }
        routingRequest.response().setStatusCode(201).end(new JsonObject(dbResponseAr.result().body().toString()).encodePrettily());
    }

}