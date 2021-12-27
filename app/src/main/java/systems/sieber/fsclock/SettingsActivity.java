package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Toast;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.gson.Gson;
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

import com.amazon.device.iap.PurchasingService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    static final String SHARED_PREF_DOMAIN = "CLOCK";

    static final int PICK_CLOCK_FACE_REQUEST = 1;
    static final int PICK_HOURS_HAND_REQUEST = 2;
    static final int PICK_MINUTES_HAND_REQUEST = 3;
    static final int PICK_SECONDS_HAND_REQUEST = 4;
    static final int PICK_BACKGROUND_REQUEST = 5;

    static final int HUAWEI_PURCHASE_REQUEST = 6666;

    boolean mLoadHuaweiIap = false;
    boolean mLoadAmazonIap = false;

    SettingsActivity me;

    FeatureCheck mFc;
    BillingClient mBillingClient;
    SkuDetails skuDetailsUnlockSettings;

    Gson mGson = new Gson();
    ArrayList<Event> mEvents = new ArrayList<>();
    SharedPreferences mSharedPref;

    LinearLayout mLinearLayoutPurchaseContainer;
    LinearLayout mLinearLayoutSettingsContainer;
    Button mButtonUnlockSettings;
    CheckBox mCheckBoxKeepScreenOn;
    CheckBox mCheckBoxShowBatteryInfo;
    CheckBox mCheckBoxShowBatteryInfoWhenCharging;
    CheckBox mCheckBoxAnalogClockShow;
    CheckBox mCheckBoxAnalogClockShowSeconds;
    CheckBox mCheckBoxAnalogClockOwnColorClockFace;
    CheckBox mCheckBoxAnalogClockOwnColor;
    CheckBox mCheckBoxAnalogClockOwnColorSeconds;
    CheckBox mCheckBoxAnalogClockOwnImage;
    Button mButtonChooseClockFace;
    Button mButtonChooseHoursHand;
    Button mButtonChooseMinutesHand;
    Button mButtonChooseSecondsHand;
    CheckBox mCheckBoxDigitalClockShow;
    CheckBox mCheckBoxDateShow;
    CheckBox mCheckBoxDigitalClockShowSeconds;
    CheckBox mCheckBoxDigitalClock24Format;
    SeekBar mSeekBarRedAnalog;
    SeekBar mSeekBarGreenAnalog;
    SeekBar mSeekBarBlueAnalog;
    View mColorPreviewAnalog;
    SeekBar mSeekBarRedDigital;
    SeekBar mSeekBarGreenDigital;
    SeekBar mSeekBarBlueDigital;
    View mColorPreviewDigital;
    SeekBar mSeekBarRedBack;
    SeekBar mSeekBarGreenBack;
    SeekBar mSeekBarBlueBack;
    View mColorPreviewBack;
    CheckBox mCheckBoxBackgroundOwnImage;
    Button mButtonChooseCustomBackground;
    Button mButtonNewEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        me = this;

        // display version
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            setTitle(getTitle() + " " + pInfo.versionName);
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // find views
        mLinearLayoutPurchaseContainer = findViewById(R.id.linearLayoutInAppPurchase);
        mLinearLayoutSettingsContainer = findViewById(R.id.linearLayoutSettings);
        mButtonUnlockSettings = findViewById(R.id.buttonUnlockSettings);
        mCheckBoxKeepScreenOn = findViewById(R.id.checkBoxKeepScreenOn);
        mCheckBoxShowBatteryInfo = findViewById(R.id.checkBoxShowBatteryInfo);
        mCheckBoxShowBatteryInfoWhenCharging = findViewById(R.id.checkBoxShowBatteryInfoWhenCharging);
        mCheckBoxAnalogClockShow = findViewById(R.id.checkBoxShowAnalogClock);
        mCheckBoxAnalogClockShowSeconds = findViewById(R.id.checkBoxSecondsAnalog);
        mCheckBoxAnalogClockOwnColorClockFace = findViewById(R.id.checkBoxOwnColorAnalogClockFace);
        mCheckBoxAnalogClockOwnColor = findViewById(R.id.checkBoxOwnColorAnalog);
        mCheckBoxAnalogClockOwnColorSeconds = findViewById(R.id.checkBoxOwnColorAnalogSeconds);
        mCheckBoxAnalogClockOwnImage = findViewById(R.id.checkBoxOwnImageAnalog);
        mButtonChooseClockFace = findViewById(R.id.buttonChooseClockFace);
        mButtonChooseHoursHand = findViewById(R.id.buttonChooseHoursHand);
        mButtonChooseMinutesHand = findViewById(R.id.buttonChooseMinutesHand);
        mButtonChooseSecondsHand = findViewById(R.id.buttonChooseSecondsHand);
        mCheckBoxDigitalClockShow = findViewById(R.id.checkBoxShowDigitalClock);
        mCheckBoxDateShow = findViewById(R.id.checkBoxShowDate);
        mCheckBoxDigitalClockShowSeconds = findViewById(R.id.checkBoxSecondsDigital);
        mCheckBoxDigitalClock24Format = findViewById(R.id.checkBox24HrsFormat);
        mSeekBarRedAnalog = findViewById(R.id.seekBarRedAnalog);
        mSeekBarGreenAnalog = findViewById(R.id.seekBarGreenAnalog);
        mSeekBarBlueAnalog = findViewById(R.id.seekBarBlueAnalog);
        mColorPreviewAnalog = findViewById(R.id.viewColorPreviewAnalog);
        mSeekBarRedDigital = findViewById(R.id.seekBarRedDigital);
        mSeekBarGreenDigital = findViewById(R.id.seekBarGreenDigital);
        mSeekBarBlueDigital = findViewById(R.id.seekBarBlueDigital);
        mColorPreviewDigital = findViewById(R.id.viewColorPreviewDigital);
        mSeekBarRedBack = findViewById(R.id.seekBarRedBack);
        mSeekBarGreenBack = findViewById(R.id.seekBarGreenBack);
        mSeekBarBlueBack = findViewById(R.id.seekBarBlueBack);
        mColorPreviewBack = findViewById(R.id.viewColorPreviewBack);
        mCheckBoxBackgroundOwnImage = findViewById(R.id.checkBoxOwnImageBackground);
        mButtonChooseCustomBackground = findViewById(R.id.buttonChooseBackground);
        mButtonNewEvent = findViewById(R.id.buttonNewEvent);

        // init settings
        mSharedPref = getSharedPreferences(SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        mCheckBoxKeepScreenOn.setChecked( mSharedPref.getBoolean("keep-screen-on", true) );
        mCheckBoxShowBatteryInfo.setChecked( mSharedPref.getBoolean("show-battery-info", true) );
        mCheckBoxShowBatteryInfoWhenCharging.setChecked( mSharedPref.getBoolean("show-battery-info-when-charging", false) );
        mCheckBoxAnalogClockShow.setChecked( mSharedPref.getBoolean("show-analog", true) );
        mCheckBoxAnalogClockShowSeconds.setChecked( mSharedPref.getBoolean("show-seconds-analog", true) );
        mCheckBoxAnalogClockOwnColorClockFace.setChecked( mSharedPref.getBoolean("own-color-analog-clock-face", true) );
        mCheckBoxAnalogClockOwnColor.setChecked( mSharedPref.getBoolean("own-color-analog", true) );
        mCheckBoxAnalogClockOwnColorSeconds.setChecked( mSharedPref.getBoolean("own-color-analog-seconds", false) );
        mCheckBoxAnalogClockOwnImage.setChecked( mSharedPref.getBoolean("own-image-analog", false) );
        mCheckBoxDigitalClockShow.setChecked( mSharedPref.getBoolean("show-digital", true) );
        mCheckBoxDateShow.setChecked( mSharedPref.getBoolean("show-date", true) );
        mCheckBoxDigitalClockShowSeconds.setChecked( mSharedPref.getBoolean("show-seconds-digital", true) );
        mCheckBoxDigitalClock24Format.setChecked( mSharedPref.getBoolean("24hrs", true) );
        mSeekBarRedAnalog.setProgress( mSharedPref.getInt("color-red-analog", 255) );
        mSeekBarGreenAnalog.setProgress( mSharedPref.getInt("color-green-analog", 255) );
        mSeekBarBlueAnalog.setProgress( mSharedPref.getInt("color-blue-analog", 255) );
        mSeekBarRedDigital.setProgress( mSharedPref.getInt("color-red", 255) );
        mSeekBarGreenDigital.setProgress( mSharedPref.getInt("color-green", 255) );
        mSeekBarBlueDigital.setProgress( mSharedPref.getInt("color-blue", 255) );
        mSeekBarRedBack.setProgress( mSharedPref.getInt("color-red-back", 0) );
        mSeekBarGreenBack.setProgress( mSharedPref.getInt("color-green-back", 0) );
        mSeekBarBlueBack.setProgress( mSharedPref.getInt("color-blue-back", 0) );
        mCheckBoxBackgroundOwnImage.setChecked( mSharedPref.getBoolean("own-image-back", false) );

        // load events
        Event[] eventsArray = mGson.fromJson(mSharedPref.getString("events",""), Event[].class);
        if(eventsArray != null) {
            mEvents = new ArrayList<>(Arrays.asList(eventsArray));
        }
        displayEvents();

        // init color preview
        initColorPreview();

        // init purchases
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if(ai.metaData.getInt("huaweiiap", 0) > 0) mLoadHuaweiIap = true;
            if(ai.metaData.getInt("amazoniap", 0) > 0) mLoadAmazonIap = true;
        } catch(PackageManager.NameNotFoundException ignored) { }
        mButtonUnlockSettings.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openUnlockInputBox("systems.sieber.fsclock.settings", "settings");
                return false;
            }
        });
        loadPurchases();
    }

    private void enableDisableAllSettings(boolean state) {
        mCheckBoxKeepScreenOn.setEnabled(state);
        mCheckBoxShowBatteryInfo.setEnabled(state);
        mCheckBoxShowBatteryInfoWhenCharging.setEnabled(state);
        mCheckBoxAnalogClockShow.setEnabled(state);
        mCheckBoxAnalogClockShowSeconds.setEnabled(state);
        mCheckBoxAnalogClockOwnColorClockFace.setEnabled(state);
        mCheckBoxAnalogClockOwnColor.setEnabled(state);
        mCheckBoxAnalogClockOwnColorSeconds.setEnabled(state);
        mCheckBoxAnalogClockOwnImage.setEnabled(state);
        mButtonChooseClockFace.setEnabled(state);
        mButtonChooseHoursHand.setEnabled(state);
        mButtonChooseMinutesHand.setEnabled(state);
        mButtonChooseSecondsHand.setEnabled(state);
        mCheckBoxDigitalClockShow.setEnabled(state);
        mCheckBoxDateShow.setEnabled(state);
        mCheckBoxDigitalClockShowSeconds.setEnabled(state);
        mCheckBoxDigitalClock24Format.setEnabled(state);
        mSeekBarRedAnalog.setEnabled(state);
        mSeekBarGreenAnalog.setEnabled(state);
        mSeekBarBlueAnalog.setEnabled(state);
        mSeekBarRedDigital.setEnabled(state);
        mSeekBarGreenDigital.setEnabled(state);
        mSeekBarBlueDigital.setEnabled(state);
        mSeekBarRedBack.setEnabled(state);
        mSeekBarGreenBack.setEnabled(state);
        mSeekBarBlueBack.setEnabled(state);
        mCheckBoxBackgroundOwnImage.setEnabled(state);
        mButtonChooseCustomBackground.setEnabled(state);
        mButtonNewEvent.setEnabled(state);
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
                    mLinearLayoutPurchaseContainer.setVisibility(View.GONE);
                    enableDisableAllSettings(true);
                }
            }
        });
        mFc.init();


        if(mLoadHuaweiIap) {

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

        } else if(mLoadAmazonIap) {

            // init amazon billing client
            final AmazonPurchasingListener purchasingListener = new AmazonPurchasingListener(this);
            PurchasingService.registerListener(this.getApplicationContext(), purchasingListener);
            final Set<String> productSkus = new HashSet<String>();
            productSkus.add("settings");
            PurchasingService.getProductData(productSkus);
            PurchasingService.getPurchaseUpdates(true);

        } else {

            // init Google billing client
            mBillingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(new PurchasesUpdatedListener() {
                        @Override
                        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                            int responseCode = billingResult.getResponseCode();
                            if(responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                                Log.i("BILLING", "onPurchasesUpdated OK");
                                for(Purchase purchase : purchases) {
                                    if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                        mFc.unlockPurchase(purchase.getSku());
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
                        Log.i("BILLING", "onBillingSetupFinished OK");
                        querySkus();
                    } else {
                        Log.e("BILLING", billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                    }
                }
                @Override
                public void onBillingServiceDisconnected() { }
            });

        }
    }
    private ProductInfoReq createProductInfoReq() {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(IapClient.PriceType.IN_APP_NONCONSUMABLE);
        ArrayList<String> productIds = new ArrayList<>();
        productIds.add("settings");
        req.setProductIds(productIds);
        return req;
    }
    private void querySkus() {
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add("settings");
        SkuDetailsParams.Builder params = SkuDetailsParams
                .newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @SuppressWarnings("SwitchStatementWithTooFewBranches")
            @SuppressLint("SetTextI18n")
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    Log.i("BILLING", "onSkuDetailsResponse OK : " + skuDetailsList.size());
                    for(SkuDetails skuDetails : skuDetailsList) {
                        String sku = skuDetails.getSku();
                        String price = skuDetails.getPrice();
                        setupPayButton(sku, price, skuDetails);
                    }
                } else {
                    Log.e("BILLING", billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                }
            }
        });
    }
    private void setupPayButton(String sku, String price) {
        setupPayButton(sku, price, null);
    }
    @SuppressLint("SetTextI18n")
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void setupPayButton(String sku, String price, SkuDetails skuDetails) {
        switch(sku) {
            case "settings":
                if(skuDetails != null) skuDetailsUnlockSettings = skuDetails;
                mButtonUnlockSettings.setEnabled(true);
                mButtonUnlockSettings.setText( getString(R.string.unlock_settings) + " (" + price + ")" );
                break;
        }
    }
    public void doBuyUnlockSettings(View v) {
        if(mLoadHuaweiIap) {
            doBuyHuawei("settings", IapClient.PriceType.IN_APP_NONCONSUMABLE);
        } else if(mLoadAmazonIap) {
            PurchasingService.purchase("settings");
        } else {
            doBuy(skuDetailsUnlockSettings);
        }
    }
    @SuppressWarnings("UnusedReturnValue")
    private BillingResult doBuy(SkuDetails sku) {
        if(sku == null) return null;
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(sku)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
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
            case(PICK_CLOCK_FACE_REQUEST) : {
                processImage(getStorage(this, FILENAME_CLOCK_FACE), data);
                break;
            }
            case(PICK_HOURS_HAND_REQUEST) : {
                processImage(getStorage(this, FILENAME_HOURS_HAND), data);
                break;
            }
            case(PICK_MINUTES_HAND_REQUEST) : {
                processImage(getStorage(this, FILENAME_MINUTES_HAND), data);
                break;
            }
            case(PICK_SECONDS_HAND_REQUEST) : {
                processImage(getStorage(this, FILENAME_SECONDS_HAND), data);
                break;
            }
            case(PICK_BACKGROUND_REQUEST) : {
                processImage(getStorage(this, FILENAME_BACKGROUND_IMAGE), data);
                break;
            }
        }

    }

    private void initColorPreview() {
        // analog color
        mSeekBarRedAnalog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedAnalog.getProgress(), mSeekBarGreenAnalog.getProgress(), mSeekBarBlueAnalog.getProgress(), mColorPreviewAnalog);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarGreenAnalog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedAnalog.getProgress(), mSeekBarGreenAnalog.getProgress(), mSeekBarBlueAnalog.getProgress(), mColorPreviewAnalog);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarBlueAnalog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedAnalog.getProgress(), mSeekBarGreenAnalog.getProgress(), mSeekBarBlueAnalog.getProgress(), mColorPreviewAnalog);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateColorPreview(mSeekBarRedAnalog.getProgress(), mSeekBarGreenAnalog.getProgress(), mSeekBarBlueAnalog.getProgress(), mColorPreviewAnalog);

        // digital color
        mSeekBarRedDigital.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedDigital.getProgress(), mSeekBarGreenDigital.getProgress(), mSeekBarBlueDigital.getProgress(), mColorPreviewDigital);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarGreenDigital.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedDigital.getProgress(), mSeekBarGreenDigital.getProgress(), mSeekBarBlueDigital.getProgress(), mColorPreviewDigital);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarBlueDigital.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedDigital.getProgress(), mSeekBarGreenDigital.getProgress(), mSeekBarBlueDigital.getProgress(), mColorPreviewDigital);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateColorPreview(mSeekBarRedDigital.getProgress(), mSeekBarGreenDigital.getProgress(), mSeekBarBlueDigital.getProgress(), mColorPreviewDigital);

        // background color
        mSeekBarRedBack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedBack.getProgress(), mSeekBarGreenBack.getProgress(), mSeekBarBlueBack.getProgress(), mColorPreviewBack);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarGreenBack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedBack.getProgress(), mSeekBarGreenBack.getProgress(), mSeekBarBlueBack.getProgress(), mColorPreviewBack);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mSeekBarBlueBack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(mSeekBarRedBack.getProgress(), mSeekBarGreenBack.getProgress(), mSeekBarBlueBack.getProgress(), mColorPreviewBack);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateColorPreview(mSeekBarRedBack.getProgress(), mSeekBarGreenBack.getProgress(), mSeekBarBlueBack.getProgress(), mColorPreviewBack);
    }
    private void updateColorPreview(int red, int green, int blue, View v) {
        v.setBackgroundColor(Color.argb(0xff, red, green, blue));
    }

    private void save() {
        SharedPreferences.Editor editor = mSharedPref.edit();

        editor.putBoolean("keep-screen-on", mCheckBoxKeepScreenOn.isChecked());
        editor.putBoolean("show-battery-info", mCheckBoxShowBatteryInfo.isChecked());
        editor.putBoolean("show-battery-info-when-charging", mCheckBoxShowBatteryInfoWhenCharging.isChecked());
        editor.putBoolean("show-analog", mCheckBoxAnalogClockShow.isChecked());
        editor.putBoolean("own-color-analog-clock-face", mCheckBoxAnalogClockOwnColorClockFace.isChecked());
        editor.putBoolean("own-color-analog", mCheckBoxAnalogClockOwnColor.isChecked());
        editor.putBoolean("own-color-analog-seconds", mCheckBoxAnalogClockOwnColorSeconds.isChecked());
        editor.putBoolean("own-image-analog", mCheckBoxAnalogClockOwnImage.isChecked());
        editor.putBoolean("show-digital", mCheckBoxDigitalClockShow.isChecked());
        editor.putBoolean("show-date", mCheckBoxDateShow.isChecked());
        editor.putBoolean("show-seconds-analog", mCheckBoxAnalogClockShowSeconds.isChecked());
        editor.putBoolean("show-seconds-digital", mCheckBoxDigitalClockShowSeconds.isChecked());
        editor.putBoolean("24hrs", mCheckBoxDigitalClock24Format.isChecked());
        editor.putString("events", mGson.toJson(mEvents.toArray()));
        editor.putInt("color-red-analog", mSeekBarRedAnalog.getProgress());
        editor.putInt("color-green-analog", mSeekBarGreenAnalog.getProgress());
        editor.putInt("color-blue-analog", mSeekBarBlueAnalog.getProgress());
        editor.putInt("color-red", mSeekBarRedDigital.getProgress());
        editor.putInt("color-green", mSeekBarGreenDigital.getProgress());
        editor.putInt("color-blue", mSeekBarBlueDigital.getProgress());
        editor.putInt("color-red-back", mSeekBarRedBack.getProgress());
        editor.putInt("color-green-back", mSeekBarGreenBack.getProgress());
        editor.putInt("color-blue-back", mSeekBarBlueBack.getProgress());
        editor.putBoolean("own-image-back", mCheckBoxBackgroundOwnImage.isChecked());

        editor.apply();
    }

    private void displayEvents() {
        LinearLayout llEvents = findViewById(R.id.linearLayoutSettingsEvents);
        llEvents.removeAllViews();
        for(final Event e : mEvents) {
            Button b = new Button(this);
            b.setText(e.toString());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editEvent(e);
                }
            });
            llEvents.addView(b);
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
                        if(statusCode == 999 && responseBody.trim().equals("")) {
                            mFc.unlockPurchase(sku);
                            loadPurchases();
                        } else {
                            Log.i("ACTIVATION", "> " + responseBody);
                            AlertDialog ad = new AlertDialog.Builder(me).create();
                            ad.setMessage(getResources().getString(R.string.activation_failed));
                            ad.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
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

    public void newEvent(View v) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_event);
        //ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setMaxValue(23);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setMaxValue(59);
        ad.findViewById(R.id.buttonNewEventRemove).setVisibility(View.GONE);
        ad.show();
        ad.findViewById(R.id.buttonNewEventOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEvents.add(new Event(
                        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).getValue(),
                        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).getValue(),
                        ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).getText().toString(),
                        ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).getText().toString(),
                        ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).isChecked(),
                        ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).isChecked()
                ));
                ad.dismiss();
                displayEvents();
            }
        });
    }
    public void editEvent(final Event e) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_event);
        //ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setMaxValue(23);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setMaxValue(59);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setValue(e.triggerHour);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setValue(e.triggerMinute);
        ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).setText(e.title);
        ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).setText(e.speakText);
        ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).setChecked(e.playAlarm);
        ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).setChecked(e.showOnScreen);
        ad.show();
        ad.findViewById(R.id.buttonNewEventOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                e.triggerHour = ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).getValue();
                e.triggerMinute = ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).getValue();
                e.title = ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).getText().toString();
                e.speakText = ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).getText().toString();
                e.playAlarm = ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).isChecked();
                e.showOnScreen = ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).isChecked();
                ad.dismiss();
                displayEvents();
            }
        });
        ad.findViewById(R.id.buttonNewEventRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEvents.remove(e);
                ad.dismiss();
                displayEvents();
            }
        });
    }

    public void onClickChooseClockFace(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_CLOCK_FACE_REQUEST);
    }
    public void onClickChooseHoursHand(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_HOURS_HAND_REQUEST);
    }
    public void onClickChooseMinutesHand(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_MINUTES_HAND_REQUEST);
    }
    public void onClickChooseSecondsHand(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_SECONDS_HAND_REQUEST);
    }
    public void onClickChooseBackground(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_BACKGROUND_REQUEST);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_done:
                save();
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    private void processImage(File fl, Intent data) {
        if(data == null || data.getData() == null) return;
        try {
            InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
            byte[] targetArray = new byte[inputStream.available()];
            inputStream.read(targetArray);
            FileOutputStream stream = new FileOutputStream(fl);
            stream.write(targetArray);
            stream.flush();
            stream.close();
            scanFile(fl);
        } catch(Exception ignored) { }
    }

    private void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    public static String FILENAME_CLOCK_FACE = "clockface.png";
    public static String FILENAME_HOURS_HAND = "hour.png";
    public static String FILENAME_MINUTES_HAND = "minute.png";
    public static String FILENAME_SECONDS_HAND = "second.png";
    public static String FILENAME_BACKGROUND_IMAGE = "bg.png";

    public static File getStorage(Context c, String filename) {
        File exportDir = c.getExternalFilesDir(null);
        return new File(exportDir, filename);
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
                    break;
            }
        }
        @Override
        public void onProductDataResponse(final ProductDataResponse response) {
            final ProductDataResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(Product p : response.getProductData().values()) {
                        if(p.getSku().equals("settings")) {
                            mSettingsActivityReference.setupPayButton(p.getSku(), p.getPrice());
                        }
                    }
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    break;
            }
        }
        @Override
        public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
            final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(final Receipt receipt : response.getReceipts()) {
                        if(!receipt.isCanceled() && receipt.getSku().equals("settings")) {
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
                    if(!receipt.isCanceled() && receipt.getSku().equals("settings")) {
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
                    break;
            }
        }
    }


    public final static String URL_GITHUB          = "https://github.com/schorschii/FsClock-Android";
    public final static String APPID_CUSTOMERDB    = "de.georgsieber.customerdb";
    public final static String APPID_REMOTEPOINTER = "systems.sieber.remotespotlight";
    public final static String APPID_BALLBREAK     = "de.georgsieber.ballbreak";
    public final static String URL_OCO             = "https://github.com/schorschii/oco-server";
    public final static String URL_MASTERPLAN      = "https://github.com/schorschii/masterplan";

    public void onClickGithub(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_GITHUB));
        startActivity(browserIntent);
    }
    public void onClickCustomerDatabaseApp(View v) {
        openPlayStore(this, APPID_CUSTOMERDB);
    }
    public void onClickRemotePointerApp(View v) {
        openPlayStore(this, APPID_REMOTEPOINTER);
    }
    public void onClickBallBreakApp(View v) {
        openPlayStore(this, APPID_BALLBREAK);
    }
    public void onClickOco(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_OCO));
        startActivity(browserIntent);
    }
    public void onClickMasterplan(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_MASTERPLAN));
        startActivity(browserIntent);
    }

    public static void openPlayStore(AppCompatActivity a, String appId) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId)));
        } catch (android.content.ActivityNotFoundException anfe) {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
        }
    }

}
