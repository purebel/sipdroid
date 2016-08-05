package org.sipdroid.sipua.ui2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Receiver;

/**
 * Created by jason on 8/3/16.
 */
public class DialActivity extends Activity {

    private Button btnSetup;
    private EditText etPhone;
    private Button btnCall;

    private static AlertDialog m_AlertDlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ui2_dial);
        final Context mContext = this;

        btnSetup = (Button)findViewById(R.id.btn_dial_setup);
        btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(mContext, org.sipdroid.sipua.ui.Settings.class);
                startActivity(myIntent);
            }
        });
        etPhone = (EditText)findViewById(R.id.et_dial_phone);

        btnCall = (Button)findViewById(R.id.btn_dial_call);
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callSomebody();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Receiver.engine(this).registerMore();
    }

    private void callSomebody() {
        if(!Receiver.engine(this).call(etPhone.getText().toString(), true)) {
            m_AlertDlg = new AlertDialog.Builder(this)
                    .setMessage(R.string.notfast)
                    .setTitle(R.string.app_name)
                    .setIcon(R.drawable.icon22)
                    .setCancelable(true)
                    .show();
        }
    }
}
