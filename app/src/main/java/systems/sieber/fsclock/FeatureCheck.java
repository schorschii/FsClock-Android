package systems.sieber.fsclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.support.api.client.Status;

import java.util.List;

class FeatureCheck {
    private BillingClient mBillingClient;
    private Context mContext;
    private SharedPreferences mSettings;
    private boolean mLoadHuaweiIap;

    FeatureCheck(Context c) {
        mContext = c;
    }

    private featureCheckReadyListener listener = null;
    public interface featureCheckReadyListener {
        void featureCheckReady(boolean fetchSuccess);
    }
    void setFeatureCheckReadyListener(featureCheckReadyListener listener) {
        this.listener = listener;
    }

    void init() {
        // get settings (faster than google play - after purchase done, billing client needs minutes to realize the purchase)
        mSettings = mContext.getSharedPreferences(SettingsActivity.SHARED_PREF_DOMAIN, 0);
        unlockedSettings = mSettings.getBoolean("purchased-settings", false);

        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            if(ai.metaData.getInt("huaweiiap", 0) > 0) mLoadHuaweiIap = true;
        } catch(PackageManager.NameNotFoundException ignored) { }

        // init billing client - get purchases later for other devices
        if(mLoadHuaweiIap) {

            OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
            ownedPurchasesReq.setPriceType(IapClient.PriceType.IN_APP_NONCONSUMABLE);
            Task<OwnedPurchasesResult> task = Iap.getIapClient(mContext).obtainOwnedPurchases(ownedPurchasesReq);
            task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
                @Override
                public void onSuccess(OwnedPurchasesResult result) {
                    if(result != null && result.getInAppPurchaseDataList() != null) {
                        for(int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                            String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                            String InAppSignature = result.getInAppSignature().get(i);
                            try {
                                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                                if(inAppPurchaseDataBean.getPurchaseState() == InAppPurchaseData.PurchaseState.PURCHASED) {
                                    //unlockPurchase(inAppPurchaseDataBean.getProductId());
                                }
                                processPurchasesResult = true;
                                processSubscriptionsResult = true;
                                finish();
                            } catch(Exception ignored) { }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if(e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException)e;
                        Status status = apiException.getStatus();
                        int returnCode = apiException.getStatusCode();
                    } else {
                        // Other external errors
                    }
                }
            });

        } else {

            mBillingClient = BillingClient.newBuilder(mContext)
                    .enablePendingPurchases()
                    .setListener(new PurchasesUpdatedListener() {
                        @Override
                        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> list) {
                        }
                    }).build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // query purchases
                        mBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                    processPurchases(billingResult.getResponseCode(), list);
                                }
                            }
                        );
                        mBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                    processSubscription(billingResult.getResponseCode(), list);
                                }
                            }
                        );
                        isReady = true;
                    } else {
                        isReady = true;
                        if(listener != null) listener.featureCheckReady(false);
                    }
                }
                @Override
                public void onBillingServiceDisconnected() { }
            });

        }

    }

    private Boolean processPurchasesResult = null;
    private Boolean processSubscriptionsResult = null;

    static void acknowledgePurchase(BillingClient client, Purchase purchase) {
        if(!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) { }
            });
        }
    }

    private void processPurchases(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponseCode.OK) {
            for(Purchase p : purchasesList) {
                if(p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    for(String sku : p.getProducts()) {
                        unlockPurchase(sku);
                    }
                    acknowledgePurchase(mBillingClient, p);
                }
            }
            processPurchasesResult = true;
        } else {
            processPurchasesResult = false;
        }
        finish();
    }
    private void processSubscription(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponseCode.OK) {
            for(Purchase p : purchasesList) {
                if(p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    for(String sku : p.getProducts()) {
                        /* if(sku.equals("...")) {
                            // process subscription here...
                            SharedPreferences.Editor editor = mSettings.edit();
                            editor.putString("sync-purchase-token", p.getPurchaseToken());
                            editor.apply();
                            if(p.isAutoRenewing()) {
                                unlockPurchase("sync");
                            }
                        } */
                    }
                    acknowledgePurchase(mBillingClient, p);
                }
            }
            processSubscriptionsResult = true;
        } else {
            processSubscriptionsResult = false;
        }
        finish();
    }

    private void finish() {
        if(processPurchasesResult != null && processSubscriptionsResult != null) {
            if(listener != null) {
                if(processPurchasesResult && processSubscriptionsResult) {
                    listener.featureCheckReady(true);
                } else {
                    listener.featureCheckReady(false);
                }
            }
        }
    }

    boolean isReady = false;

    boolean unlockedSettings = false;

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    void unlockPurchase(String sku) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch(sku) {
            case "settings":
                unlockedSettings = true;
                editor.putBoolean("purchased-settings", true);
                editor.apply();
                break;
        }
    }

}
