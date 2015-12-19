package org.firespeed.both;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;

/**
 * Created by kenz on 2015/11/19.
 */
public class Config implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String PATH = "/config";
    private static final String KEY_SMOOTH_MOVE = "SMOOTH_MOVE";

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsSmooth;
    public boolean isSmooth() {
        return mIsSmooth;
    }
    public void setIsSmooth(boolean isSmooth) {
        mIsSmooth = isSmooth;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putBoolean(KEY_SMOOTH_MOVE, mIsSmooth);
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());
   }

    private final WeakReference<OnConfigChangedListener> mConfigChangedListenerWeakReference;

    public Config(Context context, OnConfigChangedListener reference) {
        if (reference == null) {
            mConfigChangedListenerWeakReference = null;
        } else {
            mConfigChangedListenerWeakReference = new WeakReference<>(reference);
        }
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    public void connect() {
        mGoogleApiClient.connect();
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    public void disconnect() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.getDataItems(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DataItemBuffer>() {
                    @Override
                    public void onResult(DataItemBuffer dataItems) {
                        for (DataItem dataItem : dataItems) {
                            if (dataItem.getUri().getPath().equals(PATH)) {
                                DataMap dataMap = DataMap.fromByteArray(dataItem.getData());
                                mIsSmooth = dataMap.getBoolean(KEY_SMOOTH_MOVE, true);
                                if (mConfigChangedListenerWeakReference != null) {
                                    OnConfigChangedListener listener = mConfigChangedListenerWeakReference.get();
                                    if (listener != null) {
                                        listener.onConfigChanged(Config.this);
                                    }
                                }
                            }
                        }
                        dataItems.release();
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    mIsSmooth = dataMap.getBoolean(KEY_SMOOTH_MOVE);
                    if (mConfigChangedListenerWeakReference != null) {
                        OnConfigChangedListener listener = mConfigChangedListenerWeakReference.get();
                        if (listener != null) {
                            listener.onConfigChanged(Config.this);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public interface OnConfigChangedListener {
        void onConfigChanged(Config config);
    }
}
