package br.com.henry.selective.gupy.ame.starwars.util;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public abstract class ConfigUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

    private ConfigUtils() {
        // Static class
    }

    public static Future<JsonObject> getConfig(Vertx vertx, JsonObject merge) {
        LOGGER.info("Retrieving configuration...");
        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(sysPropsStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        Future<JsonObject> future = Future.future();
        retriever.getConfig(future);
        future = future.compose(f ->
                Future.succeededFuture(f.mergeIn(merge, true))
        );
        return future;
    }

    @SneakyThrows
    public static Map<String, Object> loadEnv(Object instance) {
        InputStream stream = instance.getClass().getResourceAsStream("/config.properties");
        if (stream == null) {
            LOGGER.info("config.properties was not found, skipping system property setting");
            return Collections.emptyMap();
        }
        Properties props = new Properties();
        props.load(new InputStreamReader(stream));
        Map<String, Object> nullOverriders = new HashMap<>();
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            if (System.getProperties().containsKey(prop.getKey())) {
                continue;
            }
            String key = prop.getKey().toString();
            String val = Objects.toString(prop.getValue(), null);
            if ("null".equals(val)) {
                val = null;
            }
            if (val == null) {
                System.clearProperty(key);
                nullOverriders.put(key, null);
            } else {
                System.setProperty(key, val);
            }
        }
        return nullOverriders;
    }

    public static DeploymentOptions buildConfig(AsyncResult<JsonObject> configAr) {
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
}
