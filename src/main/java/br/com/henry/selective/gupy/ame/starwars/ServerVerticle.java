package br.com.henry.selective.gupy.ame.starwars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class);

    @Override
    public void start(Future<Void> future) {
        Router router = createRoutes();
        createServer(future, router);
    }

    private void createServer(Future<Void> future, Router router) {
        int port = config().getInteger("http.port", 9000);
        String host = config().getString("http.host", "0.0.0.0");
        vertx.createHttpServer().requestHandler(router).listen(
                port,
                host,
                result -> {
                    if (result.succeeded()) {
                        future.complete();
                        LOGGER.info("Server is up and running on {}:{}", host, port);
                    } else {
                        future.fail(result.cause());
                    }
                }
        );
    }

    private Router createRoutes() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // TODO? Add the actual handlers
        router.route("/planet").method(HttpMethod.POST);
        // TODO: id and name queryParameters. If none are provided, lists the planets.
        // TODO: Separate this or use pagination?
        router.route("/planet").method(HttpMethod.GET);

        return router;
    }

}