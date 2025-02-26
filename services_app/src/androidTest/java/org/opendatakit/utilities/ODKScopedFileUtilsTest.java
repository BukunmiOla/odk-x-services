package org.opendatakit.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.logging.WebLogger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(AndroidJUnit4.class)
public class ODKScopedFileUtilsTest {

    private static final String ODK_FOLDER_NAME = "opendatakit";
    private static final String EXTERNAL_DIR_PATH = "/storage/emulated/0/";
    private static final String ODK_FOLDER_PATH = EXTERNAL_DIR_PATH + ODK_FOLDER_NAME;
    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_APP = "testApp/";
    private static final String URI_FRAGMENT = "fragment1/fragment2";
    private static final String TEST_FILE_WITH_SEPARATOR = "/file.txt";
    private static final String TEST_FILE = "file.txt";

    private static final String CONFIG_FOLDER_NAME = "config";
    private static final String SYSTEM_FOLDER_NAME = "system";
    private static final String PERMANENT_FOLDER_NAME = "permanent";
    private static final String DATA_FOLDER_NAME = "data";
    private static final String TABLES_FOLDER_NAME = "tables";
    private static final String OUTPUT_FOLDER_NAME = "output";

    private Context context;
    private ContentResolver contentResolver;


    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    );

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        contentResolver = context.getContentResolver();
        StaticStateManipulator.get().reset();
    }

    @Test
    public void givenServicesInstalled_whenGetODKFolder_thenReturnFolderPath(){
        assertEquals(ODK_FOLDER_PATH, ODKFileUtils.getOdkFolder());
    }

    @Test
    public void getAsFile_withNonExistingAppName_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile(TEST_APP, URI_FRAGMENT)
        );
    }

    @Test
    public void getAsFile_withInvalidAppName_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile(TEST_APP + "./", URI_FRAGMENT)
        );
    }

    @Test
    public void getAsFile_withNoOrNullAppName_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile(null, URI_FRAGMENT)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile("", URI_FRAGMENT)
        );
    }

    @Test
    public void getAsFile_withNoORNullUriFragment_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile(TEST_APP, "")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ODKFileUtils.getAsFile(TEST_APP, null)
        );
    }

    private Uri buildLocalFileUri(String filePath){
        return Uri.parse("file://"+ODK_FOLDER_PATH).buildUpon()
                .appendEncodedPath(filePath).build();
    }

    private void createAppFolder(){
        assertTrue(ODKFileUtils.createFolder(ODK_FOLDER_PATH+PATH_SEPARATOR+TEST_APP));
    }
    @Test
    public void getAsFile_withValidAppNameAndUriFragment_returnsFile() {
        createAppFolder();
        Uri expectedFileUri = buildLocalFileUri(TEST_APP + URI_FRAGMENT);
        assertEquals(
                expectedFileUri,
                DocumentFile.fromFile(ODKFileUtils.getAsFile(TEST_APP, URI_FRAGMENT)).getUri()
        );
        expectedFileUri = buildLocalFileUri(TEST_APP + URI_FRAGMENT + TEST_FILE_WITH_SEPARATOR);
        assertEquals(
                expectedFileUri,
                DocumentFile.fromFile(
                        ODKFileUtils.getAsFile(TEST_APP, URI_FRAGMENT + TEST_FILE_WITH_SEPARATOR)
                ).getUri()
        );
    }

    @Test
    public void fileFromUriOnWebServer_withUnrecognisedFolderUri_returnsNull() {
        createAppFolder();
        assertNull(ODKFileUtils.fileFromUriOnWebServer(TEST_APP + URI_FRAGMENT));
    }

    private void createAppDirectory(String directory) {
        assertTrue(ODKFileUtils.createFolder(ODK_FOLDER_PATH+PATH_SEPARATOR+TEST_APP+PATH_SEPARATOR+directory));
    }

    @Nullable
    private Uri createFile(String folderName, String fileNameWithExt) {
        String path = ODK_FOLDER_PATH + PATH_SEPARATOR +TEST_APP+folderName;
        File directory = new File(path);

        // Ensure the directory exists
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File file = new File(directory, fileNameWithExt);
        try {
            if (file.createNewFile()) {
                Log.e("ODKFileUtilsTest", "File created: " + file.getAbsolutePath());
            } else {
                Log.e("ODKFileUtilsTest", "File already exists.");
            }
            return DocumentFile.fromFile(file).getUri();
        } catch (IOException e) {
            Log.e("ODKFileUtilsTest", "File creation failed", e);
            return null;
        }
    }

    private void assertFileFromUriOnWebServer_withBasicUri_returnsFile(String folderName, String fileName){
        createAppDirectory(folderName);
        Uri expectedFileUri = createFile(folderName, fileName);
        File actualODKFile = ODKFileUtils.fileFromUriOnWebServer(
                TEST_APP + folderName + PATH_SEPARATOR + fileName
        );
        assertNotNull(actualODKFile);
        assertEquals(expectedFileUri, DocumentFile.fromFile(actualODKFile).getUri());
        actualODKFile = ODKFileUtils.fileFromUriOnWebServer(
                PATH_SEPARATOR + TEST_APP + folderName + PATH_SEPARATOR + fileName
        );
        assertNotNull(actualODKFile);
        assertEquals(expectedFileUri, DocumentFile.fromFile(actualODKFile).getUri());
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUriRelatedToConfig_returnsFile() {
        assertFileFromUriOnWebServer_withBasicUri_returnsFile(CONFIG_FOLDER_NAME, TEST_FILE);
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUriRelatedToPermanent_returnsFile() {
        String permanentFileName = "DoNotDelete.txt";
        assertFileFromUriOnWebServer_withBasicUri_returnsFile(
                PERMANENT_FOLDER_NAME, permanentFileName
        );
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUriRelatedToSystem_returnsFile() {
        String systemFileName = "systemFile.txt";
        assertFileFromUriOnWebServer_withBasicUri_returnsFile(SYSTEM_FOLDER_NAME, systemFileName);
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUriRelatedToDataWithoutTable_returnsNull() {
        String systemFileName = "systemFile.txt";
        createAppDirectory(DATA_FOLDER_NAME);
        createFile(DATA_FOLDER_NAME, systemFileName);
        File actualODKFile = ODKFileUtils.fileFromUriOnWebServer(
                TEST_APP + DATA_FOLDER_NAME + PATH_SEPARATOR + systemFileName
        );
        assertNull(actualODKFile);
    }

    @Test
    public void fileFromUriOnWebServer_withBasicUriRelatedToDataWithTable_returnsFile() {
        String systemFileName = "systemFile.txt";
        assertFileFromUriOnWebServer_withBasicUri_returnsFile(
                DATA_FOLDER_NAME + PATH_SEPARATOR + TABLES_FOLDER_NAME, systemFileName
        );
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
    public void givenServicesInstalled_testCreateFolder() {
        String folderName = "odk_config";
        assertTrue(ODKFileUtils.createFolder(ODK_FOLDER_PATH+PATH_SEPARATOR+TEST_APP+folderName));
    }


    private Uri buildDocumentFileUri(String filePath){
        return Uri.parse("content://"+ODK_FOLDER_PATH).buildUpon()
                .appendEncodedPath(filePath).build();
    }

    private Uri buildWebServerFileUri(String filePath){
        return Uri.parse("http://localhost/").buildUpon()
                .appendEncodedPath(filePath).build();
    }

    private void openFolderPicker(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, requestCode);
    }

    @Nullable
    private Uri createDocumentFile(Uri folderUri, String fileName, String content) {
        try {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
            if (pickedDir == null || !pickedDir.isDirectory()) return null;

            DocumentFile newFile = pickedDir.createFile("text/plain", fileName);
            if (newFile == null) return null;

            try (OutputStream os = context.getContentResolver().openOutputStream(newFile.getUri())) {
                if (os != null) {
                    os.write(content.getBytes());
                    os.flush();
                    return newFile.getUri();
                }
            }
        } catch (IOException e) {
            WebLogger.getLogger(TEST_APP).e("File creation failed", e.getMessage());
            Log.e("ODKFileUtilsTest", "File creation failed", e);
        }
        return null;
    }

    private Uri getMediaStoreUri(Context context, String fileName) {
        Uri collection = MediaStore.Files.getContentUri("external");

        String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{fileName};

        Cursor cursor = context.getContentResolver().query(
                collection, new String[]{MediaStore.Files.FileColumns._ID}, selection, selectionArgs, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return ContentUris.withAppendedId(collection, id);
        }

        return null; // File not found
    }

    @After
    public void tearDown() throws IOException {
        ODKFileUtils.deleteDirectory(new File(ODK_FOLDER_PATH+PATH_SEPARATOR+TEST_APP));
    }
}
