package org.sipdroid.sipua.ui2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * Created by jason on 8/3/16.
 */
public class InCallActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "[Jason]" + InCallActivity.class.getName();

    private Context mContext;

    final int MSG_ANSWER = 1;
    final int MSG_ANSWER_SPEAKER = 2;
    final int MSG_BACK = 3;
    final int MSG_TICK = 4;
    final int MSG_POPUP = 5;
    final int MSG_ACCEPT = 6;
    final int MSG_ACCEPT_FORCE = 7;

    final int SCREEN_OFF_TIMEOUT = 12000;

    private ViewGroup vgActionCaller;
    private ViewGroup vgActionCallee;

    private ImageButton btnActionHangup;
    private ImageButton btnActionReject;
    private ImageButton btnActionAnswer;

    private TextView tvUserNick;
    private TextView tvStats;
    private TextView tvCodec;
    private TextView tvDuration;

    Thread t;
    boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ui2_incall);

        vgActionCaller = (ViewGroup)findViewById(R.id.ll_action_caller);
        vgActionCallee = (ViewGroup)findViewById(R.id.ll_action_callee);
        btnActionHangup = (ImageButton)findViewById(R.id.btn_chat_hangup);
        btnActionReject = (ImageButton)findViewById(R.id.btn_chat_reject);
        btnActionAnswer = (ImageButton)findViewById(R.id.btn_chat_answer);
        btnActionAnswer.setOnClickListener(this);
        btnActionReject.setOnClickListener(this);
        btnActionHangup.setOnClickListener(this);

        tvUserNick = (TextView)findViewById(R.id.tv_chat_nick);
        tvStats = (TextView)findViewById(R.id.tv_chat_stats);
        tvCodec = (TextView)findViewById(R.id.tv_chat_codec);
        tvDuration = (TextView)findViewById(R.id.tv_chat_duration);
    }

    @Override
    protected void onResume() {
        Log.w(TAG, "onResume()");
        super.onResume();
        switch (Receiver.call_state) {
            case UserAgent.UA_STATE_INCOMING_CALL:
                Log.w(TAG, "Receiver.call_state [UserAgent.UA_STATE_INCOMING_CALL]");
                tvUserNick.setText(Receiver.ccConn.getAddress());
                vgActionCaller.setVisibility(View.GONE);
                vgActionCallee.setVisibility(View.VISIBLE);
                if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE"))
                    if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ON, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ON) &&
                            true)
                        mHandler.sendEmptyMessageDelayed(MSG_ANSWER, 1000);
                    else if ((PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ONDEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ONDEMAND) &&
                            PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_DEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_DEMAND)) ||
                            (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_HEADSET, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_HEADSET) &&
                                    Receiver.headset > 0))
                        mHandler.sendEmptyMessageDelayed(MSG_ANSWER_SPEAKER, 10000);
                break;
            case UserAgent.UA_STATE_INCALL:
                Log.w(TAG, "Receiver.call_state [UserAgent.UA_STATE_INCALL]");
                tvUserNick.setText(Receiver.ccConn.getAddress());
                Receiver.engine(this).speaker(1);
