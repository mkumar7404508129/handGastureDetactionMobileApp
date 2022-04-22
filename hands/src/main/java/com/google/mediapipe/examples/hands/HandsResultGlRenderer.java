// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//This is the main page where we can draw image and count the fingers
package com.google.mediapipe.examples.hands;


import android.content.Context;

import android.opengl.GLES20;

import android.util.Log;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutioncore.ResultGlRenderer;

import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.util.List;


/** A custom implementation of {@link ResultGlRenderer} to render {@link HandsResult}. */
public class HandsResultGlRenderer implements ResultGlRenderer<HandsResult>  {
  MyCallback callback;
  Context context;
  int callBackNum=0;
  private static final String TAG = "HandsResultGlRenderer";
  private static final float[] LEFT_HAND_CONNECTION_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
  private static final float[] RIGHT_HAND_CONNECTION_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
  private static final float CONNECTION_THICKNESS = 25.0f;
  private static final float[] LEFT_HAND_HOLLOW_CIRCLE_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
  private static final float[] RIGHT_HAND_HOLLOW_CIRCLE_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
  private static final float HOLLOW_CIRCLE_RADIUS = 0.01f;
  private static final float[] LEFT_HAND_LANDMARK_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
  private static final float[] RIGHT_HAND_LANDMARK_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
  private static final float LANDMARK_RADIUS = 0.008f;
  private static final int NUM_SEGMENTS = 120;
  //For Triangle draws

// this is the use for making relation with opengl pipelines
  private final String VERTEX_SHADER =
//Test
          "attribute vec2 a_TexCoordinate;" +
                  "varying vec2 v_TexCoordinate;" +
//End Test
                  "uniform mat4 uProjectionMatrix;" +
                  "attribute vec4 vPosition;" +
                  "void main() {" +
                  "  gl_Position = uProjectionMatrix * vPosition;"+
                  //Test
                  "v_TexCoordinate = a_TexCoordinate;" +
                  //End Test
                  "}";

  private final String FRAGMENT_SHADER =
          "precision mediump float;" +
                  "uniform vec4 uColor;" +
//Test
                  "uniform sampler2D u_Texture;" +
                  "varying vec2 v_TexCoordinate;" +
//End Test
                  "void main() {" +
//"gl_FragColor = vColor;" +
                  "gl_FragColor = uColor+(uColor * texture2D(u_Texture, v_TexCoordinate));" +
                  "}";


  //Different type off images

  DrawImage image0, image1,image2,image3,image4,image5;

  private int program;
  private int positionHandle;
  private int projectionMatrixHandle;
  private int colorHandle;

