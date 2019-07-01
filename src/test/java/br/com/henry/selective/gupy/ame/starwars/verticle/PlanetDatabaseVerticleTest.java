package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import br.com.henry.selective.gupy.ame.starwars.model.SwapiPlanet;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.SneakyThrows;
import org.jooq.Query;
import org.jooq.conf.ParamType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PlanetDatabaseVerticleTest extends AbstractVerticleTest<PlanetDatabaseVerticle> {

    public PlanetDatabaseVerticleTest() {
        super(PlanetDatabaseVerticle.class);
    }

    @BeforeClass
    @SneakyThrows
    public static void init() {
        AbstractVerticleTest.init();
        Class.forName(System.getProperty("database.driver_class"));
    }

    @Test
    public void testInsert(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> deployThen(testContext).setHandler(verticleAr -> {
            JsonObject examplePlanet = getExampleYavinPlanet();
            insertPlanetUsingVerticle(examplePlanet)
                .setHandler(reply -> {
                    if (reply.failed()) {
                        testContext.fail(reply.cause());
                        return;
                    }
                    JsonObject response = new JsonObject(reply.result().body().toString());
                    Long id = response.getLong("id");
                    if (id == null) {
                        testContext.fail("Planet ID is not present on insertion response!");
                        return;
                    }
                    selectUsingJooq(testContext, examplePlanet.put("id", id).mapTo(SwapiPlanet.class))
                        .setHandler(queryResponseAr -> {
                            testContext.assertEquals(queryResponseAr.result().getRows().size(), 1, "No results were returned on SELECT");
                            testContext.assertEquals(examplePlanet.put("id", id), queryResponseAr.result().getRows().get(0), "Different object was returned on SELECT");
                            testAsync.complete();
                        });
                });
        }));
    }

    private <T> Future<Message<T>> insertPlanetUsingVerticle(JsonObject examplePlanet) {
        Future<Message<T>> future = Future.future();
        rule.vertx().eventBus().send(EventBusAddresses.DATABASE_HANDLER_INSERT, examplePlanet.encode(), future);
        return future;
    }

    @Test
    public void testSearchByName(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> deployThen(testContext).setHandler(verticleAr -> {
                JsonObject examplePlanet = getExampleYavinPlanet();
                insertPlanetUsingVerticle(examplePlanet)
                    .setHandler(insertReply -> {
                        if (insertReply.failed()) {
                            testContext.fail(insertReply.cause());
                            return;
                        }
                        Long id = new JsonObject(insertReply.result().body().toString()).getLong("id");
                        JsonObject exampleQuery = new JsonObject().put("name", "Yavin IV");
                        rule.vertx().eventBus().send(EventBusAddresses.DATABASE_HANDLER_SEARCH, exampleQuery.encode(), selectReply -> {
                            if (selectReply.failed()) {
                                testContext.fail(selectReply.cause());
                                return;
                            }
                            JsonArray response = new JsonArray(selectReply.result().body().toString());
                            testContext.assertEquals(1, response.size(), "Response SELECT array size is not 1!");
                            testContext.assertEquals(examplePlanet.put("id", id), response.getJsonObject(0));
                            testAsync.complete();
                        });
                    });
            })
        );
    }

    @Test
    public void testDelete(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> deployThen(testContext).setHandler(verticleAr -> {
                JsonObject examplePlanet = getExampleYavinPlanet();
                insertPlanetUsingVerticle(examplePlanet)
                    .setHandler(insertReply -> {
                        if (insertReply.failed()) {
                            testContext.fail(insertReply.cause());
                            return;
                        }
                        Long id = new JsonObject(insertReply.result().body().toString()).getLong("id");
                        JsonObject exampleDelete = new JsonObject().put("id", id);
                        rule.vertx().eventBus().send(EventBusAddresses.DATABASE_HANDLER_DELETE, exampleDelete.encode(), deleteReply -> {
                            if (deleteReply.failed()) {
                                testContext.fail(deleteReply.cause());
                                return;
                            }
                            SwapiPlanet selectId = new SwapiPlanet();
                            selectId.setId(id);
                            selectUsingJooq(testContext, selectId).setHandler(selectResponse -> {
                                testContext.assertEquals(0, selectResponse.result().getRows().size(), "Planet was not deleted!");
                                testAsync.complete();
                            });
                        });
                    });
            })
        );
    }

    private Future<ResultSet> selectUsingJooq(TestContext testContext, SwapiPlanet examplePlanet) {
        Query select = verticle.getJooqQueryHelpers().buildSelectByExample(verticle.getEntityManager(), examplePlanet, SwapiPlanet.class);
        Future<ResultSet> future = Future.future();
        verticle.getSqlClient().query(select.getSQL(ParamType.INLINED), queryResponseAr -> {
            if (queryResponseAr.failed()) {
                testContext.fail(queryResponseAr.cause());
                return;
            }
            future.handle(queryResponseAr);
        });
        return future;
    }

}