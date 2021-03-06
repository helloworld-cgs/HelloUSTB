package me.gensh.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import me.gensh.database.DBAccounts;
import me.gensh.database.QueryData;
import me.gensh.helloustb.Browser;
import me.gensh.helloustb.MyApplication;
import me.gensh.helloustb.NetWorkSignIn;
import me.gensh.helloustb.R;
import me.gensh.helloustb.http.HttpClients;
import me.gensh.helloustb.http.Tags;
import me.gensh.helloustb.http.portal.WifiPortal;
import me.gensh.network.HttpRequestTask;
import me.gensh.utils.Const;
import me.gensh.utils.LoginDialog;
import me.gensh.utils.NotificationUtils;
import me.gensh.utils.StringUtils;

/**
 * @author gensh
 */

public class CampusNetworkTest extends IntentService implements HttpRequestTask.OnTaskFinished {
    final static String INTENT_EXTRA_SSID = "ssid";
    final int NETWORK_SING_IN_NOTIFY_ID = 10;
    final int AUTO_SIGN_IN_ERROR_NOTIFY_ID = 12;

    NotificationUtils mNotificationUtils;
    Notification.Builder errorNotifyBuilder;

    public CampusNetworkTest() {
        super("CampusNetworkTest");
    }

    public static void startCampusNetworkService(Context context, String ssid) {
        Intent intent = new Intent(context, CampusNetworkTest.class);
        intent.putExtra(INTENT_EXTRA_SSID, ssid);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationUtils = new NotificationUtils(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {  //todo 似乎打开和关闭wifi都会调用service
        if (intent != null) {
            String ssid = intent.getStringExtra(INTENT_EXTRA_SSID);
            handleActionNetworkCheck(ssid);
        }
    }

    private void handleActionNetworkCheck(String ssid) {
        try {
            Thread.sleep(200);
            final WifiPortal.Status status = WifiPortal.Companion.isWifiSetPortal();
            if (status != null && status.getNeedAuth()) {
                if (!status.isInnerNet()) { //normal auth
                    //start browser to auth.
                    setAuthWithBrowser(R.string.normal_network_notify_title, R.string.normal_network_notify_ticker,
                            R.string.normal_network_notify_content, ssid, WifiPortal.WALLED_GARDEN_URL);
                } else {  //campus net auth.
                    SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
                    String mode = pre.getString(Const.Settings.KEY_NET_SIGN_IN_MODE, Const.Settings.NET_SIGN_IN_NORMAL_MODE);
                    if (Const.Settings.NET_SIGN_IN_SILENT_MODE.equals(mode) &&
                            QueryData.hasAccount(((MyApplication) getApplication()).getDaoSession(), LoginDialog.UserType.NET)) {
                        //静默模式(有密码)
                        autoSignInNetwork();
                    } else if (Const.Settings.NET_SIGN_IN_BROWSER_MODE.equals(mode)) { //浏览器模式
                        setAuthWithBrowser(R.string.network_notify_title, R.string.network_notify_ticker,
                                R.string.network_notify_content, ssid, getString(R.string.sch_net));
                    } else {   //普通模式或者静默模式(无密码)
                        final NotificationUtils notificationUtils = new NotificationUtils(this);
                        final Notification.Builder mBuilder = notificationUtils.getDefaultNotification(getString(R.string.network_notify_title),
                                getString(R.string.network_notify_ticker), getString(R.string.network_notify_content, ssid));

                        Intent notifyIntent = new Intent(this, NetWorkSignIn.class);
                        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//                notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        // Creates the PendingIntent
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(contentIntent);
                        notificationUtils.notify(NETWORK_SING_IN_NOTIFY_ID, mBuilder);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setAuthWithBrowser(@StringRes int title, @StringRes int ticker, @StringRes int body, String ssid, String url) {
        final NotificationUtils notificationUtils = new NotificationUtils(this);
        final Notification.Builder mBuilder = notificationUtils.getDefaultNotification(getString(title), getString(ticker), getString(body, ssid));

        Intent notifyIntent = new Intent(this, Browser.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyIntent.putExtra("url", url);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);
        notificationUtils.notify(NETWORK_SING_IN_NOTIFY_ID, mBuilder);
    }

    int errorTryTimes = 0;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x100) {
                int leftTime = (int) msg.obj - 1;
                if (leftTime <= 0) {  // try again,notice 正在重试
                    errorNotifyBuilder.setContentTitle(getString(R.string.auto_sign_in_retry_title))
                            .setContentText(getString(R.string.auto_sign_in_retry_content));
                    mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
                    autoSignInNetwork();
                } else {
                    errorNotifyBuilder.setContentText(getString(R.string.auto_sign_in_error_content_try_again, leftTime));
                    mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
                    Message message = new Message();
                    message.what = 0x100;
                    message.obj = leftTime;
                    handler.sendMessageDelayed(message, 1000);
                }
            }
        }
    };


    void autoSignInNetwork() {
        if (((MyApplication) getApplication()).CheckNetwork()) {
            DBAccounts account = QueryData.queryAccountByType(((MyApplication) getApplication()).getDaoSession(), LoginDialog.UserType.NET);
            assert account != null;  // account is not null
            String password = StringUtils.decryptWithIv(account.getPasswordEncrypt(), Base64.decode(account.getR(), Base64.DEFAULT));

            Map<String, String> post_params = new LinkedHashMap<>();
            post_params.put("v6ip", LoginDialog.getLocalIpv6Address());
            post_params.put("0MKKey", "123456789");
            post_params.put("DDDDD", account.getUsername());
            post_params.put("upass", password);
            HttpRequestTask httpRequestTask = new HttpRequestTask(this, HttpClients.HTTP_POST,
                    getString(R.string.sch_net), Tags.NETWORK, 0x101, Tags.POST.ID_NETWORK_LOGIN, HttpClients.CHARSET_GB2312, post_params);
            httpRequestTask.setOnTaskFinished(this);
            httpRequestTask.execute();
        } else {
            if (errorNotifyBuilder == null) {
                errorNotifyBuilder = mNotificationUtils.getDefaultNotification(getString(R.string.auto_sign_in_error_title),
                        getString(R.string.auto_sign_in_error_ticker), getString(R.string.auto_sign_in_error_content_hand));
                mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);  //todo
            } else {
                errorNotifyBuilder.setContentTitle(getString(R.string.auto_sign_in_error_title)).setContentText(getString(R.string.auto_sign_in_error_content_hand));
                mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
            }
        }
    }

    @Override
    public void onOk(int what, @NonNull ArrayList<String> data) {
        //todo store data. add notice
        mNotificationUtils.cancel(AUTO_SIGN_IN_ERROR_NOTIFY_ID);
        Toast.makeText(this, R.string.auto_sign_in_success_toast, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPasswordError() {
        errorNotifyBuilder = mNotificationUtils.getDefaultNotification(getString(R.string.auto_sign_in_error_title),
                getString(R.string.auto_sign_in_error_ticker), getString(R.string.auto_sign_in_error_content_hand));
        mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
        Toast.makeText(getBaseContext(), R.string.error_password, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTimeoutError() {
        errorTryTimes++;
        if (errorNotifyBuilder == null) { //第一次超时
            errorNotifyBuilder = mNotificationUtils.getDefaultNotification(getString(R.string.auto_sign_in_error_title),
                    getString(R.string.auto_sign_in_error_ticker), getString(R.string.auto_sign_in_error_content_try_again, 3));// todo
        } else {  //后续超时,次数限制
            if (errorTryTimes > 4) {//todo
                Toast.makeText(getBaseContext(), R.string.connection_timeout, Toast.LENGTH_LONG).show();
                errorNotifyBuilder.setContentTitle(getString(R.string.auto_sign_in_error_title))
                        .setContentText(getString(R.string.auto_sign_in_error_content_try_limit, 4));//todo

                Intent notifyIntent = new Intent(this, NetWorkSignIn.class);
                notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                notifyIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                errorNotifyBuilder.setContentIntent(contentIntent);
                mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
                return;
            } else {
                errorNotifyBuilder.setContentTitle(getString(R.string.auto_sign_in_error_title)).setContentText(getString(R.string.auto_sign_in_error_content_try_again, 3)); //todo
            }
        }
        mNotificationUtils.notify(AUTO_SIGN_IN_ERROR_NOTIFY_ID, errorNotifyBuilder);
        Message message = new Message();
        message.what = 0x100;
        message.obj = 3; //seconds todo
        handler.sendMessageDelayed(message, 1000);
        Toast.makeText(this, R.string.connection_timeout, Toast.LENGTH_LONG).show();
    }
}
