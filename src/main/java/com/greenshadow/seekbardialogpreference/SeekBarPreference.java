package com.greenshadow.seekbardialogpreference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;

public class SeekBarPreference extends DialogPreference {
    private SeekBar mSeekBar;
    private TextView value;
    private TestThread testThread;

    private int stashValue = 0;
    private boolean direction = true;

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected View onCreateDialogView() {
        View dialogView = super.onCreateDialogView();
        if (dialogView == null) {
            return null;
        }
        mSeekBar = (SeekBar) dialogView.findViewById(R.id.seek);
        value = (TextView) dialogView.findViewById(R.id.value);
        int deviceValue = getPersistedInt(0); // 从sp读数据
        // deviceValue =  TODO 读取底层数据
        if (deviceValue == -1) {
            mSeekBar.setEnabled(false);
            value.setText("N/A");
        } else {
            value.setText("" + deviceValue);
            mSeekBar.setProgress(deviceValue);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    value.setText("" + progress);

                    // TODO: 应用更改
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // ignore
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // ignore
                }
            });
        }
        return dialogView;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton("测试", this);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        testThread = new TestThread();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (!mSeekBar.isEnabled()) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                Button neutral = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEUTRAL);
                if (neutral.getText().toString().equals("测试")) {
                    setUpTestState(true);
                } else {
                    setUpTestState(false);
                }
                // do not dismiss
                field.set(dialog, false);
            } else {
                // dismiss
                field.set(dialog, true);
                super.onClick(dialog, which);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            getSharedPreferences().edit().putInt(getKey(), mSeekBar.getProgress()).commit();
            // TODO: 保存数据
        }
    }

    private void setUpTestState(boolean state) {
        testThread.isRunning = state;
        direction = state;
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!state);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(!state);

        if (state) {
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setText("停止");
            stashValue = mSeekBar.getProgress();
            mSeekBar.setProgress(0);
            new Thread(testThread).start();
        } else {
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setText("测试");
            mSeekBar.setProgress(stashValue);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (!testThread.isRunning) {
                    return;
                }
                int progress = mSeekBar.getProgress();
                if (progress == 40) { // 到达折返点
                    direction = false;
                }

                if (!direction && progress == 0) { // 线程结束
                    setUpTestState(false);
                    return;
                }

                if (direction) {
                    mSeekBar.setProgress(progress + 1);
                } else {
                    mSeekBar.setProgress(progress - 1);
                }
            }
        }
    };

    private class TestThread implements Runnable {
        private boolean isRunning;

        @Override
        public void run() {
            isRunning = true;
            while (isRunning) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(0);
            }
        }
    }
}
