package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(VertxUnitRunner.class)
public class ServerVerticleTest extends AbstractVerticleTest<ServerVerticle> {

    public ServerVerticleTest() {
        super(ServerVerticle.class);
    }

    @Test
    public void testCreatePlanet(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            Long id = 143245L;
            Integer films = 1;
            Future<Void> swapiConsumerFuture = getSwapiAggregatorMock(testContext, films);
            Future<Void> databaseConsumerFuture = getDatabaseInsertMock(testContext, id);
            CompositeFuture.all(swapiConsumerFuture, databaseConsumerFuture).setHandler(__ -> {

                JsonObject examplePlanet = getExampleYavinPlanet(false);
                rule.vertx().executeBlocking(restFuture -> {
                        try {
                            String responseString = RestAssured
                                .given()
                                .body(examplePlanet.encode())
                                .contentType(ContentType.JSON)
                                .post(getBaseUrl() + "/planet")
                                .then()
                                .assertThat()
                                .statusCode(201)
                                .contentType(ContentType.JSON)
                                .extract()
                                .body()
                                .asString();
                            restFuture.complete(responseString);
                        } catch (Throwable e) {
                            testContext.fail(e);
                            restFuture.fail(e);
                        }
                    },
                    stringResponse -> {
                        JsonObject jsonResponse = new JsonObject(stringResponse.result().toString());
                        testContext.assertEquals(examplePlanet.put("id", id).put("films", films), jsonResponse);
                        testAsync.complete();
                    }
                );
            });
        });
    }

    @Test
    public void testDeletePlanetSuccess(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            Long id = 143245L;
            Future<Void> databaseConsumerFuture = getDatabaseDeleteMock(testContext, id, 1);
            databaseConsumerFuture.setHandler(__ -> {
                rule.vertx().executeBlocking(restFuture -> {
                        try {
                            RestAssured
                                .given()
                                .delete(getBaseUrl() + "/planet/" + id)
                                .then()
                                .assertThat()
                                .statusCode(204);
                            restFuture.complete(null);
                        } catch (Throwable e) {
                            testContext.fail(e);
                            restFuture.fail(e);
                        }
                    },
                    ___ -> {
                        testAsync.complete();
                    }
                );
            });
        });
    }

    @Test
    public void testDeletePlanetFailure(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            Long id = 143245L;
            Future<Void> databaseConsumerFuture = getDatabaseDeleteMock(testContext, id, 0);
            databaseConsumerFuture.setHandler(__ -> {
                rule.vertx().executeBlocking(restFuture -> {
                        try {
                            RestAssured
                                .given()
                                .delete(getBaseUrl() + "/planet/" + id)
                                .then()
                                .assertThat()
                                .contentType(ContentType.JSON)
                                .statusCode(400);
                            restFuture.complete(null);
                        } catch (Throwable e) {
                            testContext.fail(e);
                            restFuture.fail(e);
                        }
                    },
                    ___ -> {
                        testAsync.complete();
                    }
                );
            });
        });
    }

    @Test
    public void testRetrievePlanet(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            Long id = 143245L;
            Integer films = 1;
            JsonObject examplePlanet = new JsonObject().put("name", "Yavin IV").put("id", id);
            JsonObject returnedPlanet = new JsonObject().put("name", "Yavin IV").put("id", id).put("films", films);
            Future<Void> databaseConsumerFuture = getDatabaseSearchMock(testContext, returnedPlanet);
            databaseConsumerFuture.setHandler(__ -> {
                rule.vertx().executeBlocking(restFuture -> {
                        try {
                            String responseString = RestAssured
                                .given()
                                .body(examplePlanet.encode())
                                .contentType(ContentType.JSON)
                                .get(getBaseUrl() + "/planet?id=" + id + "&name=" + examplePlanet.getString("name"))
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .extract()
                                .body()
                                .asString();
                            restFuture.complete(responseString);
                        } catch (Throwable e) {
                            testContext.fail(e);
                            restFuture.fail(e);
                        }
                    },
                    stringResponse -> {
                        JsonObject jsonResponse = new JsonObject(stringResponse.result().toString());
                        testContext.assertEquals(returnedPlanet, jsonResponse);
                        testAsync.complete();
                    }
                );
            });
        });
    }

    @Test
    public void testRetrievePlanetError(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            JsonObject examplePlanet = new JsonObject();
            JsonObject returnedPlanet = new JsonObject();
            rule.vertx().executeBlocking(restFuture -> {
                    try {
                        String responseString = RestAssured
                            .given()
                            .body(examplePlanet.encode())
                            .contentType(ContentType.JSON)
                            .get(getBaseUrl() + "/planet")
                            .then()
                            .assertThat()
                            .statusCode(400)
                            .contentType(ContentType.JSON)
                            .extract()
                            .body()
                            .asString();
                        restFuture.complete(responseString);
                    } catch (Throwable e) {
                        testContext.fail(e);
                        restFuture.fail(e);
                    }
                },
                stringResponse -> {
                    testAsync.complete();
                }
            );
        });
    }

    @Test
    public void testListPlanets(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> {
            Long id = 143245L;
            Integer films = 1;
            JsonObject examplePlanet = new JsonObject().put("name", "Yavin IV").put("id", id);
            JsonObject returnedPlanet = new JsonObject().put("name", "Yavin IV").put("id", id).put("films", films);
            JsonArray returnedPlanets = new JsonArray(Collections.singletonList(returnedPlanet));
            Future<Void> databaseConsumerFuture = getDatabaseSearchMock(testContext, returnedPlanets);
            databaseConsumerFuture.setHandler(__ -> {
                rule.vertx().executeBlocking(restFuture -> {
                        try {
                            String responseString = RestAssured
                                .given()
                                .body(examplePlanet.encode())
                                .contentType(ContentType.JSON)
                                .get(getBaseUrl() + "/planet/list")
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .extract()
                                .body()
                                .asString();
                            restFuture.complete(responseString);
                        } catch (Throwable e) {
                            testContext.fail(e);
                            restFuture.fail(e);
                        }
                    },
                    stringResponse -> {
                        JsonArray jsonResponse = new JsonArray(stringResponse.result().toString());
                        testContext.assertEquals(returnedPlanets, jsonResponse);
                        testAsync.complete();
                    }
                );
            });
        });
    }

    private Future<Void> getDatabaseInsertMock(TestContext testContext, Long id) {
        MessageConsumer<?> databaseConsumer = rule.vertx().eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_INSERT, message -> {
            JsonObject planet = new JsonObject(message.body().toString());
            testContext.assertNull(planet.getLong("id"));
            message.reply(planet.put("id", id).encode());
        });
        Future<Void> databaseConsumerFuture = Future.future();
        databaseConsumer.completionHandler(databaseConsumerFuture);
        return databaseConsumerFuture;
    }

    private Future<Void> getDatabaseSearchMock(TestContext testContext, JsonObject planet) {
        MessageConsumer<?> databaseConsumer = rule.vertx().eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_SEARCH, message -> {
            JsonObject planetSearch = new JsonObject(message.body().toString());
            testContext.assertEquals(planet.getLong("id"), planetSearch.getLong("id"));
            testContext.assertEquals(planet.getString("name"), planetSearch.getString("name"));
            message.reply(new JsonArray(planet.isEmpty() ? Collections.emptyList() : Collections.singletonList(planet)).encode());
        });
        Future<Void> databaseConsumerFuture = Future.future();
        databaseConsumer.completionHandler(databaseConsumerFuture);
        return databaseConsumerFuture;
    }

    private Future<Void> getDatabaseSearchMock(TestContext testContext, JsonArray planets) {
        MessageConsumer<?> databaseConsumer = rule.vertx().eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_SEARCH, message -> {
            JsonObject planetSearch = new JsonObject(message.body().toString());
            testContext.assertEquals(planetSearch.size(), 0);
            message.reply(planets.encode());
        });
        Future<Void> databaseConsumerFuture = Future.future();
        databaseConsumer.completionHandler(databaseConsumerFuture);
        return databaseConsumerFuture;
    }

    private Future<Void> getDatabaseDeleteMock(TestContext testContext, Long id, Integer deleted) {
        MessageConsumer<?> databaseConsumer = rule.vertx().eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_DELETE, message -> {
            JsonObject planet = new JsonObject(message.body().toString());
            testContext.assertEquals(id, planet.getLong("id"));
            message.reply(new JsonObject().put("deleted", deleted).encode());
        });
        Future<Void> databaseConsumerFuture = Future.future();
        databaseConsumer.completionHandler(databaseConsumerFuture);
        return databaseConsumerFuture;
    }

    private Future<Void> getSwapiAggregatorMock(TestContext testContext, Integer films) {
        MessageConsumer<?> swapiConsumer = rule.vertx().eventBus().consumer(EventBusAddresses.SWAPI_AGGREGATOR, message -> {
            JsonObject planet = new JsonObject(message.body().toString());
            testContext.assertNull(planet.getLong("id"));
            message.reply(planet.put("films", films).encode());
        });
        Future<Void> swapiConsumerFuture = Future.future();
        swapiConsumer.completionHandler(swapiConsumerFuture);
        return swapiConsumerFuture;
    }

    private String getBaseUrl() {
        return "http://" + System.getProperty("http.host") + ":" + System.getProperty("http.port");
    }

}