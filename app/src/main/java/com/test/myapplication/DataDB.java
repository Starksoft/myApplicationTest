package com.test.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataDB extends SQLiteOpenHelper
{
	private static final int    DB_VERSION = 1;
	private static final String DB_NAME    = "DataDB";

	/**
	 * _id	|	timestamp		|	points_json		|
	 * 1	|	1384200000000	|	{}				|
	 * 2	|	1384200000000	|	{}				|
	 */

	public static String TABLE_NAME = "PointsTable";

	public static final String ID          = "_id";
	public static final String TIMESTAMP   = "timestamp";
	public static final String POINTS_JSON = "points_json";

	private static final String CREATE_TABLE = "create table " +
			TABLE_NAME + " ( _id integer primary key autoincrement, " +
			TIMESTAMP + " INTEGER, " +
			POINTS_JSON + " TEXT" +
			")";

	public DataDB(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase)
	{
		sqLiteDatabase.execSQL(CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
	{
		//		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		//		onCreate(sqLiteDatabase);
	}
}