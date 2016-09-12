package com.takeapeek.ormlite;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

@DatabaseTable
public class TakeAPeekSendObject
{
	public enum IntentSendTypeEnum
	{
		none,
		image,
		text
	}
	
	static private final Logger logger = LoggerFactory.getLogger(TakeAPeekSendObject.class);
	
	@DatabaseField(generatedId=true, columnName = "id")
	private int id;

	@DatabaseField(dataType=DataType.INTEGER, columnName = "NumberOfUses")
	public int NumberOfUses = 0;
	
	@DatabaseField(dataType=DataType.INTEGER, columnName = "Position")
	public int Position = -1;
	
	@DatabaseField(dataType=DataType.INTEGER, columnName = "IntentSendType")
	public int IntentSendType = -1;
	
	@DatabaseField(columnName = "PackageName")
	public String PackageName = null;
	
	@DatabaseField(columnName = "ActivityName")
	public String ActivityName = null;
	
	@DatabaseField(columnName = "Label")
	public String Label = null;
	
	@DatabaseField(dataType = DataType.BYTE_ARRAY, columnName = "IconData")
	public byte[] IconData;
	
	//getters and setters
	public void setId(int id) 
	{
		this.id = id;
	}
	public int getId() 
	{
		return id;
	}
	
	public Bitmap getIcon()
	{
		return BitmapFactory.decodeByteArray(IconData, 0, IconData.length);
	}
	
	public TakeAPeekSendObject(){}
	
	public TakeAPeekSendObject(PackageManager packageManager, ResolveInfo resolveInfo, IntentSendTypeEnum intentSendType)
	{
		logger.debug("TakeAPeekSendObject(...) Invoked");
		
		Init(packageManager, resolveInfo, intentSendType);
	}
	
	public TakeAPeekSendObject(PackageManager packageManager, ResolveInfo resolveInfo)
	{
		logger.debug("TakeAPeekSendObject(..) Invoked");
		
		Init(packageManager, resolveInfo, IntentSendTypeEnum.image);
	}
	
	private void Init(PackageManager packageManager, ResolveInfo resolveInfo, IntentSendTypeEnum intentSendType)
	{
		logger.debug(String.format("Init: package=%s, type=%s) Invoked",
				resolveInfo.activityInfo.packageName, intentSendType.name()));
		
		PackageName = resolveInfo.activityInfo.packageName;
		ActivityName = resolveInfo.activityInfo.name;
		
		Drawable icon = resolveInfo.loadIcon(packageManager);
		Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
		IconData = byteArrayOutputStream.toByteArray();
		
		Label = (String) resolveInfo.loadLabel(packageManager);
		IntentSendType = intentSendType.ordinal();
	}
}
