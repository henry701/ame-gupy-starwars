package br.com.henry.selective.gupy.ame.starwars;

public abstract class EventBusAddresses {

    public static final String SWAPI_AGGREGATOR = "aggregate";
    public static final String DATABASE_HANDLER_INSERT = "dbhandle_insert";
    public static final String DATABASE_HANDLER_SEARCH = "dbhandle_search";

    private EventBusAddresses() {
        // Static class
    }

}
