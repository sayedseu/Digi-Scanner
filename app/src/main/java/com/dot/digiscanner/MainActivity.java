package com.dot.digiscanner;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dot.digiscanner.fileView.FLAdapter;
import com.dot.digiscanner.persistance.Document;
import com.dot.digiscanner.persistance.DocumentViewModel;
import com.dot.digiscanner.utils.DialogUtil;
import com.dot.digiscanner.utils.DialogUtilCallback;
import com.dot.digiscanner.utils.FileIOUtils;
import com.dot.digiscanner.utils.FileWritingCallback;
import com.dot.digiscanner.utils.PDFWriterUtil;
import com.dot.digiscanner.utils.PermissionUtil;
import com.dot.digiscanner.utils.UIUtil;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final Context c = this;
    LiveData<List<Document>> liveData;
    private AdView mAdView;
    private FLAdapter fileAdapter;
    private final List<Uri> scannedBitmaps = new ArrayList<>();
    private DocumentViewModel viewModel;
    private LinearLayout emptyLayout;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.rw);

        UIUtil.setLightNavigationBar(recyclerView, this);
        PermissionUtil.ask(this);

        final String baseStorageDirectory = getApplicationContext().getString(R.string.base_storage_path);
        FileIOUtils.mkdir(baseStorageDirectory);

        final String baseStagingDirectory = getApplicationContext().getString(R.string.base_staging_path);
        FileIOUtils.mkdir(baseStagingDirectory);

        final String scanningTmpDirectory = getApplicationContext().getString(R.string.base_scantmp_path);
        FileIOUtils.mkdir(scanningTmpDirectory);

        this.emptyLayout = findViewById(R.id.empty_list);

        viewModel = ViewModelProviders.of(this).get(DocumentViewModel.class);

        fileAdapter = new FLAdapter(viewModel, this);
        recyclerView.setAdapter(fileAdapter);

        liveData = viewModel.getAllDocuments();
        liveData.observe(this, new Observer<List<Document>>() {
            @Override
            public void onChanged(@Nullable List<Document> documents) {

                if (documents.size() > 0) {
                    emptyLayout.setVisibility(View.GONE);

                } else {
                    emptyLayout.setVisibility(View.VISIBLE);
                }

                fileAdapter.setData(documents);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    public void goToSearch(MenuItem mi) {
//        if (mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        } else {
//            Intent intent = new Intent(this, SearchableActivity.class);
//            startActivityForResult(intent, 0);
//        }
//
//        mInterstitialAd.setAdListener(new AdListener() {
//            @Override
//            public void onAdClosed() {
//                Intent intent = new Intent(MainActivity.this, SearchableActivity.class);
//                startActivityForResult(intent, 0);
//            }
//
//            @Override
//            public void onAdFailedToLoad(int i) {
//                Intent intent = new Intent(MainActivity.this, SearchableActivity.class);
//                startActivityForResult(intent, 0);
//            }
//
//        });

    }


    public void goToPreferences(MenuItem mi) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, 0);
    }

    public void openCamera(View v) {
        scannedBitmaps.clear();

        String stagingDirPath = getApplicationContext().getString(R.string.base_staging_path);
        String scanningTmpDirectory = getApplicationContext().getString(R.string.base_scantmp_path);

        FileIOUtils.clearDirectory(stagingDirPath);
        FileIOUtils.clearDirectory(scanningTmpDirectory);


        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, ScanConstants.OPEN_CAMERA);

        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivityForResult(intent, ScanConstants.START_CAMERA_REQUEST_CODE, options.toBundle());
    }

    private void saveBitmap(final Bitmap bitmap, final boolean addMore) {

        final String baseDirectory = getApplicationContext().getString(addMore ? R.string.base_staging_path : R.string.base_storage_path);
        final File sd = Environment.getExternalStorageDirectory();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
        final String timestamp = simpleDateFormat.format(new Date());

        if (addMore) {

            try {

                String filename = "SCANNED_STG_" + timestamp + ".png";

                FileIOUtils.writeFile(baseDirectory, filename, new FileWritingCallback() {
                    @Override
                    public void write(FileOutputStream out) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                });

                bitmap.recycle();
                System.gc();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        } else {

            DialogUtil.askUserFilaname(c, null, null, new DialogUtilCallback() {

                @Override
                public void onSave(String textValue, String category) {

                    try {

                        final PDFWriterUtil pdfWriter = new PDFWriterUtil();

                        String stagingDirPath = getApplicationContext().getString(R.string.base_staging_path);

                        List<File> stagingFiles = FileIOUtils.getAllFiles(stagingDirPath);
                        for (File stagedFile : stagingFiles) {
                            pdfWriter.addFile(stagedFile);
                        }

                        pdfWriter.addBitmap(bitmap);

                        String filename = "SCANNED_" + timestamp + ".pdf";
                        FileIOUtils.writeFile(baseDirectory, filename, new FileWritingCallback() {
                            @Override
                            public void write(FileOutputStream out) {
                                try {
                                    pdfWriter.write(out);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });


                        fileAdapter.notifyDataSetChanged();

                        FileIOUtils.clearDirectory(stagingDirPath);

                        SimpleDateFormat simpleDateFormatView = new SimpleDateFormat("dd-MM-yyyy hh:mm");
                        final String timestampView = simpleDateFormatView.format(new Date());

                        Document newDocument = new Document();
                        newDocument.setName(textValue);
                        newDocument.setCategory(category);
                        newDocument.setPath(filename);
                        newDocument.setScanned(timestampView);
                        newDocument.setPageCount(pdfWriter.getPageCount());
                        viewModel.saveDocument(newDocument);

                        pdfWriter.close();

                        bitmap.recycle();
                        System.gc();

                    } catch (IOException ioe) {
                        ioe.printStackTrace();

                    }

                }
            });

        }
    }

    private void savePdf() {

        final String baseDirectory = getApplicationContext().getString(R.string.base_storage_path);
        final File sd = Environment.getExternalStorageDirectory();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
        final String timestamp = simpleDateFormat.format(new Date());


        DialogUtil.askUserFilaname(c, null, null, new DialogUtilCallback() {

            @Override
            public void onSave(String textValue, String category) {
                try {

                    final PDFWriterUtil pdfWriter = new PDFWriterUtil();

                    String stagingDirPath = getApplicationContext().getString(R.string.base_staging_path);

                    List<File> stagingFiles = FileIOUtils.getAllFiles(stagingDirPath);
                    for (File stagedFile : stagingFiles) {
                        pdfWriter.addFile(stagedFile);
                    }

                    String filename = "SCANNED_" + timestamp + ".pdf";
                    FileIOUtils.writeFile(baseDirectory, filename, new FileWritingCallback() {
                        @Override
                        public void write(FileOutputStream out) {
                            try {
                                pdfWriter.write(out);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });


                    fileAdapter.notifyDataSetChanged();

                    FileIOUtils.clearDirectory(stagingDirPath);

                    SimpleDateFormat simpleDateFormatView = new SimpleDateFormat("dd-MM-yyyy hh:mm");
                    final String timestampView = simpleDateFormatView.format(new Date());

                    Document newDocument = new Document();
                    newDocument.setName(textValue);
                    newDocument.setCategory(category);
                    newDocument.setPath(filename);
                    newDocument.setScanned(timestampView);
                    newDocument.setPageCount(pdfWriter.getPageCount());
                    viewModel.saveDocument(newDocument);

                    pdfWriter.close();

                    System.gc();

                } catch (IOException ioe) {
                    ioe.printStackTrace();

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ScanConstants.PICKFILE_REQUEST_CODE || requestCode == ScanConstants.START_CAMERA_REQUEST_CODE) &&
                resultCode == Activity.RESULT_OK) {


            boolean saveMode = data.getExtras().containsKey(ScanConstants.SAVE_PDF) ? data.getExtras().getBoolean(ScanConstants.SAVE_PDF) : Boolean.FALSE;
            if (saveMode) {
                savePdf();

            } else {
                Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
              //  boolean doScanMore = data.getExtras().getBoolean(ScanConstants.SCAN_MORE);

                final File sd = Environment.getExternalStorageDirectory();
                File src = new File(sd, uri.getPath());
                Bitmap bitmap = BitmapFactory.decodeFile(src.getAbsolutePath());
                Log.d("hasan", "onActivityResult: "+bitmap+ " "+src);

//                saveBitmap(bitmap, doScanMore);
//
//                if (doScanMore) {
//                    scannedBitmaps.add(uri);
//                    Intent intent = new Intent(this, MultiPageActivity.class);
//                    intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, ScanConstants.OPEN_CAMERA);
//
//                    startActivityForResult(intent, ScanConstants.START_CAMERA_REQUEST_CODE);
//                }

            }
        }
    }
}
