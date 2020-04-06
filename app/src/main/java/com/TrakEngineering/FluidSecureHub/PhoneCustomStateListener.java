package com.TrakEngineering.FluidSecureHub;


import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

public class PhoneCustomStateListener extends PhoneStateListener {

    public int signalSupport = 0;

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);

        signalSupport = signalStrength.getGsmSignalStrength();
        //Log.d(getClass().getCanonicalName(), "------ gsm signal --> " + signalSupport);

        if (signalSupport > 30) {
            Constants.CurrentSignalStrength = "Signal GSM : Good";
            Constants.IsSignalSrtengthOk = true;

        } else if (signalSupport > 20 && signalSupport < 30) {
            Constants.CurrentSignalStrength = "Signal GSM : Avarage";
            Constants.IsSignalSrtengthOk = true;

        } else if (signalSupport < 20 && signalSupport > 3) {
            Constants.CurrentSignalStrength = "Signal GSM : Weak";
            Constants.IsSignalSrtengthOk = false;

        } else if (signalSupport < 3) {
            Constants.CurrentSignalStrength = "Signal GSM : Very weak";
            Constants.IsSignalSrtengthOk = false;

        }
    }
}
