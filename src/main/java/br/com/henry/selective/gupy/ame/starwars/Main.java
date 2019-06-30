package br.com.henry.selective.gupy.ame.starwars;

import br.com.henry.selective.gupy.ame.starwars.util.ConfigUtils;
import br.com.henry.selective.gupy.ame.starwars.verticle.PlanetDatabaseVerticle;
import br.com.henry.selective.gupy.ame.starwars.verticle.ServerVerticle;
import br.com.henry.selective.gupy.ame.starwars.verticle.SwapiVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        new Main().instanceMain(args);
    }

    public void instanceMain(String[] args) {
        try {
            Map<String, Object> merge = ConfigUtils.loadEnv(this);
            Vertx vertx = Vertx.vertx();
            ConfigUtils.getConfig(vertx, JsonObject.mapFrom(merge)).setHandler(startWithConfig(vertx));
        } catch(Exception e) {
            LOGGER.error("Fatal error while starting server!", e);
            throw e;
        }
    }

    private Handler<AsyncResult<JsonObject>> startWithConfig(Vertx vertx) {
        return configAr -> startWithConfig(vertx, configAr);
    }

    private void startWithConfig(Vertx vertx, AsyncResult<JsonObject> configAr) {
        DeploymentOptions deploymentOptions = ConfigUtils.buildConfig(configAr);
        startVerticles(vertx, deploymentOptions);
    }

    private void startVerticles(Vertx vertx, DeploymentOptions deploymentOptions) {
        // TODO: CompletionHandler the three, and if any fails, throw exception to drop the application
        vertx.deployVerticle(new SwapiVerticle(), deploymentOptions);
        vertx.deployVerticle(new ServerVerticle(), deploymentOptions);
        vertx.deployVerticle(new PlanetDatabaseVerticle(), deploymentOptions);
        LOGGER.info("All verticles have been deployed!");
    }

}
