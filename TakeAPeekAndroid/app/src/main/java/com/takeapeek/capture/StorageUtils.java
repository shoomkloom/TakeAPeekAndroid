package com.takeapeek.capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.content.ContextCompat;

import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//import android.content.ContentValues;
//import android.location.Location;

/** Provides access to the filesystem. Supports both standard and Storage
 *  Access Framework.
 */
public class StorageUtils
{
    static private final Logger logger = LoggerFactory.getLogger(StorageUtils.class);

    SharedPreferences mSharedPreferences = null;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

	Context context = null;
    private Uri last_media_scanned = null;

	// for testing:
	public boolean failed_to_scan = false;
	
	StorageUtils(Context context)
    {
        logger.debug("StorageUtils(.) Invoked.");

        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
	}
	
	Uri getLastMediaScanned()
    {
		return last_media_scanned;
	}
	void clearLastMediaScanned()
    {
		last_media_scanned = null;
	}

	void announceUri(Uri uri, boolean is_new_picture, boolean is_new_video)
    {
        logger.debug("announceUri(...) Invoked.");

		logger.info("announceUri: " + uri);
    	if( is_new_picture )
        {
    		// note, we reference the string directly rather than via Camera.ACTION_NEW_PICTURE, as the latter class is now deprecated - but we still need to broadcast the string for other apps
    		context.sendBroadcast(new Intent( "android.hardware.action.NEW_PICTURE" , uri));
    		// for compatibility with some apps - apparently this is what used to be broadcast on Android?
    		context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));

