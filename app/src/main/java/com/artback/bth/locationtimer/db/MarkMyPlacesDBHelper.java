package com.artback.bth.locationtimer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public final class MarkMyPlacesDBHelper extends SQLiteOpenHelper {
    
    private static MarkMyPlacesDBHelper mAppDBHelper ;
    /**
     * 
     * Constructor is made private to support singleton 
     */
    private MarkMyPlacesDBHelper(Context context) {
        super(context, DBConstants.DB_NAME, null, DBConstants.DB_VERSION);
    }
 
    /**
     * method to get helper instance
     * @param context - application context preferable
     * @return
     */
    public static MarkMyPlacesDBHelper getInstance(Context context) {
        if (mAppDBHelper == null) {
            synchronized (MarkMyPlacesDBHelper.class) {
                if (mAppDBHelper == null) {
                    mAppDBHelper = new MarkMyPlacesDBHelper(context);
                }
            }
        }
        return mAppDBHelper;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
       db.execSQL(GeoFenceLocation.MyPlaceTable.CREATE_QUERY);

    }
    private void dropTables(SQLiteDatabase db) {
        db.execSQL(GeoFenceLocation.MyPlaceTable.DROP_QUERY);
         
     }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        createTables(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        dropTables(db);
        createTables(db);
    }

    protected long insert(SQLiteDatabase db,String tableName, ContentValues values ){
        long rowId = -1 ;
        synchronized (db) {
            rowId  = db.insert(tableName, null,values);
        }
        return rowId ;
    }
    protected long insert(String tableName, ContentValues values ){
        SQLiteDatabase writableDB = getWritableDatabase();
        long rowId = -1 ;
        synchronized (writableDB) {
            rowId  = writableDB.insert(tableName, null,values);
        }
        return rowId ;
    }
    protected long delete(String tableName, String[] whereArgs ){
        SQLiteDatabase writableDB = getWritableDatabase();
        long rowId = -1 ;
        String whereClause = "id=?";
        synchronized (writableDB) {
            rowId  = writableDB.delete(tableName, whereClause,whereArgs);
        }
        return rowId ;
    }
    protected Cursor query(String table,String[] columns,String selection,String[] selectionArgs,String groupby,String having,String orderBy){
        SQLiteDatabase readableDB = getReadableDatabase();
        Cursor resultCursor = null ;
        synchronized (readableDB) {
            resultCursor = readableDB.query(table, columns, selection, selectionArgs, groupby, having, orderBy);
        }
        return resultCursor ;
    }
    protected int update(String tableName, ContentValues values, String whereClause, String[] whereArgs){
        SQLiteDatabase writableDB = getWritableDatabase();
        int updatedRows = 0 ;
        synchronized (writableDB) {
            updatedRows = writableDB.update(tableName, values, whereClause, whereArgs);
        }
        return updatedRows ;
    }
    public long removeMyPlace(GeoFenceLocation geoFenceLocation){
        String[] whereArgs = new String[]{ geoFenceLocation.getId()};
        return delete(GeoFenceLocation.MyPlaceTable.TABLE_NAME,whereArgs);
    }
    public long insertMyPlace(GeoFenceLocation newGeoFenceLocation){
        return insert(GeoFenceLocation.MyPlaceTable.TABLE_NAME, newGeoFenceLocation.toContentValues());
    }
    public long insertMyPlace(SQLiteDatabase db ,GeoFenceLocation newGeoFenceLocation){
        return insert(db,GeoFenceLocation.MyPlaceTable.TABLE_NAME, newGeoFenceLocation.toContentValues());
    }
    public boolean updateMyPlace(String Id, GeoFenceLocation newGeoFenceLocation){
        String whereClause = GeoFenceLocation.MyPlaceColumns.ID + " = ?" ;
        String[] whereArgs = {Id};
        return update(GeoFenceLocation.MyPlaceTable.TABLE_NAME, newGeoFenceLocation.toContentValues(),whereClause,whereArgs) > 0;
    }
    public boolean updateMyPlace(int rowId, GeoFenceLocation newGeoFenceLocation){
        String whereClause = GeoFenceLocation.MyPlaceColumns._ID + " = ?" ;
        String[] whereArgs = {String.valueOf(rowId)};
        return update(GeoFenceLocation.MyPlaceTable.TABLE_NAME, newGeoFenceLocation.toContentValues(),whereClause,whereArgs) > 0;
    }
    public Cursor queryMyPlacesAll(){
        return query(GeoFenceLocation.MyPlaceTable.TABLE_NAME, null, null, null, null, null, null);
    }
    public Cursor queryMyPlacesGeofence(){
        String whereClause = GeoFenceLocation.MyPlaceColumns.FENCE_RADIUS+ " > ?" ;
        String[] whereArgs = {" 0 "};
        return query(GeoFenceLocation.MyPlaceTable.TABLE_NAME, null, whereClause, whereArgs, null, null, null);
    }
    public Cursor queryMyPlace(long rowId){
        String selection = GeoFenceLocation.MyPlaceColumns._ID +  " = ? " ;
        String[] selectionArgs = {String.valueOf(rowId)};
        return query(GeoFenceLocation.MyPlaceTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);
    }
    public Cursor queryMyPlace(String id){
        String selection = GeoFenceLocation.MyPlaceColumns.ID +  " = ? " ;
        String[] selectionArgs = {id};
        return query(GeoFenceLocation.MyPlaceTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);
    }
    public GeoFenceLocation getPlace(long rowId) {
        GeoFenceLocation resultPlace = null;
        Cursor resultQuery = queryMyPlace(rowId);
        if (resultQuery.moveToNext()) {
            resultPlace = new GeoFenceLocation(resultQuery);
        }
        return resultPlace;
    }
    public GeoFenceLocation getPlace(String id) {
        GeoFenceLocation resultPlace = null;
        Cursor resultQuery = queryMyPlace(id);
        if (resultQuery.moveToNext()) {
            resultPlace = new GeoFenceLocation(resultQuery);
        }
        return resultPlace;
    }
    public List<GeoFenceLocation> getMyPlaces(){
       return getPlaceListFromCursor(queryMyPlacesAll());
    }

    public List<GeoFenceLocation> getMyPlacesToGeofence(){
        return getPlaceListFromCursor(queryMyPlacesGeofence());
    }
    public List<GeoFenceLocation> getPlaceListFromCursor(Cursor placeCursor){
        ArrayList<GeoFenceLocation> myPlacesList = new ArrayList<GeoFenceLocation>();
        if(placeCursor !=null ){
            while(placeCursor.moveToNext()){
                myPlacesList.add(new GeoFenceLocation(placeCursor));
            }
        }
        return myPlacesList;
    }
}
