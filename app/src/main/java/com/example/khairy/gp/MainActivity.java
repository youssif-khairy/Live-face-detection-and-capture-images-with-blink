package com.example.khairy.gp;

import android.app.Activity;
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
import android.media.MediaPlayer;
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
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;

import static com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Detector.Processor,CameraSource.PictureCallback {

    CameraSource cameraSource;
    FaceDetector faceDetector;
    SurfaceView surfaceView;
    SurfaceView transparentView;
    final int CameraID = 1001;
    boolean rightHead = false,leftHead = false;
    boolean closed_eyes = false,captured = true;
    long timeLeft ,timeRight;
    boolean Choose_One = false,Choose_Two = false;
    int number = -1,Random = -1;
    boolean cautation_sound_One = false,cautation_sound_Two = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        transparentView = (SurfaceView)findViewById(R.id.transparentview);

        faceDetector = new FaceDetector.Builder(getApplicationContext())
                        .setTrackingEnabled(true) //for tracking in live video
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS) //get all face landmarks
                        .setMode(FaceDetector.ACCURATE_MODE) //for orientation
                        .setClassificationType(ALL_CLASSIFICATIONS) // for smile and blink
                        .build();
        cameraSource = new CameraSource.Builder(this,faceDetector)
                .setRequestedPreviewSize(640,480) // size
                .setFacing(CameraSource.CAMERA_FACING_FRONT)//selfie
                .setRequestedFps(30)
                .build();

        surfaceView.getHolder().addCallback(this);//implement it's methods
        transparentView.getHolder().addCallback(this);//implement it's methods

        transparentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);//System chooses a format that supports translucency (many alpha bits)
        transparentView.setZOrderMediaOverlay(true); //it will be overly

        faceDetector.setProcessor(this); // implement it's methods

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try
        {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){//if camera permission doesn't exist request it
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.CAMERA},CameraID);//request permission
                return;
            }

            cameraSource.start(surfaceView.getHolder());//start camera
        } catch (IOException e) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop(); // stop camera source
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) { //after requesting the permission
        switch (requestCode) { //the request code 1001
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

        SparseArray detectedFaces = detections.getDetectedItems();//get all detected face (sparseArray it map integer to object and it uses binary tree in search)

        if (detectedFaces.size()!=0){

            //for (int i=0;i<detectedFaces.size();i++){
               final Face face = (Face)detectedFaces.valueAt(0); // get only first face that appear on the camera
                try {
                    synchronized (surfaceView.getHolder()){ //only one thread that executes at the time (stop main thread to draw)
                        DrawRectangle(face.getPosition().x,face.getPosition().y +50,face.getWidth(),face.getHeight());// Draw rectangle on face
                    }
                }catch (Exception e){
                }
            
            Callable<Object> ShakeHead = new Callable<Object>() {//define region for shaking head
                public Object call() throws Exception {

                    if (face.getEulerY() < -12.0) { //get the orientation of face
                        leftHead = true;
                        timeLeft = (System.currentTimeMillis() / 1000);
                    }
                    if (face.getEulerY() > 12.0) { //get the orientation of face
                        rightHead = true;
                        timeRight = (System.currentTimeMillis() / 1000);
                    }

                    if (leftHead && rightHead) { //check if shake his head
                        if (Math.abs(timeLeft - timeRight) <= 4) //check the time between the left and the right move
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "لقد حركت راسك بطريقة صحيحة", Toast.LENGTH_SHORT).show();
                                }
                            });
                        leftHead = false;
                        rightHead = false;
                        return true;
                    }
                    return false;
                }
            };

            Callable<Object> Smile = new Callable<Object>() {
                public Object call() throws Exception {

                    if (face.getIsSmilingProbability() >= 0.3) { //get the probability of smile
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "لقد ابتسمت بطريقة صحيحة", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return true;
                    }
                    return false;
                }
            };

            Callable<Object> Blink = new Callable<Object>() {
                public Object call() throws Exception {

                    if ((face.getIsLeftEyeOpenProbability() + face.getIsRightEyeOpenProbability()) / 2.0f < 0.3 && face.getIsLeftEyeOpenProbability() > 0 && face.getIsRightEyeOpenProbability() > 0) { //closing eye (0's to check if eye option is already working in detection)
                        closed_eyes = true;
                    }
                    if (closed_eyes) {
                        if ((face.getIsLeftEyeOpenProbability() + face.getIsRightEyeOpenProbability()) / 2.0f >= 0.6 && face.getIsLeftEyeOpenProbability() > 0 && face.getIsRightEyeOpenProbability() > 0) {//open eye after closing it
                            closed_eyes = false;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "لقد اغلقت عينك بطريقة صحيحة", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return true;
                        }
                    }
                    return false;
                }
            };

            Random rand = new Random();
            if (number == -1) { //if random choose is not used before (first time)
                number = rand.nextInt(3);//option1: blink and smile ,option2 : blink and shake head, option3 : smile and shake head
                number++;
            }
                if (face.getId() == 0) { // to check if face still the same infront of camera
                    rand = new Random();
                    if (number==1) // option1
                    {
                        if (Random == -1)
                        {Random = rand.nextInt(2); Random++;}//arrangement1:blink smile, arrangement2:smile blink
                        if (Random == 1) { //arrangement1
                            if (!Choose_One) {//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.blink);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                  Choose_One = (boolean)Blink.call(); //blink
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (Choose_One && !Choose_Two) {//check if first detection is done and the second is not
                                if (!cautation_sound_Two) { //check if sound doesn't played before
                                    playSound(R.raw.smile);//play the second detection track
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (boolean) Smile.call();//smile
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else { //arrangement2
                            if (!Choose_One){//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.smile);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                    Choose_One = (boolean) Smile.call();//smile
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (Choose_One && !Choose_Two) {//check if first detection is done and the second is not
                                if (!cautation_sound_Two) {//check if sound doesn't played before
                                    playSound(R.raw.blink);//play the second detection track
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (Boolean)Blink.call();//blink
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (number==2)//option2
                    {
                        if (Random == -1)
                        {Random = rand.nextInt(2); Random++;}//arrangement1:blink shake head, arrangement2:shake head blink
                        if (Random==1) {//arrangement1

                            if (!Choose_One) {//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.blink);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                    Choose_One = (boolean)Blink.call();//blink
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (Choose_One && !Choose_Two) {//check if first detection is done and the second is not
                                if (!cautation_sound_Two) {//check if sound doesn't played before
                                    playSound(R.raw.shakehead);//play the second detection track
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (Boolean) ShakeHead.call();//shake head
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {//arrangement2

                            if (!Choose_One){//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.shakehead);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                    Choose_One = (boolean) ShakeHead.call();//shake head
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (Choose_One && !Choose_Two){//check if first detection is done and the second is not
                                if (!cautation_sound_Two) {//check if sound doesn't played before
                                    playSound(R.raw.blink);//play the desired detection sound
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (boolean)Blink.call();//blink
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (number==3)//option3
                    {
                        if (Random == -1)
                        {Random = rand.nextInt(2); Random++;}//arrangement1:smile shake head, arrangement2:shake head smile
                        if (Random == 1) {//arrangement1

                            if (!Choose_One){//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.smile);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                    Choose_One = (boolean) Smile.call();//smile
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (Choose_One && !Choose_Two){//check if first detection is done and the second is not
                                if (!cautation_sound_Two) {//check if sound doesn't played before
                                    playSound(R.raw.shakehead);//play the desired detection sound
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (boolean)ShakeHead.call();//shake head
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else{//arrangement2

                            if (!Choose_One){//check if first detection is taken
                                if (!cautation_sound_One) {//check if sound doesn't played before
                                    playSound(R.raw.shakehead);//play the desired detection sound
                                    cautation_sound_One = true;
                                }
                                try {
                                    Choose_One = (boolean)ShakeHead.call();//shake head
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (Choose_One && !Choose_Two){//check if first detection is done and the second is not
                                if (!cautation_sound_Two) {//check if sound doesn't played before
                                    playSound(R.raw.blink);//play the desired detection sound
                                    cautation_sound_Two = true;
                                }
                                try {
                                    Choose_Two = (boolean) Smile.call();//smile
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (captured && Choose_One && Choose_Two) {//check if no pictures is taken before and he passed both detections successfully
                        cameraSource.takePicture(null, MainActivity.this);//take picture
                        captured = false;
                    }
                }

        }

    }


    public void DrawRectangle(float x,float y,float width,float height) {

        Canvas canvas = transparentView.getHolder().lockCanvas(null); //creates a surface area that you will write to

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);//clear any past drawn shapes

        Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.GREEN);

        paint.setStrokeWidth(5);

        Rect rec=new Rect((int) x,(int)y,(int) x+(int)width,(int) y+(int)height); //draw rectangle

        canvas.drawRect(rec,paint);

        transparentView.getHolder().unlockCanvasAndPost(canvas);//apply the drawing and unlock canvas so that no other thread write to it
    }

    public void playSound(int audio) {
        MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this,audio); //play the audio
        mediaPlayer.start();
    }

    @Override
    public void onPictureTaken(byte[] data) {

        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length); //get the taken picture
    }


}
