package systems.sieber.fsclock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class StorageControl {

    public static String FILENAME_CLOCK_FACE = "clockface.png";
    public static String FILENAME_HOURS_HAND = "hour.png";
    public static String FILENAME_MINUTES_HAND = "minute.png";
    public static String FILENAME_SECONDS_HAND = "second.png";
    public static String FILENAME_BACKGROUND_IMAGE = "bg.png";

    private final Context mContext;

    StorageControl(Context c) {
        mContext = c;
    }

    File getStorage(String filename) {
        File exportDir = mContext.getExternalFilesDir(null);
        return new File(exportDir, filename);
    }

    void processImage(String path, Intent data) {
        if(data == null || data.getData() == null) return;
        try {
            File fl = getStorage(path);
            InputStream inputStream = mContext.getContentResolver().openInputStream(data.getData());
            byte[] targetArray = new byte[inputStream.available()];
            inputStream.read(targetArray);
            FileOutputStream stream = new FileOutputStream(fl);
            stream.write(targetArray);
            stream.flush();
            stream.close();
            scanFile(fl);
        } catch(Exception ignored) { }
    }
    void removeImage(String path) {
        try {
            File fl = getStorage(path);
            fl.delete();
            scanFile(fl);
        } catch(Exception ignored) { }
    }
    boolean existsImage(String path) {
        try {
            File fl = getStorage(path);
            return fl.exists();
        } catch(Exception ignored) { }
        return false;
    }

    void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        mContext.sendBroadcast(scanFileIntent);
    }

}
