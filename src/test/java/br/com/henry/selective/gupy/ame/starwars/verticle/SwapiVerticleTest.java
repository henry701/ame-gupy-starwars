package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SwapiVerticleTest extends AbstractVerticleTest<SwapiVerticle> {

    public SwapiVerticleTest() {
        super(SwapiVerticle.class);
    }

    @Test
    public void testGetWithYavinName(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> rule.vertx().eventBus().send(EventBusAddresses.SWAPI_AGGREGATOR, new JsonObject().put("name", "Yavin IV").encode(), reply -> {
            if (reply.failed()) {
                testContext.fail(reply.cause());
                return;
            }
            if (!new JsonObject(reply.result().body().toString()).containsKey("films")) {
                testContext.fail("SWAPI response did not contain films attribute!");
            }
            testAsync.complete();
        }));
    }

    @Test
    public void testGetWithInvalidName(TestContext testContext) {
        Async testAsync = testContext.async();
        deployThen(testContext).setHandler(verticleAr -> rule.vertx().eventBus().send(EventBusAddresses.SWAPI_AGGREGATOR, new JsonObject().put("name", "This planet does NOT exist").encode(), reply -> {
            if (reply.failed()) {
                testAsync.complete();
                return;
            }
            testContext.fail("Message returned success for invalid planet name!");
        }));
    }

}