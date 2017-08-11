package com.hift.nameofthings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.view.SurfaceHolder;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cz.jirutka.unidecode.Unidecode;

/**
 * Created by TeguhAS on 05-Aug-17.
 */
public class UploadTask extends AsyncTask<UploadBean, Void, Void> {
    UploadBean uploadBean;
    String bestLabel;
    String translatedLabel;
    String tokenizedLabel;
    String translit;

    @Override
    protected Void doInBackground(UploadBean... params) {
        this.uploadBean = params[0];
        String apiKey = uploadBean.getApiKey();

        VisionRequestInitializer initializer = new VisionRequestInitializer(apiKey);
        Vision.Builder visionBuilder = new Vision.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                .setVisionRequestInitializer(initializer);

        Vision vision = visionBuilder.build();

        Image image = new Image();
        image.encodeContent(uploadBean.getPictureBytes());
        System.out.println("image size: " + image.size());

        Feature desiredFeature = new Feature();
        desiredFeature.setType("LABEL_DETECTION");
        desiredFeature.setMaxResults(5);

        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(image);
        annotateImageRequest.setFeatures(Arrays.asList(desiredFeature));

        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(Arrays.asList(annotateImageRequest));

        try {
            BatchAnnotateImagesResponse batchAnnotateImagesResponse = vision.images().annotate(batchAnnotateImagesRequest).execute();
            List<EntityAnnotation> list = batchAnnotateImagesResponse.getResponses().get(0).getLabelAnnotations();
            int first = -1;
            Collections.sort(list, new Comparator<EntityAnnotation>() {
                @Override
                public int compare(EntityAnnotation o1, EntityAnnotation o2) {
                    return Float.compare(o2.getScore(), o1.getScore());
                }
            });
            for (EntityAnnotation entityAnnotation : list) {
                if (first == -1) {
                    bestLabel = entityAnnotation.getDescription();
                    first = 0;
                }
                System.out.println("description: " + entityAnnotation.getDescription());
                System.out.println("score: " + entityAnnotation.getScore());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Translate t = new Translate.Builder(
                    AndroidHttp.newCompatibleTransport()
                    , new AndroidJsonFactory(), null)
                    .build();
            Translate.Translations.List list = t.new Translations().list(
                    Arrays.asList(bestLabel), "JA")
                    .setSource("EN").setModel("nmt");
            list.setKey(apiKey);
            TranslationsListResponse response = list.execute();
            for (TranslationsResource tr : response.getTranslations()) {
                System.out.println("translated: " + tr.getTranslatedText());
                translatedLabel = tr.getTranslatedText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        WordSpeaker first = new WordSpeaker(uploadBean.getContext(), bestLabel, Locale.UK);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WordSpeaker second = new WordSpeaker(uploadBean.getContext(), translatedLabel, Locale.JAPAN);

        Tokenizer tokenizer = Tokenizer.builder().build();
        tokenizedLabel = "";
        for (Token token : tokenizer.tokenize(translatedLabel)) {
            tokenizedLabel += token.getReading();
        }
        System.out.println("tokenized: " + tokenizedLabel);

        Unidecode unidecode = Unidecode.toLatin2();
        translit = unidecode.decode(tokenizedLabel);
        if (translit == null || translit.isEmpty() || translit.equalsIgnoreCase("null")) {
            translit = unidecode.decode(translatedLabel);
        }

        bestLabel = bestLabel.substring(0, 1).toUpperCase() + bestLabel.substring(1).toLowerCase();
        translit = "(" + translit + ")";

        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        SurfaceHolder holder = uploadBean.getHolder();

        Canvas canvas = holder.lockCanvas(null);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.MONOSPACE);

        float desiredWidth = 900;
        int width = canvas.getWidth();

        setTextSizeForWidth(paint, desiredWidth, bestLabel);
        canvas.drawText(bestLabel, (float) (width / 2) - paint.measureText(bestLabel) / 2, (float) 400, paint);

        setTextSizeForWidth(paint, desiredWidth, translatedLabel);
        canvas.drawText(translatedLabel, (float) (width / 2) - paint.measureText(translatedLabel) / 2, (float) 1400, paint);

        setTextSizeForWidth(paint, (float) (desiredWidth / 1.5), translit);
        paint.setColor(Color.parseColor("#FFAB00"));
        paint.setUnderlineText(true);
        canvas.drawText(translit, (float) (width / 2) - paint.measureText(translit) / 2, (float) 1500, paint);

        holder.unlockCanvasAndPost(canvas);

        uploadBean.getDialog().dismiss();
    }

    private void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        float maxTextSize = desiredWidth > 600? 200 : 120;
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();
        paint.setTextSize(desiredTextSize > maxTextSize ? maxTextSize : desiredTextSize);
    }
}
