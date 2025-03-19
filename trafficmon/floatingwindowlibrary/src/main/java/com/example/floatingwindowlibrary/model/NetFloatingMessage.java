package com.example.floatingwindowlibrary.model;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import com.example.floatingwindowlibrary.R;
import com.example.floatingwindowlibrary.util.CommonUtil;

import java.math.BigDecimal;

public class NetFloatingMessage {

    private long receiveTraffic;   //接收流量
    private long sendTraffic;      //发送流量
    private long totalTrafficFirstInit; //第一次计算得到的流量总数
    private long lastTotalSend; //上一次计算得到的发送流量总数
    private long lastTotalReceive; //上一次计算得到的接收流量总数
    private long totalTrafficIncreased; //开启悬浮窗后流量总增量
    private long sendPerSecond;       //发送速率
    private long receivePerSecond;    //接收速率
    private long changePerSecond;     //总速率
    private boolean firstInitFlag = true;
    private boolean hasShowWarning20MB = false;  // 20MB 警告标志

    private final static int M_UNIT_BYTE = 1024 * 1024;
    private final static int KB_UNIT_BYTE = 1024;
    private final static int MILLSECOND_CHNAGE_SECOND_UNIT = 1000;
    private final static long WARNING_TRAFFIC_20MB = 20 * M_UNIT_BYTE;  // 20MB 的字节数
    private static final String CHANNEL_ID = "traffic_warning";
    private static final int NOTIFICATION_ID = 1001;

    private Context context;
    private int period;
    private Handler mainHandler;
    private NotificationManager notificationManager;

    public NetFloatingMessage(Context context, int period) {
        this.context = context;
        this.period = period;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "流量预警",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("显示流量使用预警");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean isFirstInitFlag() {
        return firstInitFlag;
    }

    public void setFirstInitFlag(boolean firstInitFlag) {
        this.firstInitFlag = firstInitFlag;
    }

    public void calculaTraffic() {
        // 获取所有网络接口的总流量
        receiveTraffic = TrafficStats.getTotalRxBytes();
        sendTraffic = TrafficStats.getTotalTxBytes();

        long totalTraffic = sendTraffic + receiveTraffic;

        if (firstInitFlag) {
            totalTrafficFirstInit = totalTraffic;
            lastTotalSend = sendTraffic;
            lastTotalReceive = receiveTraffic;
        } else {
            // 计算每秒的发送和接收速率
            sendPerSecond = (sendTraffic - lastTotalSend) * MILLSECOND_CHNAGE_SECOND_UNIT / period;
            receivePerSecond = (receiveTraffic - lastTotalReceive) * MILLSECOND_CHNAGE_SECOND_UNIT / period;
            
            // 计算总流量增量
            totalTrafficIncreased = totalTraffic - totalTrafficFirstInit;
            
            // 计算总速率
            changePerSecond = (sendTraffic + receiveTraffic - lastTotalSend - lastTotalReceive) * MILLSECOND_CHNAGE_SECOND_UNIT / period;

            // 更新上次的流量数据
            lastTotalSend = sendTraffic;
            lastTotalReceive = receiveTraffic;

            // 检查流量预警
            checkTrafficWarning();
        }
    }

    private void checkTrafficWarning() {
        // 检查是否达到20MB且未显示过警告
        if (!hasShowWarning20MB && totalTrafficIncreased >= WARNING_TRAFFIC_20MB) {
            hasShowWarning20MB = true;
            showWarningNotification();
            showWarningToast("本次已使用流量超过20MB");
        }
    }

    private void showWarningNotification() {
        if (context == null || notificationManager == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("流量使用预警")
                .setContentText("本次已使用流量超过20MB")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showWarningToast(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (context != null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public String getNetSpeed() {
        return getSpeed(changePerSecond);
    }

    public String getTotalTrafficIncreased() {
        if (totalTrafficIncreased == 0) {
            return "0KB";
        }
        double kb = totalTrafficIncreased > M_UNIT_BYTE ? (double) totalTrafficIncreased / M_UNIT_BYTE : (double) totalTrafficIncreased / KB_UNIT_BYTE;
        BigDecimal bd = new BigDecimal(kb);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + getUnit(totalTrafficIncreased);
    }

    public String getSendSpeed() {
        return getSpeed(sendPerSecond);
    }

    public String getReceiveSpeed() {
        return getSpeed(receivePerSecond);
    }

    private String getSpeed(long totalBytes) {
        if (totalBytes == 0) {
            return "0KB/s";
        }
        double kb = totalBytes > M_UNIT_BYTE ? (double) totalBytes / M_UNIT_BYTE : (double) totalBytes / KB_UNIT_BYTE;
        BigDecimal bd = new BigDecimal(kb);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + getUnit(totalBytes) + "/s";
    }

    private String getUnit(long value) {
        return value > M_UNIT_BYTE ? "MB" : "KB";
    }

    public void release() {
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager = null;
        }
        context = null;
    }
}
