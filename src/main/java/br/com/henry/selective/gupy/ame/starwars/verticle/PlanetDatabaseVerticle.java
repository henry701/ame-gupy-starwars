package br.com.henry.selective.gupy.ame.starwars.verticle;

import br.com.henry.selective.gupy.ame.starwars.constant.EventBusAddresses;
import br.com.henry.selective.gupy.ame.starwars.model.SwapiPlanet;
import br.com.henry.selective.gupy.ame.starwars.util.JooqQueryHelpers;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jooq.Query;
import org.jooq.conf.ParamType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetDatabaseVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetDatabaseVerticle.class);

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    private SessionFactory sessionFactory;
    private Session session;

    private SQLClient mySQLClient;

    @Override
    public void start(Future<Void> future) {

        vertx.executeBlocking(
            blockFuture -> {
                try {
                    createEntityManager();
                    createSqlClient();
                    createConsumers();
                }
                catch(Exception e) {
                    blockFuture.fail(e);
                    return;
                }
                blockFuture.complete();
            },
            blockAr -> {
                if(blockAr.failed()) {
                    future.fail(blockAr.cause());
                    return;
                }
                future.complete();
            }
        );

    }

    private void createEntityManager() {
        Configuration cfg = new Configuration()
                .setProperty("hibernate.connection.url", assembleConnectionString())
                .setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver")
                .setProperty("hibernate.connection.username", config().getString("database.username", "root"))
                .setProperty("hibernate.connection.password", config().getString("database.password", "root"))
                .setProperty("hibernate.order_updates", "true");
        Map<Object, Object> merged = new HashMap(config().getMap());
        merged.putAll(cfg.getProperties());
        this.entityManagerFactory = Persistence.createEntityManagerFactory(
                config().getString("database.persistence.unit", "PlanetPersistenceUnit"),
                merged
        );
        this.entityManager = entityManagerFactory.createEntityManager();
        this.sessionFactory = cfg.buildSessionFactory();
        this.session = this.sessionFactory.openSession();
    }

    private String assembleConnectionString() {
        String cstring = "jdbc:mysql://";
        cstring += config().getString("database.host", "127.0.0.1");
        Integer port = config().getInteger("database.port", 3306);
        if(port != null) {
            cstring += ":" + port;
        }
        cstring += "/" + config().getString("database.schema", "ameswapi");
        cstring += "?createDatabaseIfNotExist=true&autoCommit=true";
        return cstring;
    }

    @Override
    public void stop(Future<Void> future) {

        vertx.executeBlocking(
            blockFuture -> {
                try {
                    entityManagerFactory.close();
                    entityManager.close();
                    sessionFactory.close();
                    session.close();
                }
                catch(Exception e) {
                    blockFuture.fail(e);
                    return;
                }
                blockFuture.complete();
            },
            blockAr -> {
                if(blockAr.failed()) {
                    future.fail(blockAr.cause());
                }
                mySQLClient.close(closeAr -> {
                    if(closeAr.failed()) {
                        future.fail(closeAr.cause());
                        return;
                    }
                    future.complete();
                });
            }
        );

    }

    private void createSqlClient() {
        JsonObject mySQLClientConfig = buildDatabaseConfig();
        this.mySQLClient = MySQLClient.createNonShared(vertx, mySQLClientConfig);
    }

    private JsonObject buildDatabaseConfig() {
        return new JsonObject()
                        .put("host", config().getString("database.host", "127.0.0.1"))
                        .put("port", config().getInteger("database.port", 3306))
                        .put("username", config().getString("database.username", "root"))
                        .put("password", config().getString("database.password", "root"))
                        .put("database", config().getString("database.schema", "ameswapi"))
                        .put("charset", config().getString("database.charset", "UTF-8"));
    }

    private void createConsumers() {
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_INSERT, this::onInsert);
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_SEARCH, this::onSearch);
        vertx.eventBus().consumer(EventBusAddresses.DATABASE_HANDLER_DELETE, this::onDelete);
    }

    @SneakyThrows
    private void onInsert(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildInsertQuery(entityManager, planet, SwapiPlanet.class);
        String rawQuery = query.getSQL(ParamType.INLINED);
        LOGGER.trace("Generated query for Planet insertion: {}", rawQuery);
        mySQLClient.update(rawQuery, resultAr -> {
            if(resultAr.failed()) {
                message.fail(0, resultAr.cause().getMessage());
                return;
            }
            Long id = resultAr.result().getKeys().getLong(0);
            planet.setId(id);
            message.reply(JsonObject.mapFrom(planet).encode());
        });
    }

    @SneakyThrows
    private void onSearch(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildSelectByExample(entityManager, planet, SwapiPlanet.class);
        String rawQuery = query.getSQL(ParamType.INLINED);
        LOGGER.trace("Generated query for Planet search: {}", rawQuery);
        mySQLClient.query(rawQuery, resultAr -> {
            if(resultAr.failed()) {
                message.fail(0, resultAr.cause().getMessage());
                return;
            }
            List<JsonObject> results = resultAr.result().getRows();
            message.reply(new JsonArray(results).encode());
        });
    }

    @SneakyThrows
    private void onDelete(Message<Object> message) {
        SwapiPlanet planet = new JsonObject(message.body().toString()).mapTo(SwapiPlanet.class);
        Query query = JooqQueryHelpers.buildDeleteByExample(entityManager, planet, SwapiPlanet.class);
        String rawQuery = query.getSQL(ParamType.INLINED);
        LOGGER.trace("Generated query for Planet deletion: {}", rawQuery);
        mySQLClient.update(rawQuery, resultAr -> {
            if(resultAr.failed()) {
                message.fail(0, resultAr.cause().getMessage());
                return;
            }
            message.reply(resultAr.result().getUpdated());
        });
    }

}