  public HandsResultGlRenderer(MyCallback callback,Context context) {
    this.callback=callback;
    this.context=context;
    //initializing images
    image0= new DrawImage(this.context,R.mipmap.zero_foreground);
    image1 = new DrawImage(this.context,R.mipmap.ic_launcher_foreground);
    image2 = new DrawImage(this.context,R.mipmap.tow_foreground);
    image3 = new DrawImage(this.context,R.mipmap.three_foreground);
    image4 = new DrawImage(this.context,R.mipmap.four_foreground);
    image5 = new DrawImage(this.context,R.mipmap.outofrange_foreground);


  }
  static public int loadShader(int type, String shaderCode) {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, shaderCode);
    GLES20.glCompileShader(shader);
    return shader;
  }

  @Override
  public void setupRendering() {
    program = GLES20.glCreateProgram();
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
    GLES20.glAttachShader(program, vertexShader);
    GLES20.glAttachShader(program, fragmentShader);
    GLES20.glLinkProgram(program);
    positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
    projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
    colorHandle = GLES20.glGetUniformLocation(program, "uColor");

  }

  @Override
  public void renderResult(HandsResult result, float[] projectionMatrix) {
    if (result == null) {
      return;
    }

    if(result.multiHandWorldLandmarks().isEmpty()){
      if (callBackNum==0){
        callback.updateMyText("0");
        callBackNum=1;
      }
    }
    GLES20.glUseProgram(program);
    GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);
    GLES20.glLineWidth(CONNECTION_THICKNESS);

    int numHands = result.multiHandLandmarks().size();

    for (int i = 0; i < numHands; ++i) {
      boolean isLeftHand = result.multiHandedness().get(i).getLabel().equals("Left");

      drawConnections(
          result.multiHandLandmarks().get(i).getLandmarkList(),
          isLeftHand ? LEFT_HAND_CONNECTION_COLOR : RIGHT_HAND_CONNECTION_COLOR);

      for (LandmarkProto.NormalizedLandmark landmark : result.multiHandLandmarks().get(i).getLandmarkList()) {
        // Draws the landmark.

        drawCircle(
            landmark.getX(),
            landmark.getY(),
            isLeftHand ? LEFT_HAND_LANDMARK_COLOR : RIGHT_HAND_LANDMARK_COLOR);
        // Draws a hollow circle around the landmark.
        drawHollowCircle(
            landmark.getX(),
            landmark.getY(),
            isLeftHand ? LEFT_HAND_HOLLOW_CIRCLE_COLOR : RIGHT_HAND_HOLLOW_CIRCLE_COLOR);

      }
      // Draws different images according to finger
      drawFig (findCoordinates(result), result.multiHandLandmarks().get(i).getLandmarkList(),projectionMatrix);

    }
  }


  private void drawFig(int numOfFinger, List<LandmarkProto.NormalizedLandmark> landmarkList,float[] projectionMatrix) {
    Log.i(TAG,"Number of fig"+numOfFinger);

    switch (numOfFinger){
      case 0:
        //setting image coordinates to show at position at hand
        image0.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image0.Draw(projectionMatrix);
           break;
      case 1:
        //setting image coordinates to show at position at hand
        image1.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image1.Draw(projectionMatrix);
            break;
      case 2:
            //drawBigCircle(landmarkList.get(0).getX(),landmarkList.get(0).getY(),color);
        //setting image coordinates to show at position at hand

        image2.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image2.Draw(projectionMatrix);
            break;
      case 3:
        //setting image coordinates to show at position at hand
        image3.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image3.Draw(projectionMatrix);
            //drawRact(landmarkList,color);
            break;
      case 4:
        //setting image coordinates to show at position at hand
        image4.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image4.Draw(projectionMatrix);
        break;
      default:
        //setting image coordinates to show at position at hand
        image5.setCoordinate(landmarkList.get(0).getX(),landmarkList.get(0).getY(),landmarkList.get(1).getX(),landmarkList.get(1).getY());
        image5.Draw(projectionMatrix);
        break;

    }

  }

  private void drawBigCircle(float x, float y, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    int vertexCount = NUM_SEGMENTS + 2;
    float[] vertices = new float[vertexCount * 3];
    x-=0.05f;
    y+=0.05f;
    vertices[0] = x;
    vertices[1] = y;
    vertices[2] = 0;
    for (int i = 1; i < vertexCount; i++) {
      float angle = 4.0f * i * (float) Math.PI / NUM_SEGMENTS;
      int currentIndex = 3 * i;
      vertices[currentIndex] = x + (float) (0.05f* Math.cos(angle));
      vertices[currentIndex + 1] = y + (float) (0.05f * Math.sin(angle));
      vertices[currentIndex + 2] = 0;
    }
    FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertices);
    vertexBuffer.position(0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);


  }

  private void drawRact(List<LandmarkProto.NormalizedLandmark> handLandmarkList, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);

    LandmarkProto.NormalizedLandmark start = handLandmarkList.get(0);
    LandmarkProto.NormalizedLandmark end = handLandmarkList.get(1);
    float[] vertex = {start.getX()-0.06f, start.getY()+0.05f,0f,
                      end.getX()-0.06f, end.getY(),0f,
                      end.getX(), end.getY(),0f,
                      start.getX()-0.06f, start.getY()+0.05f,0f,
                      start.getX(), start.getY(),0f,
                      end.getX(), end.getY(),0f
    };

    FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(vertex.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertex);
    vertexBuffer.position(0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle,3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

  }

  private void drawTran(List<LandmarkProto.NormalizedLandmark> handLandmarkList, float[] colorArray) {

    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);

      LandmarkProto.NormalizedLandmark start = handLandmarkList.get(0);
      LandmarkProto.NormalizedLandmark end = handLandmarkList.get(1);
      float[] vertex = {start.getX()-0.06f, start.getY()+0.05f,0f,
                      end.getX()-0.06f, end.getY(),0f,
                      end.getX(), end.getY(),0f
                    };
      FloatBuffer vertexBuffer =
              ByteBuffer.allocateDirect(vertex.length * 4)
                      .order(ByteOrder.nativeOrder())
                      .asFloatBuffer()
                      .put(vertex);
      vertexBuffer.position(0);
      GLES20.glEnableVertexAttribArray(positionHandle);
      GLES20.glVertexAttribPointer(positionHandle,3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

  }

  // Function for calculating the finger of hands
  private int findCoordinates(HandsResult result) {

    int numHands = result.multiHandLandmarks().size();
    int totalFingers = 0;
    if(numHands>0) {
      for (int x = 0; x < numHands; x++) {
        boolean left = result.multiHandedness().get(x).getLabel().equals("Left");
        totalFingers += calculateFingers(result.multiHandWorldLandmarks().get(x), left);
      }

      try {
        //updating the main activity layout like show the counting in the front page
        callback.updateMyText("" + totalFingers);
        callBackNum = 0;

      } catch (Exception e) {
        Log.i(TAG, e.toString());
      }

      // Log.i(TAG,"TotalFingers"+totalFingers);

    }
    return totalFingers;
  }

  private int calculateFingers(LandmarkProto.LandmarkList landmarkList,boolean isLeft) {
    int numberofFinger=0;
    //taking position of different fingers
    float yUp1 = landmarkList.getLandmark(8).getY();
    float yDown1 = landmarkList.getLandmark(7).getY();

    float yUp2 = landmarkList.getLandmark(12).getY();
    float yDown2 = landmarkList.getLandmark(11).getY();

    float yUp3 = landmarkList.getLandmark(16).getY();
    float yDown3 = landmarkList.getLandmark(15).getY();

    float yUp4 = landmarkList.getLandmark(20).getY();
    float yDown4 = landmarkList.getLandmark(19).getY();
    float yUp5 = landmarkList.getLandmark(4).getX();
    float yDown5 = landmarkList.getLandmark(1).getX();
    if (isLeft){
      if(yUp5>yDown5){
        ++numberofFinger;
      }
    }
    else {
      if(yUp5<yDown5){
        ++numberofFinger;
      }
    }

    if(yUp1<yDown1){
      ++numberofFinger;
    }
    if (yUp2<yDown2){
      ++numberofFinger;
    }
    if(yUp3<yDown3){
      ++numberofFinger;
    }
    if(yUp4<yDown4){
      ++numberofFinger;
    }
    return numberofFinger;
  }

  public void release() {
    GLES20.glDeleteProgram(program);
  }
