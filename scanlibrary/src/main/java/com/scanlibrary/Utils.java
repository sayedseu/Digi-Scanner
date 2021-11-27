package com.scanlibrary;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Utils {

    public static Uri getUri(Context context, final Bitmap bitmap) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
        String timestamp = simpleDateFormat.format(new Date());

        final String baseDirectory =  context.getString(R.string.base_scantmp_path);
        String filename = "TMP_STG_" + timestamp + ".png";
        String path = Environment.getExternalStorageDirectory() + "/" + baseDirectory;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            String absPath = writeFile(baseDirectory, filename, new FileWritingCallbackS() {
                @Override
                public void write(FileOutputStream out) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
            });

            return Uri.parse(absPath);

        }catch (IOException ioe){
            Log.d("hasan", "getUri: "+ioe.getMessage());
            ioe.printStackTrace();
            return null;
        }
    }

    public static Bitmap getBitmap(Context context, Uri uri) throws IOException {
        final File sd = Environment.getExternalStorageDirectory();
        File src = new File(sd, uri.getPath());
        return BitmapFactory.decodeFile(src.getAbsolutePath());
    }

    public static void setLightNavigationBar(View view, Activity activity){

        int flags = view.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        view.setSystemUiVisibility(flags);
        activity.getWindow().setNavigationBarColor(Color.WHITE);
    }

    public static String writeFile( String baseDirectory, String filename, FileWritingCallbackS callback ) throws IOException {

        final File sd = Environment.getExternalStorageDirectory();
        String absFilename = baseDirectory + filename;
        File dest = new File(sd, absFilename);

        FileOutputStream out = new FileOutputStream(dest);

        callback.write( out );

        out.flush();
        out.close();

        return  absFilename;
    }

    private interface FileWritingCallbackS {
        public void write(FileOutputStream out);
    }

}