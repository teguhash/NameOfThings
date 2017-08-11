package com.hift.nameofthings;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.SurfaceHolder;

import com.google.api.services.vision.v1.model.Word;

/**
 * Created by TeguhAS on 05-Aug-17.
 */

public class UploadBean {
    ProgressDialog dialog;
    SurfaceHolder holder;
    byte[] pictureBytes;
    Context context;
    String apiKey;

    public ProgressDialog getDialog() {
        return dialog;
    }

    public void setDialog(ProgressDialog dialog) {
        this.dialog = dialog;
    }

    public SurfaceHolder getHolder() {
        return holder;
    }

    public void setHolder(SurfaceHolder holder) {
        this.holder = holder;
    }

    public byte[] getPictureBytes() {
        return pictureBytes;
    }

    public void setPictureBytes(byte[] pictureBytes) {
        this.pictureBytes = pictureBytes;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}
