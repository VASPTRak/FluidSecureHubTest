package com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_Oscilloscope;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.TrakEngineering.FluidSecureHubTest.BuildConfig;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class SerialServiceOscilloscope extends Service implements SerialListenerOscilloscope {

    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    public class SerialBinder extends Binder {
        public SerialServiceOscilloscope getService() {
            return SerialServiceOscilloscope.this;
        }
    }

    private enum QueueType {Connect, ConnectError, Read, IoError}

    private class QueueItem {
        SerialServiceOscilloscope.QueueType type;
        byte[] data;
        Exception e;

        QueueItem(SerialServiceOscilloscope.QueueType type, byte[] data, Exception e) {
            this.type = type;
            this.data = data;
            this.e = e;
        }
    }

    private final Handler mainLooper;
    private final IBinder binder;
    private final Queue<SerialServiceOscilloscope.QueueItem> queue1, queue2;

    private SerialSocketOscilloscope socket;
    private SerialListenerOscilloscope listener;
    private boolean connected;

    /**
     * Lifecylce
     */
    public SerialServiceOscilloscope() {
        mainLooper = new Handler(Looper.getMainLooper());
        binder = new SerialServiceOscilloscope.SerialBinder();
        queue1 = new LinkedList<>();
        queue2 = new LinkedList<>();
    }

    @Override
    public void onDestroy() {
        cancelNotification();
        disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Api
     */
    public void connect(SerialSocketOscilloscope socket) throws IOException {
        socket.connect(this);
        this.socket = socket;
        connected = true;
    }

    public void disconnect() {
        connected = false;
        cancelNotification();
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }

    public void write(byte[] data) throws IOException {
        socket.write(data);
    }

    public void readPulse() throws IOException {
        socket.readPulse();
    }

    public void attach(SerialListenerOscilloscope listener) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread())
            throw new IllegalArgumentException("not in main thread");
        cancelNotification();
        // use synchronized() to prevent new items in queue2
        // new items will not be added to queue1 because mainLooper.post and attach() run in main thread
        synchronized (this) {
            this.listener = listener;
        }
        for (SerialServiceOscilloscope.QueueItem item : queue1) {
            switch (item.type) {
                case Connect:
                    listener.onSerialConnectOscilloscope();
                    break;
                case ConnectError:
                    listener.onSerialConnectErrorOscilloscope(item.e);
                    break;
                case Read:
                    listener.onSerialReadOscilloscope(item.data);
                    break;
                case IoError:
                    listener.onSerialIoErrorOscilloscope(item.e);
                    break;
            }
        }
        for (SerialServiceOscilloscope.QueueItem item : queue2) {
            switch (item.type) {
                case Connect:
                    listener.onSerialConnectOscilloscope();
                    break;
                case ConnectError:
                    listener.onSerialConnectErrorOscilloscope(item.e);
                    break;
                case Read:
                    listener.onSerialReadOscilloscope(item.data);
                    break;
                case IoError:
                    listener.onSerialIoErrorOscilloscope(item.e);
                    break;
            }
        }
        queue1.clear();
        queue2.clear();
    }

    public void detach() {
        if (connected)
            // items already in event queue (posted before detach() to mainLooper) will end up in queue1
            // items occurring later, will be moved directly to queue2
            // detach() and mainLooper.post run in the main thread, so all items are caught
            listener = null;
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    /**
     * SerialListener
     */
    public void onSerialConnectOscilloscope() {
        if (connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialConnectOscilloscope();
                        } else {
                            queue1.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.Connect, null, null));
                        }
                    });
                } else {
                    queue2.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.Connect, null, null));
                }
            }
        }
    }

    public void onSerialConnectErrorOscilloscope(Exception e) {
        if (connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialConnectErrorOscilloscope(e);
                        } else {
                            queue1.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.ConnectError, null, e));
                            cancelNotification();
                            disconnect();
                        }
                    });
                } else {
                    queue2.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.ConnectError, null, e));
                    cancelNotification();
                    disconnect();
                }
            }
        }
    }

    public void onSerialReadOscilloscope(byte[] data) {
        if (connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialReadOscilloscope(data);
                        } else {
                            queue1.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.Read, data, null));
                        }
                    });
                } else {
                    queue2.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.Read, data, null));
                }
            }
        }
    }

    public void onSerialIoErrorOscilloscope(Exception e) {
        if (connected) {
            synchronized (this) {
                if (listener != null) {
                    mainLooper.post(() -> {
                        if (listener != null) {
                            listener.onSerialIoErrorOscilloscope(e);
                        } else {
                            queue1.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.IoError, null, e));
                            cancelNotification();
                            disconnect();
                        }
                    });
                } else {
                    queue2.add(new SerialServiceOscilloscope.QueueItem(SerialServiceOscilloscope.QueueType.IoError, null, e));
                    cancelNotification();
                    disconnect();
                }
            }
        }
    }

}
