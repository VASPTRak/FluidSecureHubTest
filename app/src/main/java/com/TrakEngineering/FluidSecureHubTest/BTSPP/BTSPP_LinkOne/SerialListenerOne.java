package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne;

public interface SerialListenerOne {
    void onSerialConnectOne();
    void onSerialConnectErrorOne(Exception e);
    void onSerialReadOne(byte[] data);
    void onSerialIoErrorOne(Exception e);
}
