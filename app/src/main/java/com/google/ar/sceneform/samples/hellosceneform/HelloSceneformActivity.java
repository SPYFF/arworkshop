/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.QuaternionEvaluator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Random;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private AnchorNode startNode;
    private ModelRenderable modelRenderable;
    private ModelRenderable oilRenderable;
    private Node model;
    private AnchorNode oilNode;
    private Button btnLeft;
    private Button btnRight;
    private Button btnForward;

    private ObjectAnimator moveAnimation;
    private ObjectAnimator turnAnimation;

    private ProgressBar progressBar;
    private int fuel;

    private void moveForward() {
        moveAnimation = new ObjectAnimator();
        moveAnimation.setAutoCancel(true);
        moveAnimation.setTarget(model);

        Vector3 tankPosition = model.getLocalPosition();
        tankPosition.z += 1;
        Vector3 targetPositon = model.localToWorldPoint(tankPosition);

        moveAnimation.setObjectValues(model.getWorldPosition(), targetPositon);
        moveAnimation.setPropertyName("worldPosition");
        moveAnimation.setEvaluator(new Vector3Evaluator());
        moveAnimation.setInterpolator(new LinearInterpolator());
        moveAnimation.setDuration(7000);
        moveAnimation.start();
    }

    private void rotate(int degree) {
        turnAnimation = new ObjectAnimator();
        turnAnimation.setAutoCancel(true);
        turnAnimation.setTarget(model);

        Vector3 rotateAxis = new Vector3(0f, 1f, 0f);
        Quaternion rotateWith = Quaternion.axisAngle(rotateAxis, degree);
        Quaternion newOrientation = Quaternion.multiply(model.getWorldRotation(), rotateWith);

        turnAnimation.setObjectValues(model.getWorldRotation(), newOrientation);
        turnAnimation.setPropertyName("worldRotation");
        turnAnimation.setEvaluator(new QuaternionEvaluator());
        turnAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        turnAnimation.setDuration(2000);
        turnAnimation.start();
    }

    private void placeOil(Vector3 tankPosition) {
        oilNode = new AnchorNode();
        float distance = 0.4f;

        Random random = new Random();
        float randomX = (-1) * distance + random.nextFloat() * (distance - (-1) * distance);
        float randomZ = (-1) * distance + random.nextFloat() * (distance - (-1) * distance);
        tankPosition.x += randomX;
        tankPosition.z += randomZ;

        oilNode.setWorldPosition(tankPosition);
        oilNode.setParent(arFragment.getArSceneView().getScene());
        oilNode.setRenderable(oilRenderable);
    }

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        ModelRenderable.builder()
                .setSource(this, R.raw.tank)
                .build()
                .thenAccept(renderable -> modelRenderable = renderable);

        ModelRenderable.builder()
                .setSource(this, R.raw.oildrum)
                .build()
                .thenAccept(renderable -> oilRenderable = renderable);

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if(modelRenderable == null) {
                        return;
                    }

                    Anchor anchor = hitResult.createAnchor();
                    if(startNode == null) {
                        startNode = new AnchorNode(anchor);
                        startNode.setParent(arFragment.getArSceneView().getScene());

                        model = new Node();
                        model.setParent(startNode);
                        model.setRenderable(modelRenderable);

                        placeOil(model.getWorldPosition());
                        fuel = 100;
                    }
                }
        );

        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if(model == null) {
                    return;
                }

                Node barrel = arFragment.getArSceneView().getScene().overlapTest(model);
                if(barrel != null) {
                    barrel.setRenderable(null);
                    placeOil(model.getWorldPosition());
                    fuel = 100;
                }
            }
        });

        //A thread witch updates the progress bar
        progressBar = findViewById(R.id.progressBar);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    progressBar.setProgress(fuel);
                    fuel--;
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        //Create references for the buttons on the UI
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnForward = findViewById(R.id.btnForward);

        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rotate(180);
                        Log.d(TAG, "BtnLeft");
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        turnAnimation.cancel();
                        Log.d(TAG, "BtnStopLeft");
                }
                return false;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rotate(-180);
                        Log.d(TAG, "BtnRight");
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        turnAnimation.cancel();
                        Log.d(TAG, "BtnStopRight");
                }
                return false;
            }
        });

        btnForward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        moveForward();
                        Log.d(TAG, "BtnForward");
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        moveAnimation.cancel();
                        Log.d(TAG, "BtnStopForward");
                }
                return false;
            }
        });
    }

    // Check if the device is AR compatible
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
