package com.example.floatingwindowlibrary.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.floatingwindowlibrary.model.NetFloatingMessage;
import com.example.floatingwindowlibrary.view.NetFloatingWindow;

import java.lang.ref.WeakReference;

public class TrafficMonitoringManager {
    private static NetFloatingWindow netFloatingWindow;
    private static Handler mainHandler;
    private static Context context;
    private static int period;
    private static boolean isRunning = false;
    private static final UpdateRunnable updateRunnable = new UpdateRunnable();

    private static class UpdateRunnable implements Runnable {
        @Override
        public void run() {
            if (isRunning) {
                windowMessageUpdate();
                if (mainHandler != null) {
                    mainHandler.postDelayed(this, period);
                }
            }
        }
    }

    public static void startPollingTask(Context mContext, int periodTimeMillis) {
        if (isRunning) {
            return;
        }
        
        context = mContext.getApplicationContext(); // 使用 Application Context
        period = periodTimeMillis;
        mainHandler = new Handler(Looper.getMainLooper());
        isRunning = true;
        
        initFloatingView();
        mainHandler.post(updateRunnable);
    }

    private static void initFloatingView() {
        if (context == null) return;
        
        NetFloatingMessage message = new NetFloatingMessage(context, period);
        netFloatingWindow = new NetFloatingWindow(message, context, period);
        netFloatingWindow.showMax();
    }

    private static void windowMessageUpdate() {
        if (!isRunning) return;
        
        if (netFloatingWindow == null) {
            initFloatingView();
        }
        
        if (netFloatingWindow != null) {
            netFloatingWindow.updateWindowDisplayMessage();
        }
    }

    public static void stopPollingTask() {
        isRunning = false;
        if (mainHandler != null) {
            mainHandler.removeCallbacks(updateRunnable);
            mainHandler = null;
        }
        if (netFloatingWindow != null) {
            netFloatingWindow.dismiss();
            netFloatingWindow = null;
        }
        context = null;
    }
}
