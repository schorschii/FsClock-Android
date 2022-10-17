package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;

import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsActivity extends BaseSettingsActivity {

    static final int HUAWEI_PURCHASE_REQUEST = 6666;

    FeatureCheck mFc;
    SettingsActivity me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init manual unlock
        mButtonUnlockSettings.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openUnlockInputBox("systems.sieber.fsclock.settings", "settings");
                return false;
            }
        });

        // init billing library
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
                if(mFc.unlockedSettings) {
                    runOnUiThread(new Runnable(){
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

        // init Huawei billing client
        IapClient iapClient = Iap.getIapClient(this);
        Task<ProductInfoResult> task = iapClient.obtainProductInfo(createProductInfoReq());
        task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
            @Override
            public void onSuccess(ProductInfoResult result) {
                if(result != null && !result.getProductInfoList().isEmpty()) {
                    for(ProductInfo pi : result.getProductInfoList()) {
                        setupPayButton(pi.getProductId(), pi.getPrice());
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e("IAP", e.getMessage());
            }
        });
    }
    private ProductInfoReq createProductInfoReq() {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(IapClient.PriceType.IN_APP_NONCONSUMABLE);
        ArrayList<String> productIds = new ArrayList<>();
        productIds.add("settings");
        req.setProductIds(productIds);
        return req;
    }
    @SuppressLint("SetTextI18n")
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void setupPayButton(String sku, String price) {
        switch(sku) {
            case "settings":
                mButtonUnlockSettings.setEnabled(true);
                mButtonUnlockSettings.setText( getString(R.string.unlock_settings) + " (" + price + ")" );
                break;
        }
    }
    public void doBuyUnlockSettings(View v) {
        doBuyHuawei("settings", IapClient.PriceType.IN_APP_NONCONSUMABLE);
    }
    private void doBuyHuawei(String productId, int type) {
        IapClient mClient = Iap.getIapClient(this);
        Task<PurchaseIntentResult> task = mClient.createPurchaseIntent(createPurchaseIntentReq(type, productId));
        task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
            @Override
            public void onSuccess(PurchaseIntentResult result) {
                try {
                    if(result == null) throw new Exception("Error: Result is null");
                    Status status = result.getStatus();
                    if(status == null) throw new Exception("Error: Status is null");
                    if(!status.hasResolution()) throw new Exception("Error: Intent is null");
                    status.startResolutionForResult(me, HUAWEI_PURCHASE_REQUEST);
                } catch(Exception e) {
                    Toast.makeText(me, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(me, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private PurchaseIntentReq createPurchaseIntentReq(int type, String productId) {
        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setProductId(productId);
        req.setPriceType(type);
        //req.setDeveloperPayload("test");
        return req;
    }
    private void consumeOwnedPurchase(final Context context, String inAppPurchaseData) {
        IapClient mClient = Iap.getIapClient(context);
        Task<ConsumeOwnedPurchaseResult> task = mClient.consumeOwnedPurchase(createConsumeOwnedPurchaseReq(inAppPurchaseData));
        task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
            @Override
            public void onSuccess(ConsumeOwnedPurchaseResult result) {
                // Consume success
                Log.i("IAP", "Pay success, and the product has been delivered");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e("IAP", e.getMessage());
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                if(e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException)e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();
                    Log.e("IAP", "consumeOwnedPurchase fail,returnCode: " + returnCode);
                }
            }
        });
    }
    private ConsumeOwnedPurchaseReq createConsumeOwnedPurchaseReq(String purchaseData) {
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        // Parse purchaseToken from InAppPurchaseData in JSON format.
        try {
            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseData);
            req.setPurchaseToken(inAppPurchaseData.getPurchaseToken());
        } catch(Exception e) {
            Log.e("IAP", "createConsumeOwnedPurchaseReq JSONExeption");
        }
        return req;
    }

    @Override
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;

        switch(requestCode) {
            case(HUAWEI_PURCHASE_REQUEST) : {
                PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data);
                switch(purchaseResultInfo.getReturnCode()) {
                    case OrderStatusCode.ORDER_STATE_SUCCESS:
                        try {
                            //consumeOwnedPurchase(this, purchaseResultInfo.getInAppPurchaseData()); // DO NOT USE THIS FOR NON-CONSUMABLES!
                            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseResultInfo.getInAppPurchaseData());
                            Log.e("IAP", "unlocked "+inAppPurchaseData.getProductId());
                            mFc.unlockPurchase(inAppPurchaseData.getProductId());
                            loadPurchases();
                        } catch(Exception e) {
                            Log.e("IAP", e.getMessage());
                        }
                        return;
                    case OrderStatusCode.ORDER_STATE_CANCEL:
                        // The User cancels payment.
                        //Toast.makeText(this, "user cancel", Toast.LENGTH_SHORT).show();
                        return;
                    case OrderStatusCode.ORDER_PRODUCT_OWNED:
                        //Toast.makeText(this, "you have owned the product", Toast.LENGTH_SHORT).show();
                        try {
                            mFc.unlockPurchase("settings");
                            loadPurchases();
                        } catch(Exception e) {
                            Log.e("IAP", e.getMessage());
                        }
                        return;
                    default:
                        Toast.makeText(this, "Pay failed", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            }
        }

    }

    @SuppressWarnings("SameParameterValue")
    private void openUnlockInputBox(final String requestFeature, final String sku) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_inputbox);
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
