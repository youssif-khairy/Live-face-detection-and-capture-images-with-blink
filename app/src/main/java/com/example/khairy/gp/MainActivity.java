package com.example.khairy.gp;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
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
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Detector.Processor,CameraSource.PictureCallback {

    CameraSource cameraSource;
    FaceDetector faceDetector;
    SurfaceView surfaceView;
    SurfaceView transparentView;
    final int CameraID = 1001;
    int  deviceHeight,deviceWidth;

    boolean closed_eyes = false,captured = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        transparentView = (SurfaceView)findViewById(R.id.transparentview);

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

        surfaceView.getHolder().addCallback(this);
        transparentView.getHolder().addCallback(this);

        transparentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        transparentView.setZOrderMediaOverlay(true);

        deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        deviceHeight = Resources.getSystem().getDisplayMetrics().heightPixels;


        faceDetector.setProcessor(this);



    }
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

                try {
                    synchronized (surfaceView.getHolder()){
                        DrawRectangle(face.getPosition().x,face.getPosition().y,face.getWidth(),face.getHeight());
                    }
                }catch (Exception e){

                }
//                for (Landmark landmark : face.getLandmarks()) {
//                    int x = (int) (landmark.getPosition().x);
//                    int y = (int) (landmark.getPosition().y);
//                    float radius = 10.0f;
//                    DrawCircle(x, y, radius);
//                }

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


    public void DrawRectangle(float x,float y,float width,float height) {

        Canvas canvas = transparentView.getHolder().lockCanvas(null);

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.GREEN);

        paint.setStrokeWidth(3);

        Rect rec=new Rect((int) x,(int)y,(int) x+(int)width,(int) y+(int)height+120);

        canvas.drawRect(rec,paint);

        transparentView.getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public void onPictureTaken(byte[] data) {

        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length);

    }
}
