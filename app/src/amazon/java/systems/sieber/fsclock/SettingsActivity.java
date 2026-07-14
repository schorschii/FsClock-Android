package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.Dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;

import com.amazon.device.iap.PurchasingService;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends BaseSettingsActivity {

    FeatureCheck mFc;
    SettingsActivity me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init unlock
        mButtonUnlockSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PurchasingService.purchase("settings");
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();
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

        // init Amazon billing client
        final AmazonPurchasingListener purchasingListener = new AmazonPurchasingListener(this);
        PurchasingService.registerListener(this.getApplicationContext(), purchasingListener);
        final Set<String> productSkus = new HashSet<>();
        productSkus.add("settings");
        PurchasingService.getProductData(productSkus);
        PurchasingService.getPurchaseUpdates(true);
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

    @SuppressWarnings("SameParameterValue")
    private void openUnlockInputBox(final String requestFeature, final String sku) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_inputbox);
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String code = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString().trim();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String response = checkCode(requestFeature, code);
                            if(response == null) throw new Exception();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //JSONObject licenseInfo = new JSONObject(response);
                                    mFc.unlockPurchase(sku);
                                    loadPurchases();
                                }
                            });
                        } catch(Exception e) {
                            if(me == null || me.isFinishing() || me.isDestroyed()) return;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    infoDialog(getString(R.string.activation_failed), e.getMessage()==null?"":e.getMessage());
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

    static class AmazonPurchasingListener implements PurchasingListener {
        SettingsActivity mSettingsActivityReference;
        public AmazonPurchasingListener(SettingsActivity sa) {
            this.mSettingsActivityReference = sa;
        }
        @Override
        public void onUserDataResponse(final UserDataResponse response) {
            final UserDataResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    //iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-userdata", status.toString());
                    break;
            }
        }
        @Override
        public void onProductDataResponse(final ProductDataResponse response) {
            final ProductDataResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(final Product p : response.getProductData().values()) {
                        if(p.getSku().equals("settings")) {
                            mSettingsActivityReference.setupPayButton(p.getSku(), p.getPrice());
                        }
                    }
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Snackbar.make(
                        mSettingsActivityReference.findViewById(R.id.settingsMainView),
                        mSettingsActivityReference.getResources().getString(R.string.amazon_store_not_avail) + " - " +
                                mSettingsActivityReference.getResources().getString(R.string.could_not_fetch_prices),
                        Snackbar.LENGTH_LONG)
                        .show();
                    Log.e("PURCHASE-productdata", status.toString());
                    break;
            }
        }
        @Override
        public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
            final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(final Receipt receipt : response.getReceipts()) {
                        if(!receipt.isCanceled() && receipt.getSku().equals("settings") && !mSettingsActivityReference.mFc.unlockedSettings) {
                            mSettingsActivityReference.mFc.unlockPurchase(receipt.getSku());
                            mSettingsActivityReference.loadPurchases();
                            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
                        }
                    }
                    if(response.hasMore()) {
                        PurchasingService.getPurchaseUpdates(false);
                    }
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-purchaseupdate", status.toString());
                    break;
            }
        }
        @Override
        public void onPurchaseResponse(final PurchaseResponse response) {
            final String requestId = response.getRequestId().toString();
            final String userId = response.getUserData().getUserId();
            final PurchaseResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                case ALREADY_PURCHASED:
                    final Receipt receipt = response.getReceipt();
                    if(!receipt.isCanceled() && receipt.getSku().equals("settings") && !mSettingsActivityReference.mFc.unlockedSettings) {
                        mSettingsActivityReference.mFc.unlockPurchase(receipt.getSku());
                        mSettingsActivityReference.loadPurchases();
                        PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
                    }
                    break;
                case INVALID_SKU:
                    //final Set<String> unavailableSkus = new HashSet<String>();
                    //unavailableSkus.add(response.getReceipt().getSku());
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-purchase", status.toString());
                    break;
            }
        }
    }

}
