package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFive;

public interface SerialListenerFive {
    void onSerialConnectFive();
    void onSerialConnectErrorFive(Exception e);
    void onSerialReadFive(byte[] data);
    void onSerialIoErrorFive(Exception e);
}
