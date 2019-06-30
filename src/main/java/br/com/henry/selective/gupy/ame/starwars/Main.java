package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            loadEnv();
            Vertx vertx = Vertx.vertx();
            getConfig(vertx, configHandler(vertx));
        }
        catch(Exception e) {
            LOGGER.error("Fatal error while starting server!", e);
            throw e;
        }
    }

    @SneakyThrows
    private static void loadEnv() {
        InputStream stream = Main.class.getResourceAsStream("/config.properties");
        if(stream == null) {
            LOGGER.info("config.properties was not found, skipping system property setting");
            return;
        }
        Properties props = new Properties();
        props.load(new InputStreamReader(stream));
        for(Map.Entry<Object, Object> prop : props.entrySet()) {
            if(System.getProperties().containsKey(prop.getKey())) {
                continue;
            }
            System.setProperty(prop.getKey().toString(), Objects.toString(prop.getValue(), null));
        }
    }

    private static Handler<AsyncResult<JsonObject>> configHandler(Vertx vertx) {
        return configAr -> startWithConfig(vertx, configAr);
    }

    private static void startWithConfig(Vertx vertx, AsyncResult<JsonObject> configAr) {
        DeploymentOptions deploymentOptions = buildConfig(configAr);
        startVerticles(vertx, deploymentOptions);
    }

    private static void startVerticles(Vertx vertx, DeploymentOptions deploymentOptions) {
        vertx.deployVerticle(new SwapiVerticle(), deploymentOptions);
        vertx.deployVerticle(new ServerVerticle(), deploymentOptions);
        vertx.deployVerticle(new PlanetDatabaseVerticle(), deploymentOptions);
        LOGGER.info("All verticles have been deployed!");
    }

    private static DeploymentOptions buildConfig(AsyncResult<JsonObject> configAr) {
        throwIfConfigHasError(configAr);
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(configAr.result());
        return deploymentOptions;
    }

    @SneakyThrows
    private static void throwIfConfigHasError(AsyncResult<JsonObject> configAr) {
        if (configAr.failed()) {
            throw configAr.cause();
        }
    }

    private static void getConfig(Vertx vertx, Handler<AsyncResult<JsonObject>> then) {
        LOGGER.info("Retrieving configuration...");
        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(sysPropsStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        retriever.getConfig(then);
    }

}
