package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_Oscilloscope;

public interface SerialListenerOscilloscope {
    void onSerialConnectOscilloscope();
    void onSerialConnectErrorOscilloscope(Exception e);
    void onSerialReadOscilloscope(byte[] data);
    void onSerialIoErrorOscilloscope(Exception e);
}