//                mDialerDrawer.close();
//                mDialerDrawer.setVisibility(View.VISIBLE);
                /*
                if (Receiver.docked <= 0)
                    screenOff(true);
                    */
                break;
            case UserAgent.UA_STATE_IDLE:
                Log.w(TAG, "Receiver.call_state [UserAgent.UA_STATE_IDLE]");
                if (!mHandler.hasMessages(MSG_BACK))
                    moveBack();
                break;
        }

        mHandler.sendEmptyMessage(MSG_TICK);
        if (t == null && Receiver.call_state != UserAgent.UA_STATE_IDLE) {
//            mDigits.setText("");
            running = true;
            (t = new Thread() {
                public void run() {
                    int len = 0;
                    long time;
                    ToneGenerator tg = null;

                    if (Settings.System.getInt(getContentResolver(),
                            Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1)
                        tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, (int) (ToneGenerator.MAX_VOLUME * 2 * org.sipdroid.sipua.ui.Settings.getEarGain()));
                    for (; ; ) {
                        if (!running) {
                            t = null;
                            break;
                        }
                        /*
                        if (len != mDigits.getText().length()) {
                            time = SystemClock.elapsedRealtime();
                            if (tg != null)
                                tg.startTone(mToneMap.get(mDigits.getText().charAt(len)));
                            Receiver.engine(Receiver.mContext).info(mDigits.getText().charAt(len++), 250);
                            time = 250 - (SystemClock.elapsedRealtime() - time);
                            try {
                                if (time > 0) sleep(time);
                            } catch (InterruptedException e) {
                            }
                            if (tg != null) tg.stopTone();
                            try {
                                if (running) sleep(250);
                            } catch (InterruptedException e) {
                            }
                            continue;
                        }
                        */
                        mHandler.sendEmptyMessage(MSG_TICK);
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (tg != null) tg.release();
                }
            }).start();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        switch (Receiver.call_state) {
            case UserAgent.UA_STATE_INCOMING_CALL:
                if (!RtpStreamReceiver.isBluetoothAvailable()) Receiver.moveTop();
                break;
            case UserAgent.UA_STATE_IDLE:
//                if (Receiver.ccCall != null)
//                    mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
                mHandler.sendEmptyMessageDelayed(MSG_BACK, Receiver.call_end_reason == -1?
                        2000:5000);
                break;
        }
        if (t != null) {
            running = false;
            t.interrupt();
        }
//        screenOff(false);
//        if (mCallCard.mElapsedTime != null) mCallCard.mElapsedTime.stop();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ANSWER:
                    if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
                        answer();
                    break;
                case MSG_ANSWER_SPEAKER:
                    if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
                        answer();
                        Receiver.engine(mContext).speaker(AudioManager.MODE_NORMAL);
                    }
                    break;
                case MSG_BACK:
                    moveBack();
                    break;
                case MSG_TICK:
                    tvCodec.setText(RtpStreamReceiver.getCodec());
                    if (RtpStreamReceiver.good != 0) {
                        if (RtpStreamReceiver.timeout != 0)
                            tvStats.setText("no data");
                        else if (RtpStreamSender.m > 1)
                            tvStats.setText(Math.round(RtpStreamReceiver.loss/RtpStreamReceiver.good*100)+"%loss, "+
                                    Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
                                    Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
                                    (RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
                        else
                            tvStats.setText(Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
                                    Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
                                    (RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
                        tvStats.setVisibility(View.VISIBLE);
                    } else
                        tvStats.setVisibility(View.GONE);
                    if(Receiver.call_state == UserAgent.UA_STATE_INCALL) {
                        long callDuration = SystemClock.elapsedRealtime() - Receiver.ccCall.base;
                        tvDuration.setText("通话中 " + getCallDuration(callDuration / 1000));
                    }
                    break;
                case MSG_POPUP:
//                    if (mSlidingCardManager != null) mSlidingCardManager.showPopup();
                    break;
                case MSG_ACCEPT:
                case MSG_ACCEPT_FORCE:
//                    setScreenBacklight((float) -1);
                    getWindow().setFlags(0,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    /*
                    if (mDialerDrawer != null) {
                        mDialerDrawer.close();
                        mDialerDrawer.setVisibility(View.VISIBLE);
                    }
                    ContentResolver cr = getContentResolver();
                    if (hapticset) {
                        Settings.System.putInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, haptic);
                        hapticset = false;
                    }
                    */
                    break;
            }
        }
    };

    public void answer() {
        Log.w(TAG, "answer()");
        (new Thread() {
            public void run() {
                Receiver.engine(mContext).answercall();
            }
        }).start();
        if (Receiver.ccCall != null) {
            Receiver.ccCall.setState(Call.State.ACTIVE);
            Receiver.ccCall.base = SystemClock.elapsedRealtime();
//            mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
//            mDialerDrawer.setVisibility(View.VISIBLE);
//            if (mSlidingCardManager != null)
//                mSlidingCardManager.showPopup();
        }
    }

    public void reject() {
        Log.w(TAG, "reject()");
        if (Receiver.ccCall != null) {
            Receiver.stopRingtone();
            Receiver.ccCall.setState(Call.State.DISCONNECTED);
            /*
            mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
            mDialerDrawer.close();
            mDialerDrawer.setVisibility(View.GONE);
            if (mSlidingCardManager != null)
                mSlidingCardManager.showPopup();
                */
        }
        (new Thread() {
            public void run() {
                Receiver.engine(mContext).rejectcall();
            }
        }).start();
    }

    void moveBack() {
        if (Receiver.ccConn != null && !Receiver.ccConn.isIncoming()) {
            // after an outgoing call don't fall back to the contact
            // or call log because it is too easy to dial accidentally from there
            startActivity(Receiver.createHomeIntent());
        }
        onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_chat_hangup:
                Log.w(TAG, "onClick() on view:R.id.btn_chat_hangup");
                Receiver.engine(mContext).rejectcall();
                break;
            case R.id.btn_chat_answer:
                Log.w(TAG, "onClick() on view:R.id.btn_chat_answer");
                if(Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
                    answer();
                }
                break;
            case R.id.btn_chat_reject:
                Log.w(TAG, "onClick() on view:R.id.btn_chat_reject");
                Receiver.engine(mContext).rejectcall();
                break;
        }
    }

    private DecimalFormat df = new DecimalFormat("##00");
    private String getCallDuration(long callDuration) {
        long hours = callDuration / 3600;
        long minutes = callDuration % 3600 / 60;
        long seconds = callDuration % 60;
        StringBuilder sb = new StringBuilder();
        if(hours > 0) {
            sb.append(df.format(hours)).append(":");
        }
        if(minutes >= 0) {
            sb.append(df.format(minutes)).append(":");
        }
        if(seconds >= 0) {
            sb.append(df.format(seconds));
        }
        return sb.toString();
    }
}
