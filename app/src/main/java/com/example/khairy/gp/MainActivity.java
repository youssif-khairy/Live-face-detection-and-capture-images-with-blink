package com.example.khairy.gp;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.security.Permission;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity implements Detector.Processor,CameraSource.PictureCallback {

    CameraSource cameraSource;
    FaceDetector faceDetector;
    SurfaceView surfaceView;
    final int CameraID = 1001;

    boolean closed_eyes = false,captured = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        faceDetector = new FaceDetector.Builder(getApplicationContext())
                        .setTrackingEnabled(true)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setMode(FaceDetector.FAST_MODE)
                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                        .build();
        cameraSource = new CameraSource.Builder(this,faceDetector)
                .setRequestedPreviewSize(640,480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try
                {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.CAMERA},CameraID);
                        return;
                    }

                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {

                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });


        faceDetector.setProcessor(this);
//        final Bitmap myBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.image);
//        iv.setImageBitmap(myBitmap);
//
//
//        final Paint rectPaint = new Paint();
//
//        rectPaint.setStrokeWidth(5);
//        rectPaint.setColor(Color.WHITE);
//        rectPaint.setStyle(Paint.Style.STROKE);
//
//        final Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(),myBitmap.getHeight(), Bitmap.Config.RGB_565);
//        final Canvas canvas  = new Canvas(tempBitmap);
//        canvas.drawBitmap(myBitmap,0,0,null);
//
//        b.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
//                        .setTrackingEnabled(false)
//                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
//                        .setMode(FaceDetector.FAST_MODE)
//                        .build();
//
//
//
//                if(!faceDetector.isOperational())
//                {
//                    Toast.makeText(MainActivity.this, "Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
//                SparseArray<Face> sparseArray = faceDetector.detect(frame);
//
//                for(int i=0;i<sparseArray.size();i++)
//                {
//                    Face face = sparseArray.valueAt(i);
//                    float x1=face.getPosition().x;
//                    float y1 =face.getPosition().y;
//                    float x2 = x1+face.getWidth();
//                    float y2=y1+face.getHeight();
//                    RectF rectF = new RectF(x1,y1,x2,y2);
//                    canvas.drawRoundRect(rectF,2,2,rectPaint);
//
//                }
//
//                iv.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
//            }
//        });


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CameraID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),android.Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED){
                        return;
                    }
                    try
                    {
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {

                    }

                }
            }

            // other 'switch' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections detections) {

        SparseArray detectedFaces = detections.getDetectedItems();


        if (detectedFaces.size()!=0){
            for (int i=0;i<detectedFaces.size();i++){
               final Face face = (Face)detectedFaces.valueAt(i);


                if ((face.getIsLeftEyeOpenProbability() + face.getIsRightEyeOpenProbability())/2.0f< 0.3   && face.getIsLeftEyeOpenProbability() > 0 && face.getIsRightEyeOpenProbability() > 0)
                {
                    closed_eyes = true;

                }
                if (closed_eyes)
                {
                    if ((face.getIsLeftEyeOpenProbability() + face.getIsRightEyeOpenProbability()) / 2.0f >= 0.6 && face.getIsLeftEyeOpenProbability() > 0 && face.getIsRightEyeOpenProbability() > 0)
                    {
                        if (captured)
                        {
                            cameraSource.takePicture(null, this);
                            captured = false;
                            closed_eyes = false;
                        }
                    }
                }
            }


        }

    }


    public void Draw(float x,float y,float Width ,float height) {
        Canvas canvas = surfaceView.getHolder().lockCanvas(null);

        Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.GREEN);

        paint.setStrokeWidth(3);

        Rect rec=new Rect((int) x,(int)y,(int)x+(int)Width,(int)y+(int)height);

        canvas.drawRect(rec,paint);

        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public void onPictureTaken(byte[] data) {

        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length);

    }
}
