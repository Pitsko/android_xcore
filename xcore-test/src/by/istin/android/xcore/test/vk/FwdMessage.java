package by.istin.android.xcore.test.vk;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import by.istin.android.xcore.annotations.dbLong;
import by.istin.android.xcore.annotations.dbString;
import by.istin.android.xcore.db.DBHelper;
import by.istin.android.xcore.db.IBeforeArrayUpdate;
import by.istin.android.xcore.db.IBeforeUpdate;
import by.istin.android.xcore.source.DataSourceRequest;
import by.istin.android.xcore.utils.HashUtils;

public class FwdMessage implements BaseColumns, IBeforeArrayUpdate, IBeforeUpdate {

	@dbLong
	public static final String ID = _ID;
	
	@dbLong
	public static final String UID = "uid";
	
	@dbLong
	public static final String DATE = "date";
	
	@dbString
	public static final String BODY = "body";

	@dbLong
	public static final String DIALOG_ID = "dialog_id";
	
	@Override
	public void onBeforeListUpdate(DBHelper dbHelper, SQLiteDatabase db, DataSourceRequest dataSourceRequest, int position, ContentValues contentValues) {
		
	}

	@Override
	public void onBeforeUpdate(DBHelper dbHelper, SQLiteDatabase db, DataSourceRequest dataSourceRequest, ContentValues contentValues) {
		String value = contentValues.getAsLong(UID) + contentValues.getAsString(BODY) + contentValues.getAsLong(DATE);
		contentValues.put(ID, HashUtils.generateId(value));		
	}
	
}