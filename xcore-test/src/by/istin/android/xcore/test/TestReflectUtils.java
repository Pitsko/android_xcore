/**
 * 
 */
package by.istin.android.xcore.test;

import java.lang.reflect.Field;
import java.util.List;

import android.content.ContentValues;
import android.test.AndroidTestCase;
import by.istin.android.xcore.db.IBeforeArrayUpdate;
import by.istin.android.xcore.db.IMerge;
import by.istin.android.xcore.test.bo.TestEntity;
import by.istin.android.xcore.utils.BytesUtils;
import by.istin.android.xcore.utils.ReflectUtils;

/**
 * @author Uladzimir_Klyshevich
 *
 */
public class TestReflectUtils extends AndroidTestCase {

	public void testKeysFields() throws Exception {
		List<Field> entityKeys = ReflectUtils.getEntityKeys(TestEntity.class);
		assertEquals(entityKeys.size(), 7);
	}
	
	public void testInterfaceInstance() throws Exception {
		IMerge merge = ReflectUtils.getInstanceInterface(TestEntity.class, IMerge.class);
		assertNotNull(merge);
		IBeforeArrayUpdate beforeListUpdate = ReflectUtils.getInstanceInterface(TestEntity.class, IBeforeArrayUpdate.class);
		assertNull(beforeListUpdate);
	}
	
	public void testContentValueByteConvertation() throws Exception {
		ContentValues values = new ContentValues();
		values.put("key1", true);
		values.put("key2", "value");
		byte[] byteArray = BytesUtils.toByteArray(values);
		ContentValues createFromParcel = BytesUtils.contentValuesFromByteArray(byteArray);
		assertTrue(createFromParcel.getAsBoolean("key1") && true);
		assertEquals(createFromParcel.getAsString("key2"), "value");
		
		ContentValues[] contentValues = new ContentValues[2];
		contentValues[0] = values;
		values = new ContentValues();
		values.put("key3", false);
		values.put("key4", "val2");
		contentValues[1] = values;
		byteArray = BytesUtils.arrayToByteArray(contentValues);
		contentValues = BytesUtils.arrayContentValuesFromByteArray(byteArray);
		assertEquals(contentValues.length, 2);
	}
	
}
