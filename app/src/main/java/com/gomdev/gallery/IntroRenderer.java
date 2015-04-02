package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 15. 4. 1..
 */
public class IntroRenderer implements GLSurfaceView.Renderer {
    private static final String CLASS = "IntroRenderer";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final float TEXT_SHADOW_RADIUS = 1f;
    private static final float TEXT_SHADOW_DX = 0.5f;
    private static final float TEXT_SHADOW_DY = 0.5f;
    private static final float TEXT_MARGIN = 1f;
    private static final int SHADOW_COLOR = 0xFF666666;

    private static final int NUM_OF_POINT_IN_GOM_WIDTH = 100;
    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_TEXCOORD = 2;

    static {
        System.loadLibrary("gomdev");
    }

    private class Particle {
        float mX = 0f;
        float mY = 0f;
        float mZ = 0f;

        float mInitX = 0f;
        float mInitY = 0f;
        float mInitZ = 0f;

        float mVelocity = 1f;

        Particle(float x, float y, float z) {
            mX = x;
            mY = y;
            mZ = z;
        }
    }

    private class ParticleSet {
        ArrayList<Particle> mParticles = new ArrayList<>();

        GLESShader mShader = null;

        int mNumOfPointsInWidth = 0;
        int mNumOfPointsInHeight = 0;
        int mNumOfPoints = 0;

        float mX = 0f;
        float mY = 0f;

        float mWidth = 0f;
        float mHeight = 0f;

        float mPointSize = 0f;
    }

    private final Context mContext;
    private final GLESRenderer mGLESRenderer;
    private final GLESSceneManager mSM;
    private final GLESNode mRoot;
    private final GLESObject mGomObject;
    private final GLESObject mGalleryObject;

    private GLSurfaceView mSurfaceView = null;
    private Random mRandom = new Random();
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private GLESShader mGomShader = null;
    private GLESShader mGalleryShader = null;

    private GLESAnimator mAnimator = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private float mShadowRadius = 0f;
    private float mShadowDx = 0f;
    private float mShadowDy = 0f;
    private float mTextSize = 0f;
    private float mTextMargin = 0f;

    private int mTextColor = 0;

    private ParticleSet mGomParticleSet = new ParticleSet();
    private ParticleSet mGalleryParticleSet = new ParticleSet();

    IntroRenderer(Context context) {
        if (DEBUG) {
            Log.d(TAG, "IntroRenderer()");
        }

        mContext = context;

        mGLESRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);
        state.setBlendState(true);
        state.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mGomObject = mSM.createObject("Gom");
        mGomObject.setGLState(state);
        mRoot.addChild(mGomObject);

        mGalleryObject = mSM.createObject("Gallery");
        mGalleryObject.setGLState(state);
        mRoot.addChild(mGalleryObject);

