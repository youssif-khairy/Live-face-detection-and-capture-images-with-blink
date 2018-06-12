package com.example.khairy.gp;


import android.util.SparseArray;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.util.concurrent.Callable;

import static com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS;

import android.app.Activity;
import android.media.MediaPlayer;
import android.widget.Toast;

import java.util.Random;


/**
 * Created by khairy on 11/06/2018.
 */

public class Face_Detection implements Detector.Processor {

    FaceDetector faceDetector;
    boolean rightHead ,leftHead;
    boolean closed_eyes,captured;
    long timeLeft ,timeRight;
    boolean Choose_One,Choose_Two ;
    int number ,Random ;
    boolean cautation_sound_One,cautation_sound_Two;
    Activity activity;
    Camera_Source camera_source;

    public Face_Detection(Activity activity1) {
        activity = activity1;
        rightHead = false;
        leftHead = false;
        closed_eyes = false;
        captured = true;
        Choose_One = false;
        Choose_Two = false;
        number = -1;Random = -1;
        cautation_sound_One = false;
        cautation_sound_Two = false;
        faceDetector = new FaceDetector.Builder(activity1)
                .setTrackingEnabled(true) //for tracking in live video
                .setLandmarkType(FaceDetector.ALL_LANDMARKS) //get all face landmarks
                .setMode(FaceDetector.ACCURATE_MODE) //for orientation
                .setClassificationType(ALL_CLASSIFICATIONS) // for smile and blink
                .build();
        camera_source = new Camera_Source(activity,faceDetector);
    }

    public void start()
    {
        faceDetector.setProcessor(this); // implement it's methods
    }

    public void playSound(int audio) {
        MediaPlayer mediaPlayer = MediaPlayer.create(activity,audio); //play the audio
        mediaPlayer.start();
    }

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections detections)  {

        SparseArray detectedFaces = detections.getDetectedItems();//get all detected face (sparseArray it map integer to object and it uses binary tree in search)

        if (detectedFaces.size()!=0){

            //for (int i=0;i<detectedFaces.size();i++){
            final Face face = (Face)detectedFaces.valueAt(0); // get only first face that appear on the camera
            try {
                synchronized (camera_source.surfaceView.getHolder()){ //only one thread that executes at the time (stop main thread to draw)
                    camera_source.DrawRectangle(face.getPosition().x,face.getPosition().y +50,face.getWidth(),face.getHeight());// Draw rectangle on face
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
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, "لقد حركت راسك بطريقة صحيحة", Toast.LENGTH_SHORT).show();
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
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, "لقد ابتسمت بطريقة صحيحة", Toast.LENGTH_SHORT).show();
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
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, "لقد اغلقت عينك بطريقة صحيحة", Toast.LENGTH_SHORT).show();
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
                    camera_source.cameraSource.takePicture(null, (CameraSource.PictureCallback)activity);//take picture
                    captured = false;
                }
            }

        }

    }

}
