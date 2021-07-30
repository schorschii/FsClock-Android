package systems.sieber.fsclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
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

    private final static boolean USE_CACHE = true;

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
                        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> list) {
                        }
                    }).build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // query purchases
                        if(USE_CACHE) {
                            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                            processPurchases(purchasesResult.getResponseCode(), purchasesResult.getPurchasesList());
                            Purchase.PurchasesResult subscriptionResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
                            processSubscription(subscriptionResult.getResponseCode(), subscriptionResult.getPurchasesList());
                            isReady = true;
                        } else {
                            // not implemented anymore
                            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
                                @Override
                                public void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> list) {
                                    //processPurchases(billingResult.getResponseCode(), list);
                                    isReady = true;
                                }
                            });
                            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, new PurchaseHistoryResponseListener() {
                                @Override
                                public void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> list) {
                                    //processSubscription(billingResult.getResponseCode(), list);
                                }
                            });
                        }
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
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) { }
            });
        }
    }

    private void processPurchases(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponseCode.OK) {
            for(Purchase p : purchasesList) {
                if(p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    unlockPurchase(p.getSku());
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
                    if(p.getSku().equals("...")) {
                        // process subscription here...
                        /*
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString("sync-purchase-token", p.getPurchaseToken());
                        editor.apply();
                        if(p.isAutoRenewing()) {
                            unlockPurchase("sync");
                        }
                        */
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
