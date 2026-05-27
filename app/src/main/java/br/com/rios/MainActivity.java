package br.com.rios;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int PERMISSION_REQUEST = 1002;
    private static final int NATIVE_PDF_REQUEST = 2001;
    private static final int NATIVE_GALLERY_REQUEST = 2002;
    private static final int NATIVE_CAMERA_REQUEST = 2003;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        requestBasicPermissions();
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
        }

        webView.addJavascriptInterface(new RiosAndroidBridge(), "RiosAndroid");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;
                cameraPhotoUri = null;

                Intent chooserIntent = buildChooserIntent(params);
                try {
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception error) {
                    filePathCallback = null;
                    cameraPhotoUri = null;
                    return false;
                }
            }
        });
    }

    public class RiosAndroidBridge {
        @JavascriptInterface
        public void openPdfPicker() {
            runOnUiThread(() -> startNativePicker(NATIVE_PDF_REQUEST, "application/pdf", true));
        }

        @JavascriptInterface
        public void openGalleryPicker() {
            runOnUiThread(() -> startNativePicker(NATIVE_GALLERY_REQUEST, "image/*", true));
        }

        @JavascriptInterface
        public void openCamera() {
            runOnUiThread(() -> startNativeCamera());
        }
    }

    private void startNativePicker(int requestCode, String type, boolean allowMultiple) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    private void startNativeCamera() {
        Intent cameraIntent = buildCameraIntent();
        if (cameraIntent != null) {
            startActivityForResult(cameraIntent, NATIVE_CAMERA_REQUEST);
        }
    }

    private Intent buildChooserIntent(WebChromeClient.FileChooserParams params) {
        String[] acceptTypes = normalizeAcceptTypes(params.getAcceptTypes());
        boolean allowMultiple = params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
        boolean wantsImage = acceptsType(acceptTypes, "image/");
        boolean wantsPdf = acceptsType(acceptTypes, "application/pdf") || acceptsType(acceptTypes, ".pdf");

        Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

        if (wantsImage && !wantsPdf) {
            fileIntent.setType("image/*");
        } else if (wantsPdf && !wantsImage) {
            fileIntent.setType("application/pdf");
        } else {
            fileIntent.setType("*/*");
            fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        }

        Intent cameraIntent = wantsImage ? buildCameraIntent() : null;
        if (cameraIntent == null) {
            return fileIntent;
        }

        Intent chooserIntent = Intent.createChooser(fileIntent, "Selecionar arquivo");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        return chooserIntent;
    }

    private Intent buildCameraIntent() {
        try {
            File photoFile = File.createTempFile("rios_foto_", ".jpg", getExternalCacheDir());
            cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return cameraIntent;
        } catch (IOException error) {
            cameraPhotoUri = null;
            return null;
        }
    }

    private String[] normalizeAcceptTypes(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) {
            return new String[]{"*/*"};
        }

        List<String> normalized = new ArrayList<>();
        for (String type : acceptTypes) {
            if (type == null || type.trim().isEmpty()) {
                continue;
            }
            String[] parts = type.split(",");
            for (String part : parts) {
                String clean = part.trim().toLowerCase();
                if (!clean.isEmpty()) {
                    normalized.add(clean);
                }
            }
        }

        if (normalized.isEmpty()) {
            normalized.add("*/*");
        }

        return normalized.toArray(new String[0]);
    }

    private boolean acceptsType(String[] acceptTypes, String wanted) {
        for (String type : acceptTypes) {
            if (type.equals("*/*") || type.contains(wanted) || wanted.startsWith(type.replace("*", ""))) {
                return true;
            }
        }
        return false;
    }

    private void requestBasicPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        String[] permissions = Build.VERSION.SDK_INT >= 33
                ? new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES}
                : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

        requestPermissions(permissions, PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NATIVE_PDF_REQUEST) {
            enviarArquivosParaHtml("pdf", collectSelectedUris(resultCode, data));
            return;
        }

        if (requestCode == NATIVE_GALLERY_REQUEST || requestCode == NATIVE_CAMERA_REQUEST) {
            enviarArquivosParaHtml("foto", collectSelectedUris(resultCode, data));
            return;
        }

        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) {
            return;
        }

        Uri[] results = collectSelectedUris(resultCode, data);
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
        cameraPhotoUri = null;
    }

    private Uri[] collectSelectedUris(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return null;
        }

        List<Uri> uris = new ArrayList<>();

        if (data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        uris.add(uri);
                    }
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
        }

        if (uris.isEmpty() && cameraPhotoUri != null) {
            uris.add(cameraPhotoUri);
        }

        if (uris.isEmpty()) {
            return null;
        }

        return uris.toArray(new Uri[0]);
    }

    private void enviarArquivosParaHtml(String tipo, Uri[] uris) {
        if (uris == null || uris.length == 0) {
            return;
        }

        try {
            JSONArray arquivos = new JSONArray();
            for (Uri uri : uris) {
                JSONObject item = new JSONObject();
                item.put("name", obterNomeArquivo(uri, tipo));
                item.put("mime", obterMime(uri, tipo));
                item.put("data", lerBase64(uri));
                arquivos.put(item);
            }

            String script = "window.receberArquivosAndroid && window.receberArquivosAndroid("
                    + JSONObject.quote(tipo) + "," + arquivos.toString() + ");";
            webView.evaluateJavascript(script, null);
        } catch (Exception error) {
            webView.evaluateJavascript("alert('Nao foi possivel carregar o arquivo selecionado no Android.');", null);
        } finally {
            cameraPhotoUri = null;
        }
    }

    private String lerBase64(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while (inputStream != null && (read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        if (inputStream != null) {
            inputStream.close();
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private String obterMime(Uri uri, String tipo) {
        String mime = getContentResolver().getType(uri);
        if (mime != null && !mime.trim().isEmpty()) {
            return mime;
        }
        return "pdf".equals(tipo) ? "application/pdf" : "image/jpeg";
    }

    private String obterNomeArquivo(Uri uri, String tipo) {
        String nome = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    nome = cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (nome != null && !nome.trim().isEmpty()) {
            return nome;
        }

        return "pdf".equals(tipo) ? "ordem_servico.pdf" : "foto.jpg";
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }
}
