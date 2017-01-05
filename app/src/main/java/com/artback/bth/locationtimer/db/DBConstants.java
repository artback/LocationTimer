package com.artback.bth.locationtimer.db;
final class DBConstants {
    
    static final int DB_VERSION = 1 ;

    static final String DB_NAME = "geofenceLocation.db" ;

    static final String DB_TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL";
    static final String DB_TYPE_SECONDARY_KEY = "TEXT UNIQUE";

    static final String DB_TYPE_INTEGER = "INTEGER";

    static final String DB_TYPE_REAL = "REAL";
}
