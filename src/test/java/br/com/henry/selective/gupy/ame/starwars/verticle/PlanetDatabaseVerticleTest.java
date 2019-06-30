package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import br.com.henry.selective.gupy.ame.starwars.util.ConfigUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class PlanetDatabaseVerticleTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    private static Map<String, Object> mergeConfigs;

    private PlanetDatabaseVerticle verticle;

    @Before
    @SneakyThrows
    public void before() {
        this.verticle = new PlanetDatabaseVerticle();
    }

    @After
    @SneakyThrows
    public void after() {
        verticle.stop();
    }

    @BeforeClass
    @SneakyThrows
    public static void init() {
        mergeConfigs = ConfigUtils.loadEnv(new Object());
        Class.forName(System.getProperty("database.driver_class"));
    }

    @Test
    public void testDeployment(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> ConfigUtils.getConfig(rule.vertx(), JsonObject.mapFrom(mergeConfigs)).setHandler(configAr ->
                rule.vertx().deployVerticle(verticle, ConfigUtils.buildConfig(configAr), verticleAr -> {
                    if (verticleAr.failed()) {
                        testContext.fail(verticleAr.cause());
                    } else {
                        testAsync.complete();
                    }
                })
        ));
    }

    @Test
    public void testInsert(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> ConfigUtils.getConfig(rule.vertx(), JsonObject.mapFrom(mergeConfigs)).setHandler(configAr ->
                rule.vertx().deployVerticle(verticle, ConfigUtils.buildConfig(configAr), verticleAr -> {
                    if (verticleAr.failed()) {
                        testContext.fail(verticleAr.cause());
                        return;
                    }
                    JsonObject examplePlanet = new JsonObject()
                            .put("name", "Yavin IV")
                            .put("films", 1)
                            .put("climate", "temperate")
                            .put("terrain", "forests, mountains, lakes");
                    rule.vertx().eventBus().send(EventBusAddresses.DATABASE_HANDLER_INSERT, examplePlanet.encode(), reply -> {
                        if (reply.failed()) {
                            testContext.fail(reply.cause());
                            return;
                        }
                        if (!new JsonObject(reply.result().body().toString()).containsKey("id")) {
                            testContext.fail("Planet ID is not present on response!");
                            return;
                        }
                        testAsync.complete();
                    });
                })
        ));
    }

}