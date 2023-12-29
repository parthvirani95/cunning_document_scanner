package biz.cunning.cunning_document_scanner;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.websitebeaver.documentscanner.DocumentScannerActivity;
import com.websitebeaver.documentscanner.constants.DocumentScannerExtra;

import java.util.ArrayList;
import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class CunningDocumentScannerPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private PluginRegistry.ActivityResultListener delegate;
    private ActivityPluginBinding binding;
    private Result pendingResult;
    private Activity activity;

    private MethodChannel channel;

    private static final int START_DOCUMENT_ACTIVITY = 0x362738;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "cunning_document_scanner");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPictures")) {
            Boolean crop = call.argument("crop");
            if (crop != null) {
                this.pendingResult = result;
                startScan(crop);
            } else {
                result.error("INVALID_ARGUMENT", "The value 'crop' is not a boolean", null);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();

        addActivityResultListener(binding);
    }

    private void addActivityResultListener(@NonNull ActivityPluginBinding binding) {
        this.binding = binding;
        if (this.delegate == null) {
            this.delegate = (requestCode, resultCode, data) -> {
                if (requestCode != START_DOCUMENT_ACTIVITY) {
                    return false;
                }
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // check for errors
                        String error = data != null ? data.getStringExtra("error") : null;
                        if (error != null) {
                            try {
                                throw new Exception("error - " + error);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                        }

                        // get an array with scanned document file paths
                        ArrayList<String> croppedImageResults = data != null ? data.getStringArrayListExtra("croppedImageResults") : null;
                        if (croppedImageResults == null || croppedImageResults.isEmpty()) {
                            try {
                                throw new Exception("No cropped images returned");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        // return a list of file paths
                        // removing file uri prefix as Flutter file will have problems with it
                        List<String> successResponse = new ArrayList<>();
                        for (String path : croppedImageResults) {
                            successResponse.add(path.replaceFirst("file://", ""));
                        }

                        // trigger the success event handler with an array of cropped images
                        this.pendingResult.success(successResponse);
                        return true;
                    case Activity.RESULT_CANCELED:
                        // user closed camera
                        this.pendingResult.success(new ArrayList<String>());
                        return true;
                    default:
                        return false;
                }
            };
        } else {
            binding.removeActivityResultListener(this.delegate);
        }

        binding.addActivityResultListener(delegate);
    }

    private Intent createDocumentScanIntent(boolean crop) {
        Intent documentScanIntent = new Intent(activity, DocumentScannerActivity.class);
        documentScanIntent.putExtra(DocumentScannerExtra.EXTRA_LET_USER_ADJUST_CROP, crop);
        documentScanIntent.putExtra(DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS, crop ? 100 : 1);

        return documentScanIntent;
    }

    private void startScan(boolean crop) {
        Intent intent = createDocumentScanIntent(crop);
        try {
            ActivityCompat.startActivityForResult(this.activity, intent, START_DOCUMENT_ACTIVITY, null);
        } catch (ActivityNotFoundException e) {
            pendingResult.error("ERROR", "FAILED TO START ACTIVITY", null);
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        addActivityResultListener(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        removeActivityResultListener();
    }

    private void removeActivityResultListener() {
        if (this.binding != null && this.delegate != null) {
            this.binding.removeActivityResultListener(this.delegate);
        }
    }
}
