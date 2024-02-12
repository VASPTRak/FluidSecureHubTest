package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour;

public interface SerialListenerFour {
    void onSerialConnectFour();
    void onSerialConnectErrorFour(Exception e);
    void onSerialReadFour(byte[] data);
    void onSerialIoErrorFour(Exception e, Integer fromCode);
}
