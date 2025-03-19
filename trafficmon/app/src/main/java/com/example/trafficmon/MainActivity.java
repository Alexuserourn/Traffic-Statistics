package com.example.trafficmon;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.floatingwindowlibrary.manager.TrafficMonitoringManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;
    private static final int NET_POLLING_PRIOD_MILLIS = 1000;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        handler = new Handler();
        
        checkAndRequestPermissions();

        final String testUrl = "https://www.baidu.com/";
        TextView clickToLoad = findViewById(R.id.click_load);
        final WebView webView = findViewById(R.id.my_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        clickToLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.loadUrl(testUrl);
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestFloatingWindowPermission();
                return;
            }
        }

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
                return;
            }
        }

        startMonitoring();
    }

    private void startMonitoring() {
        // 延迟初始化悬浮窗，等待Activity完全创建
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startTrafficMonitoring();
            }
        }, 1000);
    }

    private boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请授予使用情况访问权限以监控网速", Toast.LENGTH_LONG).show();
    }

    private void requestFloatingWindowPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
    }

    private void startTrafficMonitoring() {
        try {
            if (!isFinishing()) {
                TrafficMonitoringManager.startPollingTask(this, NET_POLLING_PRIOD_MILLIS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!isFinishing()) {
                Toast.makeText(this, "初始化网速监控失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMonitoring();
            } else {
                Toast.makeText(this, "需要通知权限才能显示流量预警", Toast.LENGTH_LONG).show();
                startMonitoring(); // 即使没有通知权限也启动监控
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this) && hasUsageStatsPermission()) {
                startMonitoring();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                if (hasUsageStatsPermission()) {
                    startMonitoring();
                } else {
                    requestUsageStatsPermission();
                }
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能显示网速", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
            TrafficMonitoringManager.stopPollingTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
