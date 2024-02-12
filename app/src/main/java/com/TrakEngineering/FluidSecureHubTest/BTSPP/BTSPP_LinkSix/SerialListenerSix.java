package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkSix;

public interface SerialListenerSix {
    void onSerialConnectSix();
    void onSerialConnectErrorSix(Exception e);
    void onSerialReadSix(byte[] data);
    void onSerialIoErrorSix(Exception e, Integer fromCode);
}
