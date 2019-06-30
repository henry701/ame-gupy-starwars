package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.SneakyThrows;

import java.net.URLEncoder;

public class SwapiVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwapiVerticle.class);

    private WebClient webClient;

    @Override
    public void start(Future<Void> future) {
        createWebClient();
        createConsumer();
        future.complete();
    }

    private void createConsumer() {
        vertx.eventBus().consumer(EventBusAddresses.SWAPI_AGGREGATOR, this::onMessage);
    }

    @SneakyThrows
    private void onMessage(Message<Object> message) {
        JsonObject planet = new JsonObject(message.body().toString());
        String planetName = planet.getString("name");

        webClient.get("/api/planets?search=" + URLEncoder.encode(planetName, "UTF-8"))
                 .send(httpResponseAr -> onSwapiResponse(message, planet, httpResponseAr));
    }

    private void onSwapiResponse(Message<Object> message, JsonObject planet, AsyncResult<HttpResponse<Buffer>> httpResponseAr) {

        if(httpResponseAr.failed()) {
            message.fail(0, httpResponseAr.cause().getMessage());
            return;
        }

        HttpResponse httpResponse = httpResponseAr.result();

        if(httpResponse.statusCode() != 200) {
            message.fail(httpResponse.statusCode(), httpResponse.statusMessage());
            return;
        }

        JsonObject responseBody = httpResponse.bodyAsJsonObject();

        if(responseBody.getInteger("count", 0) == 0) {
            message.fail(404, "Planet not found!");
            return;
        }

        int films = responseBody.getJsonArray("results").getJsonObject(0).getJsonArray("films", new JsonArray()).size();

        JsonObject newPlanet = planet.put("films", films);

        message.reply(newPlanet.encode());
    }

    private void createWebClient() {
        WebClientOptions webClientOptions = new WebClientOptions()
                .setDefaultHost(config().getString("swapi.http.host", "swapi.co"))
                .setDefaultPort(config().getInteger("swapi.http.port", 80));
        this.webClient = WebClient.create(vertx, webClientOptions);
    }

}