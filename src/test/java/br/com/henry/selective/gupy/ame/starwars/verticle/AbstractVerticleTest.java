package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.util.ConfigUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
public abstract class AbstractVerticleTest<T extends AbstractVerticle> {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    protected T verticle;
    protected Class<T> verticleClass;

    protected static Map<String, Object> mergeConfigs;

    public AbstractVerticleTest(Class<T> verticleClass) {
        this.verticleClass = verticleClass;
    }

    @BeforeClass
    @SneakyThrows
    public static void init() {
        mergeConfigs = ConfigUtils.loadEnv(new Object());
    }

    @Before
    @SneakyThrows
    public void before() {
        this.verticle = verticleClass.newInstance();
    }

    @After
    @SneakyThrows
    public void after() {
        verticle.stop();
    }

    @Test
    public void testDeployment(TestContext testContext) {
        Async testAsync = testContext.async();
        testContext.verify(__ -> deployThen(testContext).setHandler(verticleAr -> testAsync.complete()));
    }

    protected Future<String> deployThen(TestContext testContext) {
        Future<String> cont = Future.future();
        ConfigUtils.getConfig(rule.vertx(), JsonObject.mapFrom(mergeConfigs)).setHandler(configAr -> {
            Future<String> future = Future.future();
            rule.vertx().deployVerticle(verticle, ConfigUtils.buildConfig(configAr), future);
            future.setHandler(verticleAr -> {
                if (verticleAr.failed()) {
                    testContext.fail(verticleAr.cause());
                } else {
                    cont.handle(verticleAr);
                }
            });
        });
        return cont;
    }

    protected JsonObject getExampleYavinPlanet() {
        return getExampleYavinPlanet(true, null);
    }

    protected JsonObject getExampleYavinPlanet(boolean films) {
        return getExampleYavinPlanet(films, null);
    }

    protected JsonObject getExampleYavinPlanet(boolean films, Long id) {
        JsonObject examplePlanet = new JsonObject()
            .put("name", "Yavin IV")
            .put("climate", "temperate")
            .put("terrain", "forests, mountains, lakes");
        if (films) {
            examplePlanet.put("films", 1);
        }
        if (id != null) {
            examplePlanet.put("id", id);
        }
        return examplePlanet;
    }

}
