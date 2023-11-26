package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsActivity extends BaseSettingsActivity {

    SettingsActivity me;
    FeatureCheck mFc;

    private final static String UNLOCK_CODE_SHOP_URL = "https://georg-sieber.de/?page=app-fsclock";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init manual unlock
        mButtonUnlockSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.fsclock.settings", "settings");
            }
        });
        loadPurchases();
    }

    private void loadPurchases() {
        // disable settings by default
        mLinearLayoutPurchaseContainer.setVisibility(View.VISIBLE);
        enableDisableAllSettings(false);

        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if (mFc.unlockedSettings) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLinearLayoutPurchaseContainer.setVisibility(View.GONE);
                            enableDisableAllSettings(true);
                        }
                    });
                }
            }
        });
        mFc.init();
    }

    @SuppressWarnings("SameParameterValue")
    private void openUnlockInputBox(final String requestFeature, final String sku) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_inputbox);
        ((EditText) ad.findViewById(R.id.editTextInputBox)).setHint(R.string.unlock_code);
        ad.findViewById(R.id.buttonBuyCode).setVisibility(View.VISIBLE);
        ad.findViewById(R.id.buttonBuyCode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBrowser(UNLOCK_CODE_SHOP_URL);
            }
        });
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String text = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString().trim();
                HttpRequest hr = new HttpRequest(getResources().getString(R.string.unlock_api), null);
                ArrayList<KeyValueItem> headers = new ArrayList<>();
                headers.add(new KeyValueItem("X-Unlock-Feature",requestFeature));
                headers.add(new KeyValueItem("X-Unlock-Code",text));
                hr.setRequestHeaders(headers);
                hr.setReadyListener(new HttpRequest.readyListener() {
                    @Override
                    public void ready(int statusCode, String responseBody) {
                        try {
                            if(statusCode != 999) {
                                throw new Exception("Invalid status code: " + statusCode);
                            }
                            JSONObject licenseInfo = new JSONObject(responseBody);
                            mFc.unlockPurchase(sku);
                            loadPurchases();
                        } catch(Exception e) {
                            Log.e("ACTIVATION",  e.getMessage() + " - " + responseBody);
                            if(me == null || me.isFinishing() || me.isDestroyed()) return;
                            AlertDialog ad = new AlertDialog.Builder(me).create();
                            ad.setTitle(getResources().getString(R.string.activation_failed));
                            ad.setMessage(e.getMessage());
                            ad.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(R.string.ok), (DialogInterface.OnClickListener) null);
                            ad.show();
                        }
                    }
                });
                hr.execute();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

}
