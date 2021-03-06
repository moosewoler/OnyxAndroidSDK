/**
 * 
 */
package com.onyx.android.sdk.data.cms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.onyx.android.sdk.data.AscDescOrder;
import com.onyx.android.sdk.data.SortOrder;
import com.onyx.android.sdk.data.util.ProfileUtil;
import com.onyx.android.sdk.data.util.RefValue;

/**
 * @author joy
 * 
 */
public class OnyxCmsCenter
{
    private static final String TAG = "OnyxCMSCenter";

    public static final String PROVIDER_AUTHORITY = "com.onyx.android.sdk.OnyxCmsProvider";
    
    /**
     * 
     * @param context
     * @param sortOrder
     * @param ascOrder
     * @param selectionArgs
     * @param limitNumber how many records to select, -1 for no limitation, or you can use a large enough number such as Interger.MAX_VALUE
     * @param result
     * @return
     */
    public static boolean getLibraryItems(Context context, SortOrder sortOrder, AscDescOrder ascOrder, 
            String selectionArgs, int limitNumber, Collection<OnyxLibraryItem> result)
    {
        Cursor c = null;

        String selection = null;
        String[] filetype = selectionArgs.split(",");
        if (filetype != null && filetype.length > 0) {
            selection = "type=?";
            for(int i = 0; i < filetype.length - 1; i++) {
                selection = selection.concat(" OR type=?");   
            }
        }

        try {
            String ascDescSort = null;
            if(ascOrder == AscDescOrder.Asc) {
                ascDescSort = "ASC";
            }
            else {
                ascDescSort = "DESC";
            }

            String sort_order = null;
            if (sortOrder == SortOrder.Name) {
                sort_order = OnyxLibraryItem.Columns.NAME + " " + ascDescSort;
            }
            else if(sortOrder == SortOrder.Size) {
                sort_order = OnyxLibraryItem.Columns.SIZE + " " + ascDescSort +
                        "," + OnyxLibraryItem.Columns.NAME + " ASC";
            }
            else if(sortOrder == SortOrder.FileType){
                sort_order = OnyxLibraryItem.Columns.TYPE + " " + ascDescSort + 
                        "," + OnyxLibraryItem.Columns.NAME + " ASC";
            }
            else if(sortOrder == SortOrder.CreationTime) {
                sort_order = OnyxLibraryItem.Columns.LAST_CHANGE + " " + ascDescSort +
                        "," + OnyxLibraryItem.Columns.NAME + " ASC";
            }
            
            if (limitNumber != -1) {
                if (sort_order == null) {
                    sort_order = " LIMIT " + limitNumber;
                }
                else {
                    sort_order += (" LIMIT " + limitNumber);
                }
            }
            
            ProfileUtil.start(TAG, "query library items");
            c = context.getContentResolver().query(OnyxLibraryItem.CONTENT_URI, null, selection, filetype, sort_order);
            ProfileUtil.end(TAG, "query library items");

            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }

            ProfileUtil.start(TAG, "read db result");
            readLibraryItemCursor(c, result);
            ProfileUtil.end(TAG, "read db result");

            Log.d(TAG, "items loaded, count: " + result.size());

            return true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean getLibraryItems(Context context,
            Collection<OnyxLibraryItem> result)
    {
        return getLibraryItems(context, SortOrder.None, AscDescOrder.Asc, null, -1, result);
    }

    /**
     * reading data from DB to metadata, old data in metadata will be overwritten 
     * 
     * @param context
     * @param data
     * @return
     */
    public static boolean getMetadata(Context context, OnyxMetadata data)
    {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(OnyxMetadata.CONTENT_URI,
                    null,
                    OnyxMetadata.Columns.NATIVE_ABSOLUTE_PATH + "=?" + " AND " +
                            OnyxMetadata.Columns.SIZE + "=" + data.getSize() + " AND " +
                            OnyxMetadata.Columns.LAST_MODIFIED + "=" + data.getLastModified().getTime(),
                    new String[] { data.getNativeAbsolutePath() }, null);
            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }
            if (c.moveToFirst()) {
                OnyxMetadata.Columns.readColumnData(c, data);
                return true;
            }

            return false;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    /**
     * get metadata of file, return null if failed
     * 
     * @param context
     * @param file
     * @return
     */
    public static OnyxMetadata getMetadata(Context context, String filePath)
    {
        File file = new File(filePath);
        
        OnyxMetadata data = new OnyxMetadata();
        data.setNativeAbsolutePath(file.getAbsolutePath());
        data.setSize(file.length());
        data.setlastModified(new Date(file.lastModified()));
        
        ProfileUtil.start(TAG, "getMetadata query");
        if (OnyxCmsCenter.getMetadata(context, data)) {
            ProfileUtil.end(TAG, "getMetadata query");
            return data;
        }
        ProfileUtil.end(TAG, "getMetadata query");
        
        return null;
    }

    public static boolean getMetadatas(Context context,
            Collection<OnyxMetadata> result)
    {
        Cursor c = null;
        try {
            ProfileUtil.start(TAG, "query metadatas");
            c = context.getContentResolver().query(OnyxMetadata.CONTENT_URI,
                    null, null, null, null);
            ProfileUtil.end(TAG, "query metadatas");

            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }

            ProfileUtil.start(TAG, "read db result");
            readMetadataCursor(c, result);
            ProfileUtil.end(TAG, "read db result");
            
            return true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean insertMetadata(Context context, OnyxMetadata data)
    {
        Log.d(TAG, "insert metadata: " + data.getNativeAbsolutePath());
        
        int n = context.getContentResolver().delete(OnyxMetadata.CONTENT_URI,
                OnyxMetadata.Columns.NATIVE_ABSOLUTE_PATH + "=?",
                new String[] { data.getNativeAbsolutePath() });
        if (n > 0) {
            Log.w(TAG, "delete obsolete metadata: " + n);
        }
        
        Uri result = context.getContentResolver().insert(
                OnyxMetadata.CONTENT_URI,
                OnyxMetadata.Columns.createColumnData(data));
        if (result == null) {
            return false;
        }

        String id = result.getLastPathSegment();
        if (id == null) {
            return false;
        }

        data.setId(Long.parseLong(id));

        return true;
    }

    public static boolean updateMetadata(Context context, OnyxMetadata data)
    {
        Log.d(TAG, "update metadata: " + data.getNativeAbsolutePath());
        
        Uri row = Uri.withAppendedPath(OnyxMetadata.CONTENT_URI,
                String.valueOf(data.getId()));
        int count = context.getContentResolver().update(row,
                OnyxMetadata.Columns.createColumnData(data), null, null);
        if (count <= 0) {
            return false;
        }

        assert (count == 1);
        return true;
    }

    public static boolean getRecentReadings(Context context,
            Collection<OnyxMetadata> result)
    {
        Cursor c = null;
        try {
            ProfileUtil.start(TAG, "query recent readings");
            c = context.getContentResolver().query(
                    OnyxMetadata.CONTENT_URI,
                    null,
                    "(" + OnyxMetadata.Columns.LAST_ACCESS
                            + " is not null) and ("
                            + OnyxMetadata.Columns.LAST_ACCESS + "!='') and ("
                            + OnyxMetadata.Columns.LAST_ACCESS + "!=0)", null,
                    OnyxMetadata.Columns.LAST_ACCESS + " desc");
            ProfileUtil.end(TAG, "query recent readings");

            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }

            ProfileUtil.start(TAG, "read db result");
            readMetadataCursor(c, result);
            ProfileUtil.end(TAG, "read db result");

            Log.d(TAG, "items loaded, count: " + result.size());

            return true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    public static boolean getBookmarks(Context context, String md5, ArrayList<OnyxBookmark> result)
    {
        Cursor c = null;
        try {
            ProfileUtil.start(TAG, "query bookmarks");
            c = context.getContentResolver().query(OnyxBookmark.CONTENT_URI,
                    null,
                    OnyxBookmark.Columns.MD5 + "='" + md5 + "'", 
                    null, null);
            ProfileUtil.end(TAG, "query bookmarks");

            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }

            ProfileUtil.start(TAG, "read db result");
            readBookmarkCursor(c, result);
            ProfileUtil.end(TAG, "read db result");
            
            Log.d(TAG, "items loaded, count: " + result.size());
            
            return true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    public static boolean insertBookmark(Context context, OnyxBookmark bookmark)
    {
        Uri result = context.getContentResolver().insert(
                OnyxBookmark.CONTENT_URI,
                OnyxBookmark.Columns.createColumnData(bookmark));
        if (result == null) {
            return false;
        }

        String id = result.getLastPathSegment();
        if (id == null) {
            return false;
        }

        bookmark.setId(Long.parseLong(id));

        return true;
    }
    
    public static boolean deleteBookmark(Context context, OnyxBookmark bookmark)
    {
        Uri row = Uri.withAppendedPath(OnyxBookmark.CONTENT_URI, String.valueOf(bookmark.getId()));
        int count = context.getContentResolver().delete(row, null, null);
        if (count <= 0) {
            return false;
        }
        
        assert(count == 1);
        return true;
    }
    
    public static boolean getAnnotations(Context context, String md5, ArrayList<OnyxAnnotation> result)
    {
        Cursor c = null;
        try {
            ProfileUtil.start(TAG, "query annotations");
            c = context.getContentResolver().query(OnyxAnnotation.CONTENT_URI,
                    null, 
                    OnyxAnnotation.Columns.MD5 + "='" + md5 + "'", 
                    null, null);
            ProfileUtil.end(TAG, "query annotations");

            if (c == null) {
                Log.d(TAG, "query database failed");
                return false;
            }

            ProfileUtil.start(TAG, "read db result");
            readAnnotationCursor(c, result);
            ProfileUtil.end(TAG, "read db result");
            
            Log.d(TAG, "items loaded, count: " + result.size());
            
            return true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    public static boolean insertAnnotation(Context context, OnyxAnnotation annotation)
    {
        Uri result = context.getContentResolver().insert(
                OnyxAnnotation.CONTENT_URI,
                OnyxAnnotation.Columns.createColumnData(annotation));
        if (result == null) {
            return false;
        }

        String id = result.getLastPathSegment();
        if (id == null) {
            return false;
        }

        annotation.setId(Long.parseLong(id));

        return true;
    }
    
    public static boolean updateAnnotation(Context context, OnyxAnnotation annotation)
    {
        Uri row = Uri.withAppendedPath(OnyxAnnotation.CONTENT_URI, String.valueOf(annotation.getId()));
        int count = context.getContentResolver().update(row,
                OnyxAnnotation.Columns.createColumnData(annotation), null, null);
        if (count <= 0) {
            return false;
        }

        assert (count == 1);
        return true;
    }
    
    public static boolean deleteAnnotation(Context context, OnyxAnnotation annotation)
    {
        Uri row = Uri.withAppendedPath(OnyxAnnotation.CONTENT_URI, String.valueOf(annotation.getId()));
        int count = context.getContentResolver().delete(row, null, null);
        if (count <= 0) {
            return false;
        }
        
        assert(count == 1);
        return true;
    }

    public static boolean getThumbnail(Context context, OnyxMetadata metadata, 
            OnyxThumbnail.ThumbnailKind thumbnailKind, RefValue<Bitmap> result)
    {
        if (metadata == null) {
            assert (false);
            return false;
        }

        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    OnyxThumbnail.CONTENT_URI,
                    null,
                    OnyxThumbnail.Columns.SOURCE_MD5 + "='" + metadata.getMD5() + "' AND " +
                    OnyxThumbnail.Columns.THUMBNAIL_KIND + "='" + thumbnailKind.toString() + "'", 
                    null, null);
            if (c == null) {
                Log.w(TAG, "query thumbnail failed: " + thumbnailKind.toString() + ", try to query original thumbnail");
                c = context.getContentResolver().query(
                        OnyxThumbnail.CONTENT_URI,
                        null,
                        OnyxThumbnail.Columns.SOURCE_MD5 + "='" + metadata.getMD5() + "' AND " +
                        OnyxThumbnail.Columns.THUMBNAIL_KIND + "='" + OnyxThumbnail.ThumbnailKind.Original.toString() + "'", 
                        null, null);
                if (c == null) {
                    Log.w(TAG, "query original thumbnail failed");
                    return false;
                }
            }
            
            if (c.moveToFirst()) {
                OnyxThumbnail data = OnyxThumbnail.Columns.readColumnData(c);
                Uri row = Uri.withAppendedPath(OnyxThumbnail.CONTENT_URI,
                        String.valueOf(data.getId()));
                InputStream is = null;
                try {
                    is = context.getContentResolver().openInputStream(row);
                    if (is == null) {
                        Log.w(TAG, "openInputStream failed");
                        return false;
                    }
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inPurgeable = true;
                    Bitmap b = BitmapFactory.decodeStream(is, null, o);
                    
                    result.setValue(b);
                    return true;
                } catch (Throwable tr) {
                    Log.e(TAG, "exception", tr);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            Log.w(TAG, e);
                        }
                    }
                }
            }

            return false;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    public static boolean getThumbnail(Context context, OnyxMetadata metadata, RefValue<Bitmap> result)
    {
        return getThumbnail(context, metadata, OnyxThumbnail.ThumbnailKind.Original, result);
    }

    /**
     * 
     * @param context
     * @param data
     * @param thumbnail
     * @return
     */
    public static boolean insertThumbnail(Context context, OnyxMetadata data,
            Bitmap thumbnail)
    {
        if (data == null) {
            assert (false);
            return false;
        }
        
        if (!insertThumbnailHelper(context, thumbnail, data.getMD5(), OnyxThumbnail.ThumbnailKind.Original)) {
            return false;
        }
        
        insertThumbnailHelper(context, thumbnail, data.getMD5(), OnyxThumbnail.ThumbnailKind.Large);
        insertThumbnailHelper(context, thumbnail, data.getMD5(), OnyxThumbnail.ThumbnailKind.Middle);
        insertThumbnailHelper(context, thumbnail, data.getMD5(), OnyxThumbnail.ThumbnailKind.Small);
        
        return true;
    }

    private static void readLibraryItemCursor(Cursor c,
            Collection<OnyxLibraryItem> result)
    {
        if (c.moveToFirst()) {
            result.add(OnyxLibraryItem.Columns.readColumnData(c));

            while (c.moveToNext()) {
                if (Thread.interrupted()) {
                    return;
                }

                result.add(OnyxLibraryItem.Columns.readColumnData(c));
            }
        }
    }

    private static void readMetadataCursor(Cursor c,
            Collection<OnyxMetadata> result)
    {
        if (c.moveToFirst()) {
            result.add(OnyxMetadata.Columns.readColumnData(c));

            while (c.moveToNext()) {
                if (Thread.interrupted()) {
                    return;
                }

                result.add(OnyxMetadata.Columns.readColumnData(c));
            }
        }
    }
    
    private static void readBookmarkCursor(Cursor c,
            Collection<OnyxBookmark> result)
    {
        if (c.moveToFirst()) {
            result.add(OnyxBookmark.Columns.readColumnData(c));

            while (c.moveToNext()) {
                if (Thread.interrupted()) {
                    return;
                }

                result.add(OnyxBookmark.Columns.readColumnData(c));
            }
        }
    }
    
    private static void readAnnotationCursor(Cursor c,
            Collection<OnyxAnnotation> result)
    {
        if (c.moveToFirst()) {
            result.add(OnyxAnnotation.Columns.readColumnData(c));

            while (c.moveToNext()) {
                if (Thread.interrupted()) {
                    return;
                }

                result.add(OnyxAnnotation.Columns.readColumnData(c));
            }
        }
    }
    
    private static boolean insertThumbnailHelper(Context context, Bitmap bmp,
            String md5, OnyxThumbnail.ThumbnailKind thumbnailKind)
    {
        Bitmap thumbnail = bmp;
        OutputStream os = null;
        
        try {
            switch (thumbnailKind) {
            case Original:
                break;
            case Large:
                thumbnail = OnyxThumbnail.createLargeThumbnail(bmp);
                break;
            case Middle:
                thumbnail = OnyxThumbnail.createMiddleThumbnail(bmp);
                break;
            case Small:
                thumbnail = OnyxThumbnail.createSmallThumbnail(bmp);
                break;
            default:
                assert(false);
                break;
            }

            Uri result = context.getContentResolver().insert(
                    OnyxThumbnail.CONTENT_URI,
                    OnyxThumbnail.Columns.createColumnData(md5, thumbnailKind));
            if (result == null) {
                Log.d(TAG, "insertThumbnail db insert failed");
                return false;
            }

            os = context.getContentResolver().openOutputStream(result);
            if (os == null) {
                Log.d(TAG, "openOutputStream failed");
                return false;
            }
            thumbnail.compress(CompressFormat.JPEG, 85, os);
            Log.d(TAG, "insertThumbnail success");
            return true;
        } catch (FileNotFoundException e) {
            Log.w(TAG, e);
            return false;
        } finally {
            if (thumbnail != null && thumbnail != bmp) {
                thumbnail.recycle();
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }
    
    public static  boolean getFavoriteItems(Context context, Collection<OnyxMetadata> result)
    {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(OnyxMetadata.CONTENT_URI, null,"favorite=1", null, null);
            if (c == null) {
                return false;
            }
            
            readMetadataCursor(c, result);
            return true;
        }
        finally {
            if (c != null) {
                c.close();
            }
        }

    }

    public static boolean searchBooks (Context context, Collection<OnyxLibraryItem> result, String arg)
    {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(OnyxLibraryItem.CONTENT_URI, null,
                    "name like ?",
                    new String[] { "%" + arg + "%" }, null);
            if (c == null) {
                return false;
            }
            
            readLibraryItemCursor(c, result);
            return true;
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