        mShadowRadius = GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_RADIUS);
        mShadowDx = GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DX);
        mShadowDy = GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DY);
        mTextMargin = GLESUtils.getPixelFromDpi(mContext, TEXT_MARGIN);
        mTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.intro_text_size);

        mTextColor = mContext.getResources().getColor(R.color.actionBarTitleText);

        mAnimator = new GLESAnimator(mAnimatorCB);
        mAnimator.setValues(0f, 1f);
        mAnimator.setDuration(0L, GalleryConfig.INTRO_ANIMATION_DURATION);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }

        mGLESRenderer.updateScene(mSM);
        mGLESRenderer.drawScene(mSM);
    }

    // onSurfaceChanged

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;

        mGLESRenderer.reset();

        GLESCamera camera = setupCamera(width, height);
        mGomObject.setCamera(camera);
        mGalleryObject.setCamera(camera);

        Bitmap gomBitmap = createTextBitmap("Gom");
        float gomWidth = gomBitmap.getWidth();
        float gomHeight = gomBitmap.getHeight();

        GLESTexture gomTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, (int) gomWidth, (int) gomHeight)
                .load(gomBitmap);
        mGomObject.setTexture(gomTexture);
        gomBitmap.recycle();

        Bitmap galleryBitmap = createTextBitmap("Gallery");
        float galleryWidth = galleryBitmap.getWidth();
        float galleryHeight = galleryBitmap.getHeight();

        GLESTexture galleryTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, (int) galleryWidth, (int) galleryHeight)
                .load(galleryBitmap);
        mGalleryObject.setTexture(galleryTexture);
        galleryBitmap.recycle();

        float margin = (mWidth - (gomWidth * 0.5f + galleryWidth)) * 0.5f;

        {
            float gomStartX = -mWidth * 0.5f + margin;
            float gomStartY = gomHeight;

            float pointSize = Math.round(gomWidth / NUM_OF_POINT_IN_GOM_WIDTH);;
            int numOfPointsInWidth = (int) Math.ceil(gomWidth / pointSize);
            float temp = numOfPointsInWidth * gomHeight / gomWidth;
            int numOfPointsInHeight = (int) Math.ceil(temp);
            int numOfPoints = numOfPointsInWidth * numOfPointsInHeight;

            mGomParticleSet.mX = gomStartX;
            mGomParticleSet.mY = gomStartY;
            mGomParticleSet.mWidth = gomWidth;
            mGomParticleSet.mHeight = gomHeight;
            mGomParticleSet.mNumOfPointsInWidth = numOfPointsInWidth;
            mGomParticleSet.mNumOfPointsInHeight = numOfPointsInHeight;
            mGomParticleSet.mNumOfPoints = numOfPoints;
            mGomParticleSet.mPointSize = pointSize;
            mGomParticleSet.mShader = mGomShader;

            GLESVertexInfo gomVertexInfo = createParticles(mGomParticleSet);
            mGomObject.setVertexInfo(gomVertexInfo, false, false);

            setUniforms(mGomParticleSet);

            Log.d(TAG, "onSurfaceChanged()");
            Log.d(TAG, "\t gom width=" + gomWidth + " height=" + gomHeight);
            Log.d(TAG, "\t gallery width=" + galleryWidth + " height=" + galleryHeight);
            Log.d(TAG, "\t pointSize=" + pointSize);
        }

        {
            float galleryStartX = -mWidth * 0.5f + margin + gomWidth * 0.5f;
            float galleryStartY = 0f;

            float pointSize = Math.round(gomWidth / NUM_OF_POINT_IN_GOM_WIDTH);;
            int numOfPointsInWidth = (int) Math.ceil(galleryWidth / pointSize);
            float temp = numOfPointsInWidth * galleryHeight / galleryWidth;
            int numOfPointsInHeight = (int) Math.ceil(temp);
            int numOfPoints = numOfPointsInWidth * numOfPointsInHeight;

            mGalleryParticleSet.mX = galleryStartX;
            mGalleryParticleSet.mY = galleryStartY;
            mGalleryParticleSet.mWidth = galleryWidth;
            mGalleryParticleSet.mHeight = galleryHeight;
            mGalleryParticleSet.mNumOfPointsInWidth = numOfPointsInWidth;
            mGalleryParticleSet.mNumOfPointsInHeight = numOfPointsInHeight;
            mGalleryParticleSet.mNumOfPoints = numOfPoints;
            mGalleryParticleSet.mPointSize = pointSize;
            mGalleryParticleSet.mShader = mGalleryShader;

            GLESVertexInfo galleryVertexInfo = createParticles(mGalleryParticleSet);
            mGalleryObject.setVertexInfo(galleryVertexInfo, false, false);

            setUniforms(mGalleryParticleSet);
        }

        mAnimator.start();
    }

    private void setUniforms(ParticleSet particleSet) {
        particleSet.mShader.useProgram();

        int location = GLES20.glGetUniformLocation(particleSet.mShader.getProgram(),
                "uPointSize");
        GLES20.glUniform1f(location, particleSet.mPointSize);

        location = GLES20.glGetUniformLocation(particleSet.mShader.getProgram(),
                "uPointSizeInTexCoord");
        float texCoordFactorX = particleSet.mPointSize / particleSet.mWidth;
        float texCoordFactorY = particleSet.mPointSize / particleSet.mHeight;
        GLES20.glUniform2f(location, texCoordFactorX, texCoordFactorY);
    }


    private GLESVertexInfo createParticles(ParticleSet particleSet) {
        particleSet.mParticles.clear();

        float[] position = new float[particleSet.mNumOfPoints * NUM_ELEMENT_OF_POSITION];
        float[] texCoord = new float[particleSet.mNumOfPoints * NUM_ELEMENT_OF_TEXCOORD];

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        int posIndexOffsetX = 0;
        int posIndexOffsetY = 0;
        int texIndexOffsetX = 0;
        int texIndexOffsetY = 0;

        float halfPointSize = particleSet.mPointSize * 0.5f;

        for (int i = 0; i < particleSet.mNumOfPointsInHeight; i++) {
            posIndexOffsetY = i * NUM_ELEMENT_OF_POSITION * particleSet.mNumOfPointsInWidth;
            texIndexOffsetY = i * NUM_ELEMENT_OF_TEXCOORD * particleSet.mNumOfPointsInWidth;

            posX = particleSet.mX + halfPointSize;
            posY = particleSet.mY - i * particleSet.mPointSize - halfPointSize;

            for (int j = 0; j < particleSet.mNumOfPointsInWidth; j++) {
                posIndexOffsetX = j * NUM_ELEMENT_OF_POSITION;
                texIndexOffsetX = j * NUM_ELEMENT_OF_TEXCOORD;

                posX += particleSet.mPointSize;

                position[posIndexOffsetY + posIndexOffsetX + 0] = posX;
                position[posIndexOffsetY + posIndexOffsetX + 1] = posY;
                position[posIndexOffsetY + posIndexOffsetX + 2] = posZ;

                Particle particle = new Particle(posX, posY, posZ);
                particleSet.mParticles.add(particle);

                if (particleSet == mGomParticleSet) {
                    particle.mInitX = -mWidth - mRandom.nextFloat() * 0.2f * mWidth;
                } else {
                    particle.mInitX = mWidth + mRandom.nextFloat() * 0.2f * mWidth;
                }

                particle.mInitY = (mRandom.nextFloat() - 0.5f) * mHeight;
                particle.mInitZ = 0f;

                particle.mVelocity = 1f + mRandom.nextFloat() * 0.3f;

                texCoord[texIndexOffsetY + texIndexOffsetX + 0] = (j * particleSet.mPointSize + halfPointSize) / particleSet.mWidth;
                texCoord[texIndexOffsetY + texIndexOffsetX + 1] = (i * particleSet.mPointSize + halfPointSize) / particleSet.mHeight;
            }
        }

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(particleSet.mShader.getPositionAttribIndex(), position, NUM_ELEMENT_OF_POSITION);
        vertexInfo.setBuffer(particleSet.mShader.getTexCoordAttribIndex(), texCoord, NUM_ELEMENT_OF_TEXCOORD);

        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.POINTS);
        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);

        return vertexInfo;
    }

    private GLESCamera setupCamera(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "setupCamera() width=" + width + " hegiht=" + height);
        }

        GLESCamera camera = new GLESCamera();

        float eyeZ = 0.1f;

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);
        camera.setOrtho(-mWidth * 0.5f, mWidth * 0.5f, -mHeight * 0.5f, mHeight * 0.5f, -1f, 1f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    private Bitmap createTextBitmap(String text) {
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(
                mShadowRadius,
                mShadowDx,
                mShadowDy,
                SHADOW_COLOR);
        textPaint.setTextSize(mTextSize);
        textPaint.setARGB(0xFF, 0x00, 0x00, 0x00);
        textPaint.setColor(mTextColor);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);

        Rect bound = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bound);

        int ascent = (int) Math.ceil(-textPaint.ascent());
        int descent = (int) Math.ceil(textPaint.descent());

        int textHeight = ascent + descent;

        int width = (int) (textPaint.measureText(text) + mTextMargin * 2);
        int height = (int) (textHeight + mTextMargin * 2);


        int x = (int) mTextMargin;
        int y = (height - textHeight) / 2 + ascent;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        bitmap = GLESUtils.drawTextToBitmap(x, y,
                text, textPaint, bitmap);

        return bitmap;
    }

    // onSurfaceCreated

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        mGomShader = createShader(R.raw.particle_texture_20_vs, R.raw.particle_texture_20_fs);
        mGomObject.setShader(mGomShader);

        mGalleryShader = createShader(R.raw.particle_texture_20_vs, R.raw.particle_texture_20_fs);
        mGalleryObject.setShader(mGalleryShader);
    }

    private GLESShader createShader(int vsResID, int fsResID) {
        GLESShader textureShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, vsResID);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, fsResID);

        textureShader.setShaderSource(vsSource, fsSource);
        if (textureShader.load() == false) {
            return null;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        textureShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
        textureShader.setTexCoordAttribIndex(attribName);

        return textureShader;
    }

    // CB / Listener

    // set / get

    void setSurfaceView(GLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    private final GLESAnimatorCallback mAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = mInterpolator.getInterpolation(current.getX());

            {
                GLESShader shader = mGomParticleSet.mShader;
                ArrayList<Particle> particles = mGomParticleSet.mParticles;

                GLESVertexInfo vertexInfo = mGomObject.getVertexInfo();
                FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(shader.getPositionAttribIndex());

                float x = 0f;
                float y = 0f;

                float scaledNormalizedValue = 0f;

                int size = particles.size();
                for (int i = 0; i < size; i++) {
                    Particle particle = particles.get(i);

                    scaledNormalizedValue = normalizedValue * particle.mVelocity;

                    if (scaledNormalizedValue > 1f) {
                        scaledNormalizedValue = 1f;
                    }

                    x = particle.mInitX + (particle.mX - particle.mInitX) * scaledNormalizedValue;
                    y = particle.mInitY + (particle.mY - particle.mInitY) * scaledNormalizedValue;

                    position.put(i * NUM_ELEMENT_OF_POSITION + 0, x);
                    position.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
                }
            }

            {
                GLESShader shader = mGalleryParticleSet.mShader;
                ArrayList<Particle> particles = mGalleryParticleSet.mParticles;

                GLESVertexInfo vertexInfo = mGalleryObject.getVertexInfo();
                FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(shader.getPositionAttribIndex());

                float x = 0f;
                float y = 0f;

                float scaledNormalizedValue = 0f;

                int size = particles.size();
                for (int i = 0; i < size; i++) {
                    Particle particle = particles.get(i);

                    scaledNormalizedValue = normalizedValue * particle.mVelocity;

                    if (scaledNormalizedValue > 1f) {
                        scaledNormalizedValue = 1f;
                    }

                    x = particle.mInitX + (particle.mX - particle.mInitX) * scaledNormalizedValue;
                    y = particle.mInitY + (particle.mY - particle.mInitY) * scaledNormalizedValue;

                    position.put(i * NUM_ELEMENT_OF_POSITION + 0, x);
                    position.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
                }
            }
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {

        }
    };
}