//drawing the lines on hands
  private void drawConnections(List<LandmarkProto.NormalizedLandmark> handLandmarkList, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    for (Hands.Connection c : Hands.HAND_CONNECTIONS) {
      LandmarkProto.NormalizedLandmark start = handLandmarkList.get(c.start());
      LandmarkProto.NormalizedLandmark end = handLandmarkList.get(c.end());
      float[] vertex = {start.getX(), start.getY(), end.getX(), end.getY()};
      FloatBuffer vertexBuffer =
          ByteBuffer.allocateDirect(vertex.length * 4)
              .order(ByteOrder.nativeOrder())
              .asFloatBuffer()
              .put(vertex);
      vertexBuffer.position(0);
      GLES20.glEnableVertexAttribArray(positionHandle);
      GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
      GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
    }
  }

//drawing point of hands
  private void drawCircle(float x, float y, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    int vertexCount = NUM_SEGMENTS + 2;
    float[] vertices = new float[vertexCount * 3];
    vertices[0] = x;
    vertices[1] = y;
    vertices[2] = 0;
    for (int i = 1; i < vertexCount; i++) {
      float angle = 2.0f * i * (float) Math.PI / NUM_SEGMENTS;
      int currentIndex = 3 * i;
      vertices[currentIndex] = x + (float) (LANDMARK_RADIUS * Math.cos(angle));
      vertices[currentIndex + 1] = y + (float) (LANDMARK_RADIUS * Math.sin(angle));
      vertices[currentIndex + 2] = 0;
    }
    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);


  }
// Draw hollow circle outside the points
  private void drawHollowCircle(float x, float y, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    int vertexCount = NUM_SEGMENTS + 1;
    float[] vertices = new float[vertexCount * 3];
    for (int i = 0; i < vertexCount; i++) {
      float angle = 2.0f * i * (float) Math.PI / NUM_SEGMENTS;
      int currentIndex = 3 * i;
      vertices[currentIndex] = x + (float) (HOLLOW_CIRCLE_RADIUS * Math.cos(angle));
      vertices[currentIndex + 1] = y + (float) (HOLLOW_CIRCLE_RADIUS * Math.sin(angle));
      vertices[currentIndex + 2] = 0;
    }
    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
  }
}
