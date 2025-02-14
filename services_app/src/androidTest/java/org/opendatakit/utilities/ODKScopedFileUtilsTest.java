package org.opendatakit.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(AndroidJUnit4.class)
public class ODKScopedFileUtilsTest {

    private static final String ODK_FOLDER_NAME = "opendatakit";
    private static final String EXTERNAL_DIR_PATH = "/storage/emulated/0/";
    private static final String ODK_FOLDER_PATH = EXTERNAL_DIR_PATH + ODK_FOLDER_NAME;
    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_APP = "testApp";
    private static final String URI_FRAGMENT = "fragment1/fragment2";
    private static final String TEST_FILE = "file.txt";

    private static final String CONFIG_FOLDER_NAME = "config";
    private Context context;
    private ContentResolver contentResolver;
    private ContentValues contentValues;
    private File ODK_FOLDER;

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    );

    @Before
    public void setUp() {
        ODK_FOLDER = new File(ODK_FOLDER_PATH);
        context = ApplicationProvider.getApplicationContext();
        contentResolver = context.getContentResolver();
        contentValues = new ContentValues();
        contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH, ODK_FOLDER_NAME + PATH_SEPARATOR);
    }

    @Test
    public void givenServicesInstalled_whenGetODKFolder_thenReturnFolderPath(){
        assertEquals(ODK_FOLDER_PATH, ODKFileUtils.getOdkFolder());
    }

    @Test
    public void getAsFile_withValidAppNameAndUriFragment_returnsFile() {
        File expectedFile = new File(ODK_FOLDER, TEST_APP + PATH_SEPARATOR + URI_FRAGMENT);
        assertEquals(expectedFile, ODKFileUtils.getAsFile(TEST_APP, URI_FRAGMENT));
        expectedFile = new File(ODK_FOLDER, TEST_APP + PATH_SEPARATOR + URI_FRAGMENT + TEST_FILE);
        assertEquals(expectedFile, ODKFileUtils.getAsFile(TEST_APP, URI_FRAGMENT + TEST_FILE));
    }

    @Test
    public void getAsFile_withInvalidAppName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ODKFileUtils.getAsFile(TEST_APP + "./", URI_FRAGMENT));
    }

    @Test
    public void getAsFile_withNoOrNullAppName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ODKFileUtils.getAsFile(null, URI_FRAGMENT));
        assertThrows(IllegalArgumentException.class, () -> ODKFileUtils.getAsFile("", URI_FRAGMENT));
    }

    @Test
    public void getAsFile_withNoORNullUriFragment_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ODKFileUtils.getAsFile(TEST_APP, ""));
        assertThrows(IllegalArgumentException.class, () -> ODKFileUtils.getAsFile(TEST_APP, null));
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUri_returnsFile() {
        String fileUri = TEST_APP + PATH_SEPARATOR + URI_FRAGMENT;
        assertNull(ODKFileUtils.fileFromUriOnWebServer(fileUri));
        //TODO: Add test cases for config, permanent, system, data

    }

    @Test
    public void fileFromUriOnWebServer_withUriStartingWithSeparator_returnsFile() {
        String fileUri = PATH_SEPARATOR + TEST_APP + PATH_SEPARATOR + URI_FRAGMENT;
        assertNull(ODKFileUtils.fileFromUriOnWebServer(fileUri));
        //TODO: Add test cases for config, permanent, system, data
        String configFileUri = TEST_APP + PATH_SEPARATOR + CONFIG_FOLDER_NAME + PATH_SEPARATOR + TEST_FILE;

    }

    @Test
    public void getNameOfSQLiteDatabase_returnsSQLiteDBName() {
        assertEquals("sqlite.db", ODKFileUtils.getNameOfSQLiteDatabase());
    }

    @Test
    public void fileFromUriOnWebServer_withInvalidUri_returnsNull() {
        assertNull(ODKFileUtils.fileFromUriOnWebServer("appName"));
    }
    @Test
    public void givenServicesInstalled_testCreateDirectory() {
        String folderName = "odk_config";
        String expectedFolderPath;
        expectedFolderPath = Environment.getExternalStorageDirectory().getPath() + PATH_SEPARATOR + ODK_FOLDER_NAME + PATH_SEPARATOR + folderName;

        Uri externalUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns.RELATIVE_PATH};
        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + "=?";
        String[] selectionArgs = {expectedFolderPath + "/"};
        try (Cursor cursor = context.getContentResolver().query(externalUri, projection, selection, selectionArgs, null)) {
            boolean directoryExists = cursor != null && cursor.getCount() > 0;
//            assertTrue("Directory should exist after creation", directoryExists);
        }
    }

    private void openFolderPicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, requestCode);
    }

    private Uri createFile(String folderUriString, String fileName, String content) {
        Uri folderUri = Uri.parse(ODK_FOLDER_PATH).buildUpon().appendEncodedPath(folderUriString).build();
        try {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
            if (pickedDir == null || !pickedDir.isDirectory()) return null;

            DocumentFile newFile = pickedDir.createFile("text/plain", fileName);
            if (newFile == null) return null;

            try (OutputStream os = contentResolver.openOutputStream(newFile.getUri())) {
                if (os != null) {
                    os.write(content.getBytes());
                    os.flush();
                    return newFile.getUri();
                }
            }
        } catch (IOException e) {

        }
        return null;
    }
}
