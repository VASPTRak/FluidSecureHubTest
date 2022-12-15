package com.TrakEngineering.FluidSecureHubTest;

import static com.azure.android.maps.control.options.AnimationOptions.animationDuration;
import static com.azure.android.maps.control.options.AnimationOptions.animationType;
import static com.azure.android.maps.control.options.CameraOptions.center;
import static com.azure.android.maps.control.options.CameraOptions.zoom;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.TrakEngineering.FluidSecureHubTest.enity.AddTankFromAPP_entity;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.retrofit.AzureMapApi;
import com.TrakEngineering.FluidSecureHubTest.retrofit.BusProvider;
import com.TrakEngineering.FluidSecureHubTest.retrofit.ErrorEvent;
import com.TrakEngineering.FluidSecureHubTest.retrofit.Interface;
import com.TrakEngineering.FluidSecureHubTest.retrofit.ServerEvent;
import com.TrakEngineering.FluidSecureHubTest.retrofit.ServerResponse;
import com.azure.android.maps.control.AzureMap;
import com.azure.android.maps.control.events.OnClick;
import com.azure.android.maps.control.options.AnimationType;
import com.azure.android.maps.control.options.StyleOptions;
import com.github.xizzhu.simpletooltip.ToolTip;
import com.github.xizzhu.simpletooltip.ToolTipView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.azure.android.maps.control.AzureMaps;
import com.azure.android.maps.control.MapControl;
import com.azure.android.maps.control.Control;
import com.azure.android.maps.control.controls.ZoomControl;
import com.azure.android.maps.control.layer.SymbolLayer;
import com.azure.android.maps.control.source.DataSource;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class AddNewLinkToCloud extends AppCompatActivity implements LifecycleObserver, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private AddnewLink_ViewModel addnewlinkViewModel;
    private String TAG = this.getClass().getSimpleName();
    private Spinner Spinner_tankNumber, spin_pulseRatio;
    private EditText edt_linkname, edt_pumpOnTime, edt_pumpOffTime; //, edt_username, edt_enter_password
    private EditText edt_LinkNewName, edt_StreetAddress; // edt_UnitsMeasured, edt_Pulses
    private Button btn_cancel, btn_done, btnAddNewTank, btnMap;
    private String expression = "^[a-zA-Z0-9-_ ]*$";
    private ProgressDialog pd;
    //private CheckBox chkShowHidePassword;
    private ImageButton iBtn_LinkName, iBtn_NewName, iBtn_AddNewTank, iBtn_PumpOnTime, iBtn_PumpOffTime, iBtn_PulseRatio, iBtn_StreetAddress;
    public String selectedLongitude = "0", selectedLatitude = "0";
    public String selectedPulseRatio = "";
    Bundle mySavedInstanceState;
    public int selectedProductPosition = 0;
    public Dialog addTankDialog;
    private String SubscriptionKeyForAzureMap = "";
    MapControl mapControl;
    SymbolLayer layer;
    public ArrayAdapter<String> autoAdapter;
    public AutoCompleteTextView tv_SearchAddress;
    ArrayList<String> searchResults = new ArrayList<>();
    ArrayList<HashMap<String, String>> listOfPositions = new ArrayList<>();
    //public boolean isAddressSelected = false;
    public String selectedAddress = "";

    /*static {
        AzureMaps.setSubscriptionKey("FJ29LaayVFiy20Hp29hEe5mG7F6QTbhfyV6wuWwG7Sg");
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mySavedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_add_new_link_to_cloud);

        btn_done = (Button) findViewById(R.id.btn_done);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        edt_linkname = (EditText) findViewById(R.id.edt_linkname);
        edt_pumpOnTime = (EditText) findViewById(R.id.edt_pumpOnTime);
        edt_pumpOffTime = (EditText) findViewById(R.id.edt_pumpOffTime);
        //edt_username = (EditText) findViewById(R.id.edt_username);
        //edt_enter_password = (EditText) findViewById(R.id.edt_enter_password);
        Spinner_tankNumber = (Spinner) findViewById(R.id.spin_tanknumber);
        Spinner_tankNumber.setOnItemSelectedListener(this);
        //chkShowHidePassword = (CheckBox) findViewById(R.id.chkShowHidePassword);
        edt_LinkNewName = (EditText) findViewById(R.id.edt_LinkNewName);
        //edt_UnitsMeasured = (EditText) findViewById(R.id.edt_UnitsMeasured);
        //edt_Pulses = (EditText) findViewById(R.id.edt_Pulses);
        edt_StreetAddress = (EditText) findViewById(R.id.edt_StreetAddress);
        AppConstants.isLocationSelected = false;

        iBtn_LinkName = (ImageButton) findViewById(R.id.iBtn_LinkName);
        iBtn_NewName = (ImageButton) findViewById(R.id.iBtn_NewName);
        btnAddNewTank = (Button) findViewById(R.id.btnAddNewTank);
        iBtn_AddNewTank = (ImageButton) findViewById(R.id.iBtn_AddNewTank);
        iBtn_PumpOnTime = (ImageButton) findViewById(R.id.iBtn_PumpOnTime);
        iBtn_PumpOffTime = (ImageButton) findViewById(R.id.iBtn_PumpOffTime);
        spin_pulseRatio = (Spinner) findViewById(R.id.spin_pulseRatio);

        spin_pulseRatio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPulseRatio = spin_pulseRatio.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        iBtn_PulseRatio = (ImageButton) findViewById(R.id.iBtn_PulseRatio);
        iBtn_StreetAddress = (ImageButton) findViewById(R.id.iBtn_StreetAddress);
        btnMap = (Button) findViewById(R.id.btnMap);

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AddnewLink_ViewModel(getApplication()));

        // Get the ViewModel.
        addnewlinkViewModel = new ViewModelProvider(this).get(AddnewLink_ViewModel.class);
        LiveData<String> myRandomNumber = addnewlinkViewModel.getNumber();
        myRandomNumber.hasActiveObservers();

        myRandomNumber.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
               AlertDialogBox(AddNewLinkToCloud.this, s);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " SaveLinkFromAPP Response: " + s);
            }
        });

        LiveData<Boolean> IsUpdating = addnewlinkViewModel.getIsUpdating();
        IsUpdating.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isupdating) {
                if (isupdating){
                    ShowProgressDialog();
                }else{
                    HideProgressDialog();
                }
            }
        });

        // Get Tank and Product details
        new GetTanksAndProducts().execute();

        /*ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, addnewlinkViewModel.getSpinnerList());
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner_tankNumber.setAdapter(aa);*/ // OLD code
        //BindTankList();

        ArrayAdapter<CharSequence> aaPR = ArrayAdapter.createFromResource(this, R.array.pulseRatios, R.layout.spinner_item_list);
        aaPR.setDropDownViewResource(R.layout.spinner_item_list);
        spin_pulseRatio.setAdapter(aaPR);
        spin_pulseRatio.setSelection(1);

        btnAddNewTank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertAddNewTankDialog();
            }
        });

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateData()) {
                    String UnitsMeasured = "1";
                    String Pulses = "20";
                    try {
                        selectedPulseRatio = spin_pulseRatio.getSelectedItem().toString();
                        if (!selectedPulseRatio.isEmpty()) {
                            String[] split = selectedPulseRatio.split("/");

                            if (split.length > 0) {
                                Pulses = split[0].trim();
                                UnitsMeasured = split[1];
                            }
                        }
                    } catch (Exception e) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " SaveLinkFromAPP Exception while getting PulseRatio: " + e.getMessage());
                    }
                    addnewlinkViewModel.ProcessData(edt_linkname.getText().toString().trim(), edt_pumpOnTime.getText().toString().trim(),
                            edt_pumpOffTime.getText().toString().trim(), edt_LinkNewName.getText().toString().trim(),
                            UnitsMeasured, Pulses, edt_StreetAddress.getText().toString().trim());

                    //, edt_username.getText().toString().trim(), edt_enter_password.getText().toString().trim()
                    //, edt_UnitsMeasured.getText().toString().trim(), edt_Pulses.getText().toString().trim()
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SetSubscriptionKeyForAzureMap();

                alertMapDialog();
            }
        });

        SetToolTips();

        /*chkShowHidePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // show password
                    edt_enter_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    // hide password
                    edt_enter_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });*/

    }

    private void SetSubscriptionKeyForAzureMap() {
        try {
            SharedPreferences sharedPref = this.getSharedPreferences(AppConstants.sharedPref_AzureMapDetails, Context.MODE_PRIVATE);
            SubscriptionKeyForAzureMap = sharedPref.getString("SubscriptionKey", "");

            AzureMaps.setSubscriptionKey(SubscriptionKeyForAzureMap);

            //Set the language to be used by Azure Maps.
            if (!AppConstants.LANG_PARAM.isEmpty()) {
                AzureMaps.setLanguage(AppConstants.LANG_PARAM.replace(":", ""));
            }
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " SetSubscriptionKeyForAzureMap Exception: " + ex.getMessage());
        }
    }

    public void BindTankList() {

        ArrayAdapter aa = new ArrayAdapter(this, R.layout.spinner_item_list, addnewlinkViewModel.getTankSpinnerList());
        aa.setDropDownViewResource(R.layout.spinner_item_list);
        Spinner_tankNumber.setAdapter(aa);

    }

    public void alertAddNewTankDialog() {
        try {
            final Dialog dialog = new Dialog(AddNewLinkToCloud.this);
            dialog.setTitle("");
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_add_new_tank);
            dialog.setCancelable(false);
            addTankDialog = dialog;

            EditText edt_TankNumber = (EditText) dialog.findViewById(R.id.edt_TankNumber);
            EditText edt_TankName = (EditText) dialog.findViewById(R.id.edt_TankName);
            Spinner spin_Product = (Spinner) dialog.findViewById(R.id.spin_Product);

            ArrayAdapter aaPd = new ArrayAdapter(this, R.layout.spinner_item_list, addnewlinkViewModel.getProductList());
            aaPd.setDropDownViewResource(R.layout.spinner_item_list);
            spin_Product.setAdapter(aaPd);

            spin_Product.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //String selected_val = spin_Product.getSelectedItem().toString();
                    selectedProductPosition = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            Button btnSaveTank = (Button) dialog.findViewById(R.id.btnSaveMap);
            Button btnCancelTank = (Button) dialog.findViewById(R.id.btnCancelMap);

            btnSaveTank.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (validateTankData(edt_TankNumber, edt_TankName)) {
                        String TankNumber = edt_TankNumber.getText().toString();
                        String TankName = edt_TankName.getText().toString();
                        String FuelTypeId = addnewlinkViewModel.getFuelTypeIdByFuelType(selectedProductPosition);

                        new AddTankFromAPP().execute(TankNumber, TankName, FuelTypeId);
                        //dialog.dismiss();
                    }
                }
            });

            btnCancelTank.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " alertAddNewTankDialog Exception: " + ex.getMessage());
        }
    }

    public void alertMapDialog() {
        try {

            final Dialog dialog = new Dialog(AddNewLinkToCloud.this);
            dialog.setTitle("");
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_map);
            dialog.setCancelable(false);

            mapControl = dialog.findViewById(R.id.map_control);
            mapControl.onCreate(mySavedInstanceState);

            // Address search functionality
                /*for (int i = 0; i < 5; i++) {
                    searchResults.add("ABC" + i);
                    searchResults.add("PQR" + i);
                }*/
            tv_SearchAddress = dialog.findViewById(R.id.tv_SearchAddress);
            autoAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, searchResults);
            tv_SearchAddress.setAdapter(autoAdapter);

            tv_SearchAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //isAddressSelected = true;
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!s.toString().isEmpty()) {
                        selectedAddress = "";
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().length() > 2 && !selectedAddress.equalsIgnoreCase(s.toString())) { // isAddressSelected) {
                        retrieveAddressDataFromApi(s.toString());
                    }
                }
            });
            //=================================================

            //Wait until the map resources are ready.
            mapControl.onReady(map -> {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " <map resources are ready.>");
                InitializeMap(map);

                tv_SearchAddress.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //Log.d(TAG, "Selected address is " + searchResults.get(position).toString());
                        selectedAddress = searchResults.get(position).toString();
                        //isAddressSelected = false;
                        tv_SearchAddress.dismissDropDown();
                        if (listOfPositions != null) {
                            if (listOfPositions.size() > 0) {
                                String latitude = listOfPositions.get(position).get("latitude");
                                String longitude = listOfPositions.get(position).get("longitude");
                                if (latitude != null && longitude != null) {
                                    map.layers.remove(layer);
                                    AddSymbolLayerToMap(map, Double.parseDouble(longitude), Double.parseDouble(latitude));
                                }
                            }
                        }
                    }
                });
            });

            Button btnSaveMap = (Button) dialog.findViewById(R.id.btnSaveMap);
            Button btnCancelMap = (Button) dialog.findViewById(R.id.btnCancelMap);

            btnSaveMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppConstants.Latitude = selectedLatitude;
                    AppConstants.Longitude = selectedLongitude;
                    AppConstants.isLocationSelected = true;
                    GetAddressByLatLng(Double.parseDouble(AppConstants.Latitude), Double.parseDouble(AppConstants.Longitude));
                    mapControl.onDestroy();
                    dialog.dismiss();
                }
            });

            btnCancelMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mapControl.onDestroy();
                    dialog.dismiss();
                }
            });

            mapControl.onResume();
            dialog.show();
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " alertMapDialog Exception: " + ex.getMessage());
        }
    }

    private void InitializeMap(AzureMap map) {
        try {
            AddSymbolLayerToMap(map, Double.parseDouble(AppConstants.Longitude), Double.parseDouble(AppConstants.Latitude));

            /*//Set the camera of the map.
            map.setCamera(
                    center(Point.fromLngLat(Double.parseDouble(AppConstants.Longitude), Double.parseDouble(AppConstants.Latitude))),
                    zoom(15),
                    animationType(AnimationType.FLY),
                    animationDuration(2000)
            );*/

            // Add controls on MAP
            map.controls.add(
                    new Control[]{
                            new ZoomControl()
                    }
            );

            // Handle events
            map.events.add((OnClick) (lat, lon) -> {
                //Map clicked.
                map.layers.remove(layer);
                AddSymbolLayerToMap(map, lon, lat);
                return true;
            });
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " InitializeMap Exception: " + ex.getMessage());
        }
    }

    private void AddSymbolLayerToMap(AzureMap map, double longitude, double latitude) {
        try {
            selectedLatitude = String.valueOf(latitude);
            selectedLongitude = String.valueOf(longitude);
            //Create a data source and add it to the map.
            DataSource source = new DataSource();
            map.sources.add(source);
            //Create a point and add it to the data source.
            source.add(Point.fromLngLat(longitude, latitude));
            //Create a symbol layer to render icons and/or text at points on the map.
            layer = new SymbolLayer(source);
            //Add the layer to the map.
            map.layers.add(layer);

            //Set the camera of the map.
            map.setCamera(
                    center(Point.fromLngLat(longitude, latitude)),
                    zoom(15),
                    animationType(AnimationType.FLY),
                    animationDuration(2000)
            );
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " AddSymbolLayerToMap Exception: " + ex.getMessage());
        }
    }

    private void SetToolTips() {
        try {
            // Set Tooltip for LINK Name
            iBtn_LinkName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_LinkName.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================
            // Set Tooltip for New Name
            iBtn_NewName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_NewName.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================
            // Set Tooltip for Pulse Ratio
            iBtn_AddNewTank.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_AddNewTank.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================
            // Set Tooltip for Pump On Time
            iBtn_PumpOnTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_PumpOnTime.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================
            // Set Tooltip for Pump Off Time
            iBtn_PumpOffTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_PumpOffTime.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================
            // Set Tooltip for Pulse Ratio
            iBtn_PulseRatio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_PulseRatio.getContentDescription().toString(), Gravity.BOTTOM);
                }
            });
            //===============================================================*/
            // Set Tooltip for Pulse Ratio
            iBtn_StreetAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateToolTip(v, iBtn_StreetAddress.getContentDescription().toString(), Gravity.TOP);
                }
            });
            //===============================================================
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " SetToolTips Exception: " + ex.getMessage());
        }
    }

    public void CreateToolTip(View view, String message, int gravity) {
        try {
            int padding = getResources().getDimensionPixelSize(R.dimen.tooltip_padding);
            int textSize = getResources().getDimensionPixelSize(R.dimen.tooltip_text_size);
            int radius = getResources().getDimensionPixelSize(R.dimen.tooltip_radius);

            ToolTip toolTip = new ToolTip.Builder()
                    .withText(message)
                    .withTextColor(Color.WHITE)
                    .withTextSize(textSize)
                    .withBackgroundColor(getResources().getColor(R.color.colorPrimary))
                    .withPadding(padding, padding, padding, padding)
                    .withCornerRadius(radius)
                    .build();
            ToolTipView toolTipView = new ToolTipView.Builder(AddNewLinkToCloud.this)
                    .withAnchor(view)
                    .withToolTip(toolTip)
                    .withGravity(gravity)
                    .build();
            toolTipView.show();
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " CreateToolTip Exception: " + ex.getMessage());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

//        ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
//        ((TextView) parent.getChildAt(0)).setTextSize(25);
        if (parent.getId() == R.id.spin_tanknumber) {
            addnewlinkViewModel.TankPositionSel = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private boolean validateData() {

        if (edt_linkname.getText().toString().trim().isEmpty()) {
            //edt_linkname.setError(getResources().getString(R.string.LinkNameRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.LinkNameRequired));
            return false;
        } else if (!edt_linkname.getText().toString().trim().matches(expression)) {
            //edt_linkname.setError(getResources().getString(R.string.LinkNameInvalid));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.LinkNameInvalid));
            return false;
        } else if (edt_pumpOnTime.getText().toString().trim().isEmpty()) {
            //edt_pumpOnTime.setError(getResources().getString(R.string.PumpOnTimeRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.PumpOnTimeRequired));
            return false;
        } else if (edt_pumpOffTime.getText().toString().trim().isEmpty()) {
            //edt_pumpOffTime.setError(getResources().getString(R.string.PumpOffTimeRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.PumpOffTimeRequired));
            return false;
        /*} else if (edt_username.getText().toString().trim().isEmpty()) {
            edt_username.setError("Enter valid data");
            return false;
        } else if (edt_enter_password.getText().toString().trim().isEmpty()) {
            edt_enter_password.setError("Enter valid data");
            return false;*/
        /*} else if (edt_UnitsMeasured.getText().toString().trim().isEmpty()) {
            edt_UnitsMeasured.setError("Enter valid data");
            return false;
        } else if (edt_Pulses.getText().toString().trim().isEmpty()) {
            edt_Pulses.setError("Enter valid data");
            return false;*/
        } else if (!edt_LinkNewName.getText().toString().trim().isEmpty()) {
            if (!edt_LinkNewName.getText().toString().trim().matches(expression)) {
                //edt_LinkNewName.setError(getResources().getString(R.string.NewNameInvalid));
                showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.NewNameInvalid));
                return false;
            }
        } else if (!AppConstants.isLocationSelected) {
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.LocationRequired));
            return false;
        } else if (edt_StreetAddress.getText().toString().trim().isEmpty()) {
            //edt_StreetAddress.setError(getResources().getString(R.string.AddressRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.AddressRequired));
            return false;
        }
        return true;
    }

    private boolean validateTankData(EditText edt_TankNumber, EditText edt_TankName) {

        if (edt_TankNumber.getText().toString().trim().isEmpty()) {
            //edt_TankNumber.setError(getResources().getString(R.string.TankNumberRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.TankNumberRequired));
            return false;
        } else if (edt_TankName.getText().toString().trim().isEmpty()) {
            //edt_TankName.setError(getResources().getString(R.string.TankNameRequired));
            showCustomMessageDialog(AddNewLinkToCloud.this, getResources().getString(R.string.TankNameRequired));
            return false;
        }
        return true;
    }

    public void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        if (message.contains("success")) {
                            dialog.dismiss();
                            finish();
                        } else {
                            dialog.dismiss();
                        }
                    }
                }
        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
        textView.setTextSize(35);
    }

    public void ShowProgressDialog(){

        String s = getResources().getString(R.string.PleaseWait);
        SpannableString ss2 = new SpannableString(s);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
        pd = new ProgressDialog(AddNewLinkToCloud.this);
        pd.setMessage(ss2);
        pd.setCancelable(true);
        pd.show();

    }

    public void HideProgressDialog(){
        pd.dismiss();
    }

    public class TankAndProductDetailsIMEI {
        public String IMEIUDID;
    }

    public class GetTanksAndProducts extends AsyncTask<String, Void, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s = getResources().getString(R.string.PleaseWait);
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(AddNewLinkToCloud.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(String... params) {

            String resp = "";

            String userEmail = CommonUtils.getCustomerDetails(AddNewLinkToCloud.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(AddNewLinkToCloud.this) + ":" + userEmail + ":" + "GetTanksAndProducts" + AppConstants.LANG_PARAM);

            TankAndProductDetailsIMEI obj = new TankAndProductDetailsIMEI();
            obj.IMEIUDID = AppConstants.getIMEI(AddNewLinkToCloud.this);
            Gson gson = new Gson();
            String jsonData = gson.toJson(obj);

            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetTanksAndProducts InBackground Exception: " + e.getMessage());
                if (OfflineConstants.isOfflineAccess(AddNewLinkToCloud.this)) {
                    AppConstants.NETWORK_STRENGTH = false;
                }
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String Response) {

            pd.dismiss();

            try {
                JSONObject jsonObject = new JSONObject(Response);

                /*if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Response: " + jsonObject);*/
                String ResponseMessage = jsonObject.getString("ResponseMessage");
                if (ResponseMessage.equalsIgnoreCase("success")) {

                    String tanksForLinksObj = jsonObject.getString(AppConstants.RES_TANK_DATA);
                    CommonUtils.BindTankData(tanksForLinksObj, true);
                    BindTankList();

                    String productsForTanksObj = jsonObject.getString(AppConstants.RES_PRODUCT_DATA);
                    CommonUtils.BindProductData(productsForTanksObj);

                }

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetTanksAndProducts onPostExecute Exception: " + e.getMessage());
                if (OfflineConstants.isOfflineAccess(AddNewLinkToCloud.this)) {
                    AppConstants.NETWORK_STRENGTH = false;
                }
            }
        }
    }

    public class AddTankFromAPP extends AsyncTask<String, Void, String> {

        ProgressDialog pd;
        String tankNumber = "";

        @Override
        protected void onPreExecute() {

            String s = getResources().getString(R.string.PleaseWait);
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(AddNewLinkToCloud.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(String... params) {

            String resp = "";

            String userEmail = CommonUtils.getCustomerDetails(AddNewLinkToCloud.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(AddNewLinkToCloud.this) + ":" + userEmail + ":" + "AddTankFromAPP" + AppConstants.LANG_PARAM);

            AddTankFromAPP_entity obj = new AddTankFromAPP_entity();
            obj.TankNumber = params[0];
            obj.TankName = params[1];
            obj.FuelTypeId = params[2];

            Gson gson = new Gson();
            String jsonData = gson.toJson(obj);

            tankNumber = params[0];

            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " AddTankFromAPP InBackground Exception: " + e.getMessage());
                if (OfflineConstants.isOfflineAccess(AddNewLinkToCloud.this)) {
                    AppConstants.NETWORK_STRENGTH = false;
                }
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String Response) {

            try {
                JSONObject jsonObject = new JSONObject(Response);

                String ResponseMessage = jsonObject.getString("ResponseMessage");
                String ResponseText = jsonObject.getString("ResponseText");
                if (ResponseMessage.equalsIgnoreCase("success")) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Tank Added Successfully. (" + tankNumber + ")");
                    String tanksForLinksObj = jsonObject.getString(AppConstants.RES_TANK_DATA);
                    CommonUtils.BindTankData(tanksForLinksObj, false);
                    //showCustomMessageDialog(AddNewLinkToCloud.this, ResponseMessage, ResponseText, addTankDialog);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BindTankList();
                            pd.dismiss();
                            if (addTankDialog != null) {
                                addTankDialog.dismiss();
                            }
                        }
                    }, 1000);

                } else {
                    pd.dismiss();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " AddTankFromAPP Response: " + ResponseText);
                    showCustomMessageDialog(AddNewLinkToCloud.this, ResponseText);
                }

            } catch (Exception e) {
                pd.dismiss();
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " AddTankFromAPP onPostExecute Exception: " + e.getMessage());
                if (OfflineConstants.isOfflineAccess(AddNewLinkToCloud.this)) {
                    AppConstants.NETWORK_STRENGTH = false;
                }
            }
        }
    }

    public void showCustomMessageDialog(final Activity context, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge);
        dialogBus.show();

        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        edt_message.setText(Html.fromHtml(message));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();

                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);

            }
        });
    }

    private void GetAddressByLatLng(Double Latitude, Double Longitude) {

        ProgressDialog pdAddress;
        String s = getResources().getString(R.string.PleaseWait);
        SpannableString ss2 = new SpannableString(s);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
        pdAddress = new ProgressDialog(AddNewLinkToCloud.this);
        pdAddress.setMessage(ss2);
        pdAddress.setCancelable(false);
        pdAddress.show();

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.BASE_URL_AZURE_MAP)
                .build();

        AzureMapApi apiInterface;
        apiInterface = retrofit.create(AzureMapApi.class);

        Call<JsonObject> call = apiInterface.GetAddressByLatLng("1.0", SubscriptionKeyForAzureMap, Latitude.toString() + ',' + Longitude.toString(), AppConstants.LANG_PARAM.replace(":", ""));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                /*if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAddressByLatLng onResponse: " + response.body());*/
                if (response.body() != null) {
                    parseResponseAddressByLatLng(pdAddress, response.body().toString());
                } else {
                    pdAddress.dismiss();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAddressByLatLng onFailure: " + t.getMessage());
                pdAddress.dismiss();
            }
        });
    }

    private void parseResponseAddressByLatLng(ProgressDialog pdAddress, String response) {
        try {
            if (!response.isEmpty()) {
                JSONObject jsonObj = new JSONObject(response);
                String addresses = jsonObj.getString("addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    JSONArray jsonArray = new JSONArray(addresses);
                    if (jsonArray.length() > 0) {
                        JSONObject jObj = jsonArray.getJSONObject(0);
                        String address = jObj.getString("address");
                        if (address != null && !address.isEmpty()) {
                            JSONObject addressObj = new JSONObject(address);
                            String freeformAddress = addressObj.getString("freeformAddress");
                            if (freeformAddress != null && !freeformAddress.isEmpty()) {
                                if (pdAddress != null) {
                                    pdAddress.dismiss();
                                }
                                edt_StreetAddress.setText(freeformAddress);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (pdAddress != null) {
                pdAddress.dismiss();
            }
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " parseResponseAddressByLatLng Exception: " + ex.getMessage());
        }
    }

    private void retrieveAddressDataFromApi(String query) {

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.BASE_URL_AZURE_MAP)
                .build();

        AzureMapApi apiInterface;
        apiInterface = retrofit.create(AzureMapApi.class);

        Call<JsonObject> call = apiInterface.GetAddressSuggestions("true", SubscriptionKeyForAzureMap, "1.0", query,
                AppConstants.LANG_PARAM.replace(":", ""), AppConstants.Longitude, AppConstants.Latitude, "", "Auto");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                /*if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " retrieveAddressDataFromApi onResponse: " + response.body());*/
                if (response.body() != null) {
                    parseAddressesFromResponse(response.body().toString());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " retrieveAddressDataFromApi onFailure: " + t.getMessage());
            }
        });
    }

    private void parseAddressesFromResponse(String response) {
        try {
            if (!response.isEmpty()) {
                JSONObject jsonObj = new JSONObject(response);
                String addresses = jsonObj.getString("results");
                if (addresses != null && !addresses.isEmpty()) {
                    searchResults.clear();
                    listOfPositions.clear();
                    JSONArray jsonArray = new JSONArray(addresses);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jObj = jsonArray.getJSONObject(i);
                        String address = jObj.getString("address");
                        if (address != null && !address.isEmpty()) {
                            JSONObject addressObj = new JSONObject(address);
                            String freeformAddress = addressObj.getString("freeformAddress");
                            if (freeformAddress != null && !freeformAddress.isEmpty()) {
                                searchResults.add(freeformAddress);
                            }
                        }

                        String positions = jObj.getString("position");
                        if (positions != null && !positions.isEmpty()) {
                            JSONObject positionObj = new JSONObject(positions);
                            String lat = positionObj.getString("lat");
                            String lon = positionObj.getString("lon");
                            HashMap<String, String> mapPositions = new HashMap<>();
                            mapPositions.put("index", String.valueOf(i));
                            mapPositions.put("latitude", lat);
                            mapPositions.put("longitude", lon);
                            listOfPositions.add(mapPositions);
                        }
                    }
                    autoAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, searchResults);
                    //autoAdapter.notifyDataSetChanged();
                    tv_SearchAddress.setAdapter(autoAdapter);
                    if (selectedAddress.isEmpty()) {
                        tv_SearchAddress.showDropDown();
                    }
                }
            }
        }  catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " parseAddressesFromResponse Exception: " + ex.getMessage());
        }
    }
}