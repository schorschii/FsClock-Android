package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingsActivity extends BaseSettingsActivity {

    SettingsActivity me;
    FeatureCheck mFc;
    BillingClient mBillingClient;
    ProductDetails skuDetailsUnlockSettings;

    @SuppressLint("SetTextI18n")
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

        // init Google billing client
        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                        int responseCode = billingResult.getResponseCode();
                        if(responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for(Purchase purchase : purchases) {
                                if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    for(String sku : purchase.getProducts()) {
                                        mFc.unlockPurchase(sku);
                                    }
                                    FeatureCheck.acknowledgePurchase(mBillingClient, purchase);
                                    loadPurchases();
                                }
                            }
                        } else {
                            Log.e("BILLING", billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                        }
                    }
                }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    querySkus();
                } else {
                    Snackbar.make(
                            findViewById(R.id.settingsMainView),
                            getResources().getString(R.string.play_store_not_avail) + " - " +
                                    getResources().getString(R.string.could_not_fetch_prices),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() { }
        });
    }
    private void querySkus() {
        ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("settings")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        );
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for(final ProductDetails skuDetails : queryProductDetailsResult.getProductDetailsList()) {
                        final String sku = skuDetails.getProductId();
                        final String price = Objects.requireNonNull(skuDetails.getOneTimePurchaseOfferDetails()).getFormattedPrice();
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                setupPayButton(sku, price, skuDetails);
                            }
                        });
                    }
                } else {
                    Log.e("BILLING", billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                }
            }
        });
    }
    @SuppressLint("SetTextI18n")
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void setupPayButton(String sku, String price, ProductDetails skuDetails) {
        switch(sku) {
            case "settings":
                if(skuDetails != null) skuDetailsUnlockSettings = skuDetails;
                mButtonUnlockSettings.setEnabled(true);
                mButtonUnlockSettings.setText( getString(R.string.unlock_settings) + " (" + price + ")" );
                break;
        }
    }
    public void doBuyUnlockSettings(View v) {
        doBuy(skuDetailsUnlockSettings);
    }
    @SuppressWarnings("UnusedReturnValue")
    private BillingResult doBuy(ProductDetails sku) {
        if(sku == null) return null;
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(sku)
                    // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                    // for a list of offers that are available to the user
                    //.setOfferToken(selectedOfferToken)
                    .build()
        );
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
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
