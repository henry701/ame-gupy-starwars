package br.com.henry.selective.gupy.ame.starwars;

import br.com.henry.selective.gupy.ame.starwars.util.ConfigUtils;
import br.com.henry.selective.gupy.ame.starwars.verticle.PlanetDatabaseVerticle;
import br.com.henry.selective.gupy.ame.starwars.verticle.ServerVerticle;
import br.com.henry.selective.gupy.ame.starwars.verticle.SwapiVerticle;
import io.vertx.core.*;
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
            ConfigUtils.getConfig(vertx, JsonObject.mapFrom(merge))
                .setHandler(configAr -> {
                    startWithConfig(vertx, configAr)
                        .setHandler(startedAr -> {
                            if (startedAr.failed()) {
                                LOGGER.error("Error occurred while starting the verticles!", startedAr.cause());
                                System.exit(-1);
                            }
                        });
                });
        } catch(Exception e) {
            LOGGER.error("Fatal error occurred while starting application!", e);
            throw e;
        }
    }

    private Future<Void> startWithConfig(Vertx vertx, AsyncResult<JsonObject> configAr) {
        DeploymentOptions deploymentOptions = ConfigUtils.buildConfig(configAr);
        return startVerticles(vertx, deploymentOptions);
    }

    private Future<Void> startVerticles(Vertx vertx, DeploymentOptions deploymentOptions) {
        Future<Void> swapi = Future.future();
        vertx.deployVerticle(new SwapiVerticle(), deploymentOptions);
        Future<Void> server = Future.future();
        vertx.deployVerticle(new ServerVerticle(), deploymentOptions);
        Future<Void> database = Future.future();
        vertx.deployVerticle(new PlanetDatabaseVerticle(), deploymentOptions);
        CompositeFuture future = CompositeFuture.all(swapi, server, database);
        return future.mapEmpty();
    }

}
