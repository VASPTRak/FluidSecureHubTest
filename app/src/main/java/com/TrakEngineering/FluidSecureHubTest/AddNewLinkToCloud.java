package com.TrakEngineering.FluidSecureHubTest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;


public class AddNewLinkToCloud extends AppCompatActivity implements LifecycleObserver, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private AddnewLink_ViewModel addnewlinkViewModel;
    private String TAG = this.getClass().getSimpleName();
    private Spinner Spinner_tankNumber;
    private EditText edt_linkname, edt_pumpOnTime, edt_pumpOffTime, edt_username, edt_enter_password;
    private EditText edt_LinkNewName, edt_UnitsMeasured, edt_Pulses;
    private Button btn_cancel, btn_done;
    private String expression = "^[a-zA-Z0-9-_ ]*$";
    private ProgressDialog pd;
    private CheckBox chkShowHidePassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_link_to_cloud);

        btn_done = (Button) findViewById(R.id.btn_done);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        edt_linkname = (EditText) findViewById(R.id.edt_linkname);
        edt_pumpOnTime = (EditText) findViewById(R.id.edt_pumpOnTime);
        edt_pumpOffTime = (EditText) findViewById(R.id.edt_pumpOffTime);
        edt_username = (EditText) findViewById(R.id.edt_username);
        edt_enter_password = (EditText) findViewById(R.id.edt_enter_password);
        Spinner_tankNumber = (Spinner) findViewById(R.id.spin_tanknumber);
        Spinner_tankNumber.setOnItemSelectedListener(this);
        chkShowHidePassword = (CheckBox) findViewById(R.id.chkShowHidePassword);
        edt_LinkNewName = (EditText) findViewById(R.id.edt_LinkNewName);
        edt_UnitsMeasured = (EditText) findViewById(R.id.edt_UnitsMeasured);
        edt_Pulses = (EditText) findViewById(R.id.edt_Pulses);

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

        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, addnewlinkViewModel.getSpinnerList());
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner_tankNumber.setAdapter(aa);

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateData()) {
                    addnewlinkViewModel.ProcessData(edt_linkname.getText().toString().trim(), edt_pumpOnTime.getText().toString().trim(),
                            edt_pumpOffTime.getText().toString().trim(), edt_username.getText().toString().trim(), edt_enter_password.getText().toString().trim(),
                            edt_LinkNewName.getText().toString().trim(), edt_UnitsMeasured.getText().toString().trim(), edt_Pulses.getText().toString().trim());
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        chkShowHidePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
        });

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

//        ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
//        ((TextView) parent.getChildAt(0)).setTextSize(25);
        addnewlinkViewModel.TankPositionSel = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private boolean validateData() {

        if (edt_linkname.getText().toString().trim().isEmpty()) {
            edt_linkname.setError("Enter valid data");
            return false;
        } else if (!edt_linkname.getText().toString().trim().matches(expression)) {
            edt_linkname.setError("Enter valid data");
            return false;
        } else if (edt_pumpOnTime.getText().toString().trim().isEmpty()) {
            edt_pumpOnTime.setError("Enter valid data");
            return false;
        } else if (edt_pumpOffTime.getText().toString().trim().isEmpty()) {
            edt_pumpOffTime.setError("Enter valid data");
            return false;
        } else if (edt_username.getText().toString().trim().isEmpty()) {
            edt_username.setError("Enter valid data");
            return false;
        } else if (edt_enter_password.getText().toString().trim().isEmpty()) {
            edt_enter_password.setError("Enter valid data");
            return false;
        } else if (edt_UnitsMeasured.getText().toString().trim().isEmpty()) {
            edt_UnitsMeasured.setError("Enter valid data");
            return false;
        } else if (edt_Pulses.getText().toString().trim().isEmpty()) {
            edt_Pulses.setError("Enter valid data");
            return false;
        } else if (!edt_LinkNewName.getText().toString().trim().isEmpty()) {
            if (!edt_LinkNewName.getText().toString().trim().matches(expression)) {
                edt_LinkNewName.setError("Enter valid data");
                return false;
            }
        }
        return true;
    }

    public void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        if (message.contains("success")){
                            dialog.dismiss();
                            finish();
                        }else{
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

        String s = "Please wait...";
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
}