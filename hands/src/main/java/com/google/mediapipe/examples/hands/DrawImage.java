package com.google.mediapipe.examples.hands;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

//This will draw the image on hand
public class DrawImage {

    static String TAG ="DrawImage";
    //Added for Textures
    private final Context mActivityContext;


    // setting image for working
    public void setBitmap(int resourceId) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inScaled = true;   // No pre-scaling
        this.bitmap = BitmapFactory.decodeResource(mActivityContext.getResources(), resourceId, options);
    }

    Bitmap bitmap;
    //Added for Textures
    private FloatBuffer mCubeTextureCoordinates;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;
    private final int mTextureCoordinateDataSize = 2;
    private int mTextureDataHandle;

    private final String vertexShaderCode =
//Test
            "attribute vec2 a_TexCoordinate;" +
                    "varying vec2 v_TexCoordinate;" +
//End Test
                    "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;"+
                    //Test
                    "v_TexCoordinate = a_TexCoordinate;" +
                    //End Test
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
//Test
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoordinate;" +
//End Test
                    "void main() {" +
                    //"gl_FragColor = vColor;" +
                    "gl_FragColor = (vColor * texture2D(u_Texture, v_TexCoordinate));" +
                    "}";

    private final int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    static float spriteCoords[] = { 0.2f,  0.1f,   // top left
            0.2f, 0.2f,   // bottom left
            0.4f, 0.2f,   // bottom right
            0.4f,  0.1f }; //top right

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; //Order to draw vertices
    private final int vertexStride = COORDS_PER_VERTEX * 4; //Bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
    public void setVertex(float x_bottom , float y_bottom,float x_top,float y_top ){
        spriteCoords = new float[] { x_top,  0.0f+y_top,   // top left
                0.0f+x_bottom, 0.0f+y_bottom,   // bottom left
                0.1f+x_bottom, 0.1f+y_bottom,   // bottom right
                0.1f+x_top,  0.1f+y_top };

    }
    public DrawImage(final Context activityContext,int resouceId)
    {
        mActivityContext = activityContext;
        setCoordinate(0.2f,0.2f,0.1f,0.1f);
       int vertexShader = HandsResultGlRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
      int fragmentShader = HandsResultGlRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        shaderProgram=GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);

        //Texture Code
        GLES20.glBindAttribLocation(shaderProgram, 0, "a_TexCoordinate");

       GLES20.glLinkProgram(shaderProgram);
       // mTextureDataHandle = loadTexture(mActivityContext, R.mipmap.four_foreground);
        setBitmap(resouceId);
        mTextureDataHandle =loadTexture();
    }

    //updating coordinate of hand
    public void setCoordinate(float x_bottom , float y_bottom,float x_top,float y_top ){
        //Initialize Vertex Byte Buffer for Shape Coordinates / # of coordinate values * 4 bytes per float
        setVertex(x_bottom,y_bottom,x_top,y_top);
        ByteBuffer bb = ByteBuffer.allocateDirect(spriteCoords.length * 4);
        //Use the Device's Native Byte Order
        bb.order(ByteOrder.nativeOrder());
        //Create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        //Add the coordinates to the FloatBuffer
        vertexBuffer.put(spriteCoords);
        //Set the Buffer to Read the first coordinate
        vertexBuffer.position(0);
        for(int x=0;x<spriteCoords.length;x++) {
            Log.i(TAG, spriteCoords[x]+"");
        }

        // S, T (or X, Y)
        // Texture coordinate data.
        // Because images have a Y axis pointing downward (values increase as you move down the image) while
        // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
        // What's more is that the texture coordinates are the same for every face.
        final float[] cubeTextureCoordinateData = {
                1f,0f, 1f,1f, 0f,1f, 0f,0f };
        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);

        //Initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(spriteCoords.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
        //Load the texture


    }
    public void Draw(float[] mvpMatrix)
    {
        //Add program to OpenGL ES Environment

        GLES20.glUseProgram(shaderProgram);

        //Get handle to vertex shader's vPosition member
       mPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");

        //Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(this.mPositionHandle);

        //Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        //Get Handle to Fragment Shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");

        //Set the Color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        //Set Texture Handles and bind Texture
        mTextureUniformHandle = GLES20.glGetAttribLocation(shaderProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoordinate");

        //Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        //Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        //Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        //Get Handle to Shape's Transformation Matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        //Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        //Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        //Disable Vertex Array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public  int loadTexture()
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {


                // Read in the resource

                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_REPEAT);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();

        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}
