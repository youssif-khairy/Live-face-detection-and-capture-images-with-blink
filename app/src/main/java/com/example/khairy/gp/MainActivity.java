package com.example.khairy.gp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.vision.CameraSource;

public class MainActivity extends AppCompatActivity implements CameraSource.PictureCallback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Face_Detection face_detection = new Face_Detection(MainActivity.this);
        face_detection.start();//start Face detection and facial expression recognition
    }

    @Override
    public void onPictureTaken(byte[] data) {
        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length); //get the taken picture
    }
}
