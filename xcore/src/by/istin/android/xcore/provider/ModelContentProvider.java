package by.istin.android.xcore.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import by.istin.android.xcore.db.DBHelper;
import by.istin.android.xcore.provider.ModelContract.ModelColumns;
import by.istin.android.xcore.source.DataSourceRequest;
import by.istin.android.xcore.source.DataSourceRequestEntity;
import by.istin.android.xcore.utils.Log;
import by.istin.android.xcore.utils.StringUtil;

public abstract class ModelContentProvider extends ContentProvider {

	private UriMatcher mUriMatcher;

	private static final int MODELS = 1;

	private static final int MODELS_ID = 2;
	
	private static final int MODELS_ID_NEGOTIVE = 3;

	private DBHelper dbHelper;
	
	@Override
	public boolean onCreate() {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		String authority = ModelContract.getAuthority(getContext());
		mUriMatcher.addURI(authority, "*", MODELS);
		mUriMatcher.addURI(authority, "*/#", MODELS_ID);
		//for negotive number
		mUriMatcher.addURI(authority, "*/*", MODELS_ID_NEGOTIVE);
		dbHelper = new DBHelper(getContext());
		Class<?>[] dbEntities = getDbEntities();
		dbHelper.createTablesForModels(DataSourceRequestEntity.class);
		dbHelper.createTablesForModels(dbEntities);
		return true;
	}
	
