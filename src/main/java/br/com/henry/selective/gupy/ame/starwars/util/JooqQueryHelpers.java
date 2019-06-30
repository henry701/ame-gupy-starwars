package br.com.henry.selective.gupy.ame.starwars.util;

import io.vertx.core.json.Json;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.util.Map;

/**
 * https://www.jooq.org/doc/3.11/manual/sql-execution/alternative-execution-models/using-jooq-with-jpa/using-jooq-with-jpa-entities/
 */
public abstract class JooqQueryHelpers {

    private JooqQueryHelpers() {
        // Static class
    }

    public static <T> org.jooq.Query buildInsertQuery(EntityManager em, T object, Class<T> type) {
        String tableName = getTableName(em, type);
        @SuppressWarnings("unchecked")
        Map<String, Object> record = Json.mapper.convertValue(object, Map.class);
        return getDSL()
                .insertInto(DSL.table(DSL.name(tableName)))
                .columns(getFields(record))
                .values(record.values());
    }

    private static Field[] getFields(Map<String, Object> record) {
        return record.keySet().stream().map(f -> DSL.field(DSL.name(f))).toArray(Field[]::new);
    }

    public static <T> org.jooq.Query buildSelectByExample(EntityManager em, T object, Class<T> type) {
        String tableName = getTableName(em, type);
        @SuppressWarnings("unchecked")
        Map<String, Object> record = Json.mapper.convertValue(object, Map.class);
        return getDSL()
                .select(getFields(record))
                .from(DSL.table(DSL.name(tableName)))
                .where(getConditions(record));
    }

    public static <T> org.jooq.Query buildDeleteByExample(EntityManager em, T object, Class<T> type) {
        String tableName = getTableName(em, type);
        @SuppressWarnings("unchecked")
        Map<String, Object> record = Json.mapper.convertValue(object, Map.class);
        return getDSL()
                .delete(DSL.table(DSL.name(tableName)))
                .where(getConditions(record));
    }

    private static Condition[] getConditions(Map<String, Object> record) {
        return record
                .entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .map(e -> DSL.field(DSL.name(e.getKey())).eq(e.getValue()))
                .toArray(Condition[]::new);
    }

    private static DSLContext getDSL() {
        return DSL.using(SQLDialect.MYSQL_5_7);
    }

    /**
     * Returns the table name for a given entity type in the {@link EntityManager}.
     */
    public static <T> String getTableName(EntityManager em, Class<T> entityClass) {

        /*
         * Check if the specified class is present in the metamodel.
         * Throws IllegalArgumentException if not.
         */
        Metamodel meta = em.getMetamodel();
        EntityType<T> entityType = meta.entity(entityClass);

        //Check whether @Table annotation is present on the class.
        Table t = entityClass.getAnnotation(Table.class);

        return (t == null)
                ? entityType.getName().toUpperCase()
                : t.name();
    }

}
