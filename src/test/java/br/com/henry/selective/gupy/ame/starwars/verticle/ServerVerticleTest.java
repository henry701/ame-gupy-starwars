package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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
                try {
                    JsonObject examplePlanet = getExampleYavinPlanet(false);
                    String responseString = RestAssured
                        .given()
                        .body(examplePlanet.encode())
                        .contentType(ContentType.JSON)
                        .post(getBaseUrl() + "/planet")
                        .then()
                        .assertThat()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString();
                    JsonObject jsonResponse = new JsonObject(responseString);
                    testContext.assertEquals(examplePlanet.put("id", id), jsonResponse);
                } catch (Throwable e) {
                    testContext.fail(e);
                }
                testAsync.complete();
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