	public abstract Class<?>[] getDbEntities();
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		String className = uri.getLastPathSegment();
		try {
			String cleanerParameter = uri.getQueryParameter(ModelContract.PARAM_CLEANER);
			int count = dbHelper.updateOrInsert(getDataSourceRequestFromUri(uri), !StringUtil.isEmpty(cleanerParameter), Class.forName(className), values);
			if (count > 0) {
				if (StringUtil.isEmpty(uri.getQueryParameter(ModelContract.PARAM_NOT_NOTIFY_CHANGES))) {
					getContext().getContentResolver().notifyChange(uri, null);
				}
			}
			return count;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static DataSourceRequest getDataSourceRequestFromUri(Uri uri) {
		String parameter = uri.getQueryParameter(ModelContract.DATA_SOURCE_REQUEST_PARAM);
		if (!StringUtil.isEmpty(parameter)) {
			return DataSourceRequest.fromUri(Uri.parse("content://temp?"+StringUtil.decode(parameter)));
		}
		return null;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case MODELS:
			return ModelContract.getContentType(uri.getLastPathSegment());
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
	
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		List<String> pathSegments = uri.getPathSegments();
		String className = StringUtil.EMPTY;
		switch (mUriMatcher.match(uri)) {
		case MODELS:
			className = pathSegments.get(pathSegments.size()-1);
			break;
		case MODELS_ID:
			className = pathSegments.get(pathSegments.size()-2);
			if (where == null) {
				where = StringUtil.EMPTY;
			}
			where = where + ModelColumns._ID + " = " + uri.getLastPathSegment();
			break;
		case MODELS_ID_NEGOTIVE:
			className = pathSegments.get(pathSegments.size()-2);
			if (where == null) {
				where = StringUtil.EMPTY;
			}
			where = where + ModelColumns._ID + " = " + uri.getLastPathSegment();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		try {
			int count = dbHelper.delete(Class.forName(className), where, whereArgs);
			if (StringUtil.isEmpty(uri.getQueryParameter(ModelContract.PARAM_NOT_NOTIFY_CHANGES))) {
				getContext().getContentResolver().notifyChange(uri, null);
			}
			return count;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (mUriMatcher.match(uri) != MODELS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		String className = uri.getLastPathSegment();
		try {
			String cleanerParameter = uri.getQueryParameter(ModelContract.PARAM_CLEANER);
            DataSourceRequest dataSourceRequestFromUri = getDataSourceRequestFromUri(uri);
            boolean withCleaner = !StringUtil.isEmpty(cleanerParameter);
            Class<?> classOfModel = Class.forName(className);
            long rowId = dbHelper.updateOrInsert(dataSourceRequestFromUri, withCleaner, classOfModel, initialValues);
			if (rowId != -1l) {
				Uri serializableModelUri = ContentUris.withAppendedId(uri, rowId);
				if (StringUtil.isEmpty(uri.getQueryParameter(ModelContract.PARAM_NOT_NOTIFY_CHANGES))) {
					getContext().getContentResolver().notifyChange(
						serializableModelUri, null);
				}
				return serializableModelUri;
			} else {
                throw new IllegalArgumentException(uri + ": " +initialValues.toString());
			}
			//TODO need check throw new SQLException("Failed to insert row into " + uri);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public int update(Uri uri, ContentValues initialValues, String where,
			String[] whereArgs) {
		throw new UnsupportedOperationException("unsupported operation, please use insert method");
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String className = null;
		List<String> pathSegments = null;
		switch (mUriMatcher.match(uri)) {
		case MODELS:
			className = uri.getLastPathSegment();
			break;
		case MODELS_ID:
			pathSegments = uri.getPathSegments();
			className = pathSegments.get(pathSegments.size()-2);
			if (StringUtil.isEmpty(selection)) {
				selection = ModelColumns._ID + " = " + uri.getLastPathSegment();
			} else {
				selection = selection + ModelColumns._ID + " = " + uri.getLastPathSegment();	
			}
			break;
		case MODELS_ID_NEGOTIVE:
			pathSegments = uri.getPathSegments();
			className = pathSegments.get(pathSegments.size()-2);
			if (StringUtil.isEmpty(selection)) {
				selection = ModelColumns._ID + " = " + uri.getLastPathSegment();
			} else {
				selection = selection + ModelColumns._ID + " = " + uri.getLastPathSegment();	
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		if (className.equals(ModelContract.SEGMENT_RAW_QUERY)) {
			Cursor c = dbHelper.rawQuery(uri.getQueryParameter(ModelContract.SQL_PARAM), selectionArgs);		
			if (c != null) {
				c.getCount();
				c.moveToFirst();
			}
			String encodedUri = uri.getQueryParameter(ModelContract.OBSERVER_URI_PARAM);
			if (!StringUtil.isEmpty(encodedUri)) {
				c.setNotificationUri(getContext().getContentResolver(), Uri.parse(StringUtil.decode(encodedUri)));
			}
			return c;
		} else {
			try {
				String offsetParameter = uri.getQueryParameter("offset");
				String sizeParameter = uri.getQueryParameter("size");
				String limitParam = null;
				if (!StringUtil.isEmpty(offsetParameter) && !StringUtil.isEmpty(sizeParameter)) {
					limitParam = String.format("%s,%s",offsetParameter, sizeParameter);
				}
				Cursor c = dbHelper.query(Class.forName(className), projection, selection, selectionArgs, null, null, sortOrder, limitParam);
				if (c != null) {
					c.setNotificationUri(getContext().getContentResolver(), uri);
					c.getCount();
					c.moveToFirst();
				}
				return c;	
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
        dbHelper.lockTransaction();
        ContentProviderResult[] result = new ContentProviderResult[operations.size()];
        try {
            Set<Uri> set = new HashSet<Uri>();
            for(int i = 0; i < operations.size(); i++) {
                ContentProviderOperation contentProviderOperation = operations.get(i);
                Uri uri = contentProviderOperation.getUri();
                Log.xd(this, uri);
                result[i] = contentProviderOperation.apply(this, result, i);
                //ContentValues contentValues = contentProviderOperation.resolveValueBackReferences(result, i);
                set.add(uri);
            }
            for (Iterator<Uri> iterator = set.iterator(); iterator.hasNext(); ) {
                Uri uri = iterator.next();
                getContext().getContentResolver().notifyChange(uri, null);
            }
            dbHelper.unlockTransaction();
        } catch (OperationApplicationException e1) {
            dbHelper.errorUnlockTransaction();
            throw e1;
        } catch (Exception e) {
            dbHelper.errorUnlockTransaction();
            throw new IllegalArgumentException(e);
        }
		return result;
	}
	
	
	
}