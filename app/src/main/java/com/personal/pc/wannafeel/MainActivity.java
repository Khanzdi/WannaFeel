package com.personal.pc.wannafeel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    private Button btn_hacerfoto, salir;
    private ImageView img;
    private ProgressBar proceso;
    private Button btnProcesar;
    private EditText mEditText;
    private Bitmap bMap;
    private EmotionServiceClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView) this.findViewById(R.id.imgMostrar);
        salir = (Button) this.findViewById(R.id.btn_salir);
        btnProcesar = (Button) this.findViewById(R.id.procesar);
        btnProcesar.setVisibility(View.INVISIBLE);
        btn_hacerfoto = (Button) this.findViewById(R.id.btn_camara);
        mEditText = (EditText) this.findViewById(R.id.mEditText);
        proceso = (ProgressBar) this.findViewById(R.id.progreso);
        mEditText.setEnabled(false);
        proceso.setVisibility(View.INVISIBLE);
        client = new EmotionServiceRestClient(getString(R.string.subscription_key), "https://westus.api.cognitive.microsoft.com/emotion/v1.0");

        btn_hacerfoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                File imagesFolder = new File(
                        Environment.getExternalStorageDirectory(), "Wannafeel");
                imagesFolder.mkdirs();
                File image = new File(imagesFolder, "foto.jpg");
                Uri uriSavedImage = Uri.fromFile(image);

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
                startActivityForResult(cameraIntent, 1);
            }
        });
        salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
             bMap = BitmapFactory.decodeFile(
                    Environment.getExternalStorageDirectory() +
                            "/Wannafeel/" + "foto.jpg");
            img.setImageBitmap(bMap);
            btnProcesar.setVisibility(View.VISIBLE);
        }
    }

    public void procesar(View v) {
        Toast.makeText(this, "Procesando...", Toast.LENGTH_LONG).show();
        btnProcesar.setVisibility(View.INVISIBLE);
        mEditText.getText().clear();
        proceso.setVisibility(View.VISIBLE);
        try {
            new doRequest(true).execute();
        } catch (Exception e) {
            mEditText.append("Error encountered. Exception is: " + e.toString());
            Toast.makeText(this, "Error...", Toast.LENGTH_LONG).show();
        }
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return processWithFaceRectangles();
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            proceso.setVisibility(View.INVISIBLE);
            // Display based on error existenc
            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
                return;
            }
            if (result.size() == 0) {
                mEditText.append("Lamentamos informar que en esa fotografía no se detectaron emociones :( ");
                return;
            }
            Integer count = 1;
            Bitmap bitmapCopy = bMap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas faceCanvas = new Canvas(bitmapCopy);
            faceCanvas.drawBitmap(bMap, 0, 0, null);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.RED);
            for (RecognizeResult r : result) {
                mEditText.append(String.format("\nRostro #%1$d \n", count));
                mEditText.append(String.format("\t Enfado: %1$.5f\n", r.scores.anger));
                mEditText.append(String.format("\t Desprecio: %1$.5f\n", r.scores.contempt));
                mEditText.append(String.format("\t Disgusto: %1$.5f\n", r.scores.disgust));
                mEditText.append(String.format("\t Miedo: %1$.5f\n", r.scores.fear));
                mEditText.append(String.format("\t Felicidad: %1$.5f\n", r.scores.happiness));
                mEditText.append(String.format("\t Neutral: %1$.5f\n", r.scores.neutral));
                mEditText.append(String.format("\t Tristeza: %1$.5f\n", r.scores.sadness));
                mEditText.append(String.format("\t Sorpresa: %1$.5f\n", r.scores.surprise));
                mEditText.append(String.format("\t Rectangulo del rostro: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                faceCanvas.drawRect(r.faceRectangle.left,
                        r.faceRectangle.top,
                        r.faceRectangle.left + r.faceRectangle.width,
                        r.faceRectangle.top + r.faceRectangle.height,
                        paint);
                count++;
            }
            img.setImageDrawable(new BitmapDrawable(getResources(), bitmapCopy));
            mEditText.setSelection(0);
        }
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bMap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bMap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0", faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.client.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }
}