            {
    	        String[] CONTENT_PROJECTION =
                        { Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE, Images.Media.DATE_TAKEN, Images.Media.DATE_ADDED };
    	        Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null); 
    	        if( c == null )
                {
	 				Helper.Error(logger, "Couldn't resolve given uri [1]: " + uri);
    	        }
    	        else if( !c.moveToFirst() )
                {
	 				Helper.Error(logger, "Couldn't resolve given uri [2]: " + uri);
    	        }
    	        else
                {
        	        String file_path = c.getString(c.getColumnIndex(Images.Media.DATA)); 
        	        String file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME)); 
        	        String mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE)); 
        	        long date_taken = c.getLong(c.getColumnIndex(Images.Media.DATE_TAKEN)); 
        	        long date_added = c.getLong(c.getColumnIndex(Images.Media.DATE_ADDED)); 
	 				logger.info("file_path: " + file_path);
	 				logger.info("file_name: " + file_name);
	 				logger.info("mime_type: " + mime_type);
	 				logger.info("date_taken: " + date_taken);
	 				logger.info("date_added: " + date_added);
        	        c.close(); 
    	        }
    		}
    	}
    	else if( is_new_video )
        {
    		context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));
    	}
	}
	
    public void broadcastFile(final File file, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned)
    {
        logger.debug("broadcastFile(....) Invoked.");

		logger.info("broadcastFile: " + file.getAbsolutePath());
    	// note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
    	if( file.isDirectory() )
        {
    		//this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
        	// ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives: SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MEDIA_MOUNTED
    		// note that we don't actually need to broadcast anything, the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
    		// also note that we definitely don't want to broadcast ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as this means the folder shows up as a file on a PC via MTP (and isn't fixed by rebooting!)
    	}
    	else
        {
        	// both of these work fine, but using MediaScannerConnection.scanFile() seems to be preferred over sending an intent
    		//context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
 			failed_to_scan = true; // set to true until scanned okay
            logger.info("failed_to_scan set to true");
        	MediaScannerConnection.scanFile(context, new String[]
                    { file.getAbsolutePath() }, null,
        			new MediaScannerConnection.OnScanCompletedListener()
                    {
					public void onScanCompleted(String path, Uri uri)
                    {
    		 			failed_to_scan = false;

                        logger.info("Scanned " + path + ":");
                        logger.info("-> uri=" + uri);

    		 			if( set_last_scanned )
                        {
    		 				last_media_scanned = uri;
       		 				logger.info("set last_media_scanned to " + last_media_scanned);
    		 			}
    		 			announceUri(uri, is_new_picture, is_new_video);

    	    			// it seems caller apps seem to prefer the content:// Uri rather than one based on a File
    		 			Activity activity = (Activity)context;
    		 			String action = activity.getIntent().getAction();
    		 	        if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) )
                        {
   		    				logger.info("from video capture intent");
	    		 			Intent output = new Intent();
	    		 			output.setData(uri);
	    		 			activity.setResult(Activity.RESULT_OK, output);
	    		 			activity.finish();
    		 	        }
    		 		}
    			}
    		);
    	}
	}

    boolean isUsingSAF()
    {
        logger.debug("isUsingSAF() Invoked.");

    	// check Android version just to be safe
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
			if( mSharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) )
            {
				return true;
			}
        }
        return false;
    }

    // only valid if !isUsingSAF()
    String getSaveLocation()
    {
        logger.debug("getSaveLocation() Invoked.");

		String folder_name = mSharedPreferences.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera");
		return folder_name;
    }
    
    public static File getBaseFolder()
    {
        logger.debug("getBaseFolder() Invoked.");

        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getImageFolder(String folder_name)
    {
        logger.debug("getImageFolder(.) Invoked.");

		File file = null;
		if( folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length()-1 )
        {
			// ignore final '/' character
			folder_name = folder_name.substring(0, folder_name.length()-1);
		}
		//if( folder_name.contains("/") )
        // {
		if( folder_name.startsWith("/") )
        {
			file = new File(folder_name);
		}
		else
        {
	        file = new File(getBaseFolder(), folder_name);
		}
        return file;
    }

    // only valid if isUsingSAF()
    Uri getTreeUriSAF()
    {
        logger.debug("getTreeUriSAF() Invoked.");

		Uri treeUri = Uri.parse(mSharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), ""));
		return treeUri;
    }

    // only valid if isUsingSAF()
    // return a human readable name for the SAF save folder location
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	String getImageFolderNameSAF()
    {
        logger.debug("getImageFolderNameSAF() Invoked.");

	    String filename = null;
		Uri uri = getTreeUriSAF();
		logger.info("uri: " + uri);
		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) )
        {
            final String id = DocumentsContract.getTreeDocumentId(uri);
   			logger.info("id: " + id);
            String [] split = id.split(":");
            if( split.length >= 2 )
            {
                String type = split[0];
    		    String path = split[1];

                logger.info("type: " + type);
                logger.info("path: " + path);

        		filename = path;
            }
		}
		return filename;
	}

    // only valid if isUsingSAF()
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	File getFileFromDocumentIdSAF(String id)
    {
        logger.debug("getFileFromDocumentIdSAF(.) Invoked.");

	    File file = null;
        String [] split = id.split(":");
        if( split.length >= 2 )
        {
            String type = split[0];
		    String path = split[1];

		    File [] storagePoints = new File("/storage").listFiles();

            if( "primary".equalsIgnoreCase(type) )
            {
    			final File externalStorage = Environment.getExternalStorageDirectory();
    			file = new File(externalStorage, path);
            }
	        for(int i=0;storagePoints != null && i<storagePoints.length && file==null;i++)
            {
	            File externalFile = new File(storagePoints[i], path);
	            if( externalFile.exists() )
                {
	            	file = externalFile;
	            }
	        }
		}
		return file;
	}

    // valid if whether or not isUsingSAF()
    // but note that if isUsingSAF(), this may return null - it can't be assumed that there is a File corresponding to the SAF Uri
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder()
    {
        logger.debug("getImageFolder() Invoked.");

		File file = null;
    	if( isUsingSAF() )
        {
    		Uri uri = getTreeUriSAF();

    		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) )
            {
                final String id = DocumentsContract.getTreeDocumentId(uri);

        		file = getFileFromDocumentIdSAF(id);
    		}
    	}
    	else
        {
    		String folder_name = getSaveLocation();
    		file = getImageFolder(folder_name);
    	}
    	return file;
    }

	// only valid if isUsingSAF()
	// This function should only be used as a last resort - we shouldn't generally assume that a Uri represents an actual File, and instead.
	// However this is needed for a workaround to the fact that deleting a document file doesn't remove it from MediaStore.
	// See:
	// http://stackoverflow.com/questions/21605493/storage-access-framework-does-not-update-mediascanner-mtp
	// http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework/
    // only valid if isUsingSAF()
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	File getFileFromDocumentUriSAF(Uri uri)
    {
        logger.debug("getFileFromDocumentUriSAF(.) Invoked.");

		logger.info("getFileFromDocumentUriSAF: " + uri);
	    File file = null;
		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) )
        {
            final String id = DocumentsContract.getDocumentId(uri);
   			logger.info("id: " + id);
    		file = getFileFromDocumentIdSAF(id);
		}

        if( file != null )
        {
            logger.info("file: " + file.getAbsolutePath());
        }
        else
        {
            logger.info("failed to find file");
        }

		return file;
	}
	
	private String createMediaFilename(int type, int count, String extension, Date current_date)
    {
        logger.debug("createMediaFilename(....) Invoked.");

        String index = "";
        if( count > 0 )
        {
            index = "_" + count; // try to find a unique filename
        }
		boolean useZuluTime = mSharedPreferences.getString(PreferenceKeys.getSaveZuluTimePreferenceKey(), "local").equals("zulu");
		String timeStamp = null;
		if( useZuluTime )
        {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss'Z'", Locale.US);
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			timeStamp = fmt.format(current_date);
		}
		else
        {
			timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(current_date);
		}
		String mediaFilename = null;
        if( type == MEDIA_TYPE_IMAGE )
        {
    		String prefix = mSharedPreferences.getString(PreferenceKeys.getSavePhotoPrefixPreferenceKey(), "IMG_");
    		mediaFilename = prefix + timeStamp + index + "." + extension;
        }
        else if( type == MEDIA_TYPE_VIDEO )
        {
    		String prefix = mSharedPreferences.getString(PreferenceKeys.getSaveVideoPrefixPreferenceKey(), "VID_");
    		mediaFilename = prefix + timeStamp + index + "." + extension;
        }
        else
        {
        	// throw exception as this is a programming error
   			Helper.Error(logger, "unknown type: " + type);
        	throw new RuntimeException();
        }
        return mediaFilename;
    }
    
    // only valid if !isUsingSAF()
    @SuppressLint("SimpleDateFormat")
	File createOutputMediaFile(int type, String extension, Date current_date) throws IOException
    {
        logger.debug("createOutputMediaFile(...) Invoked.");

    	File mediaStorageDir = getImageFolder();

        // Create the storage directory if it does not exist
        if( !mediaStorageDir.exists() )
        {
            if( !mediaStorageDir.mkdirs() )
            {
       			Helper.Error(logger, "failed to create directory");
        		throw new IOException();
            }
            broadcastFile(mediaStorageDir, false, false, false);
        }

        // Create a media file name
        File mediaFile = null;
        for(int count=0;count<100;count++)
        {
        	String mediaFilename = createMediaFilename(type, count, extension, current_date);
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mediaFilename);
            if( !mediaFile.exists() )
            {
            	break;
            }
        }

		logger.info("getOutputMediaFile returns: " + mediaFile);

		if( mediaFile == null )
        {
            throw new IOException();
        }
        return mediaFile;
    }

    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputMediaFileSAF(int type, String extension, Date current_date) throws IOException
    {
        logger.debug("createOutputMediaFileSAF(...) Invoked.");

    	try
        {
	    	Uri treeUri = getTreeUriSAF();
	    	logger.info("treeUri: " + treeUri);
	        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
	    	logger.info("docUri: " + docUri);
		    String mimeType = "";
	        if( type == MEDIA_TYPE_IMAGE )
            {
	        	if( extension.equals("dng") )
                {
	        		mimeType = "image/dng";
	        		//mimeType = "image/x-adobe-dng";
	        	}
	        	else
	        		mimeType = "image/jpeg";
	        }
	        else if( type == MEDIA_TYPE_VIDEO )
            {
	        	mimeType = "video/mp4";
	        }
	        else
            {
	        	// throw exception as this is a programming error
    			Helper.Error(logger, "unknown type: " + type);
	        	throw new RuntimeException();
	        }
	        // note that DocumentsContract.createDocument will automatically append to the filename if it already exists
	        String mediaFilename = createMediaFilename(type, 0, extension, current_date);
		    Uri fileUri = DocumentsContract.createDocument(context.getContentResolver(), docUri, mimeType, mediaFilename);   
	    	logger.info("returned fileUri: " + fileUri);
			if( fileUri == null )
				throw new IOException();
	    	return fileUri;
    	}
    	catch(IllegalArgumentException e)
        {
    		// DocumentsContract.getTreeDocumentId throws this if URI is invalid
	    	Helper.Error(logger, "createOutputMediaFileSAF failed", e);
		    throw new IOException();
    	}
    }

    static class Media
    {
    	long id;
    	boolean video;
    	Uri uri;
    	long date;
    	int orientation;

    	Media(long id, boolean video, Uri uri, long date, int orientation)
        {
            logger.debug("Media:Media() Invoked.");

    		this.id = id;
    		this.video = video;
    		this.uri = uri;
    		this.date = date;
    		this.orientation = orientation;
    	}
    }
    
    private Media getLatestMedia(boolean video)
    {
        logger.debug("getLatestMedia(.) Invoked.");

		logger.info("getLatestMedia: " + (video ? "video" : "images"));
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED )
        {
			// needed for Android 6, in case users deny storage permission, otherwise we get java.lang.SecurityException from ContentResolver.query()
			// see https://developer.android.com/training/permissions/requesting.html
			// currently we don't bother requesting the permission, as still using targetSdkVersion 22
			// we restrict check to Android 6 or later just in case, see note in LocationSupplier.setupLocationListener()
			Helper.Error(logger, "don't have READ_EXTERNAL_STORAGE permission");
			return null;
		}
    	Media media = null;
		Uri baseUri = video ? Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		//Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
		Uri query = baseUri;
		final int column_id_c = 0;
		final int column_date_taken_c = 1;
		final int column_data_c = 2;
		final int column_orientation_c = 3;
		String [] projection = video ? new String[]
                {VideoColumns._ID, VideoColumns.DATE_TAKEN, VideoColumns.DATA} : new String[]
                {ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.DATA, ImageColumns.ORIENTATION};
		String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg'";
		String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
		Cursor cursor = null;
		try
        {
			cursor = context.getContentResolver().query(query, projection, selection, null, order);
			if( cursor != null && cursor.moveToFirst() )
            {
				logger.info("found: " + cursor.getCount());
				// now sorted in order of date - scan to most recent one in the Open Camera save folder
				boolean found = false;
				File save_folder = isUsingSAF() ? null : getImageFolder();
				String save_folder_string = isUsingSAF() ? null : save_folder.getAbsolutePath() + File.separator;
				logger.info("save_folder_string: " + save_folder_string);
				do
                {
					String path = cursor.getString(column_data_c);
					logger.info("path: " + path);
					// path may be null on Android 4.4!: http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
					// and if isUsingSAF(), it's not clear how we can get the real path, or otherwise tell if an item is a subset of the SAF treeUri
					if( isUsingSAF() || (path != null && path.contains(save_folder_string) ) )
                    {
						logger.info("found most recent in Open Camera folder");
						// we filter files with dates in future, in case there exists an image in the folder with incorrect datestamp set to the future
						// we allow up to 2 days in future, to avoid risk of issues to do with timezone etc
						long date = cursor.getLong(column_date_taken_c);
				    	long current_time = System.currentTimeMillis();
						if( date > current_time + 172800000 )
                        {
							logger.info("skip date in the future!");
						}
						else
                        {
							found = true;
							break;
						}
					}
				} while( cursor.moveToNext() );
				if( !found )
                {
					logger.info("can't find suitable in Open Camera folder, so just go with most recent");
					cursor.moveToFirst();
				}
				long id = cursor.getLong(column_id_c);
				long date = cursor.getLong(column_date_taken_c);
				int orientation = video ? 0 : cursor.getInt(column_orientation_c);
				Uri uri = ContentUris.withAppendedId(baseUri, id);
				logger.info("found most recent uri for " + (video ? "video" : "images") + ": " + uri);
				media = new Media(id, video, uri, date, orientation);
			}
		}
		catch(SQLiteException e)
        {
			// had this reported on Google Play from getContentResolver().query() call
			Helper.Error(logger, "SQLiteException trying to find latest media", e);
		}
		finally
        {
			if( cursor != null )
            {
				cursor.close();
			}
		}
		return media;
    }
    
    Media getLatestMedia()
    {
        logger.debug("getLatestMedia() Invoked.");

		Media image_media = getLatestMedia(false);
		Media video_media = getLatestMedia(true);
		Media media = null;
		if( image_media != null && video_media == null )
        {
			logger.info("only found images");
			media = image_media;
		}
		else if( image_media == null && video_media != null )
        {
			logger.info("only found videos");
			media = video_media;
		}
		else if( image_media != null && video_media != null )
        {
			logger.info("found images and videos");
			logger.info("latest image date: " + image_media.date);
			logger.info("latest video date: " + video_media.date);

			if( image_media.date >= video_media.date )
            {
				logger.info("latest image is newer");
				media = image_media;
			}
			else

            {
				logger.info("latest video is newer");
				media = video_media;
			}
		}
		logger.info("return latest media: " + media);
		return media;
    }
}
