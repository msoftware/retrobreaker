package br.usp.ime.ep2;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

class TouchSurfaceView extends GLSurfaceView {
	
	private static final String TAG = TouchSurfaceView.class.getSimpleName();
	private static final float WALL = 0.05f;

	private long mPrevFrameTime;

	private Renderer mRenderer;

	private int mScreenWidth;
	private int mScreenHeight;

	private float[] mUnprojectViewMatrix = new float[16];
	private float[] mUnprojectProjMatrix = new float[16];

	private class Renderer implements GLSurfaceView.Renderer {

		private Game mGame;

		public Renderer() {
			mGame = new Game();
			mPrevFrameTime = System.nanoTime()/Constants.NANOS_PER_SECONDS;
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			long currentTime = System.nanoTime();
			long deltaTime = (currentTime - mPrevFrameTime)/Constants.NANOS_PER_SECONDS;
			mPrevFrameTime = currentTime;
		
			Log.v(TAG, "FPS: " + Constants.MS_PER_SECONDS / deltaTime);
			
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			mGame.updateState();
			// Sometimes deltaTime is very high (when the game starts for example)
			// So we need to check for that
			mGame.drawElements(gl,
					deltaTime < Constants.MS_PER_FRAME ? deltaTime : Constants.MS_PER_FRAME);
		}
		
//		@Override
//		public void onCreate(int width, int height, boolean contextLost) {
//			
//		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, 0, width, height);
			mScreenWidth = width;
			mScreenHeight = height;
			float ratio = (float) width / height;
			mGame.updateScreenMeasures((2.0f * ratio) - WALL, 2.0f - WALL);

			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrthof(-ratio, ratio, -1.0f, 1.0f, -1.0f, 1.0f);

			Matrix.orthoM(mUnprojectProjMatrix, 0, -ratio, ratio, -1.0f, 1.0f, -1.0f, 1.0f);
			Matrix.setIdentityM(mUnprojectViewMatrix, 0);
		}


		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			mScreenWidth = TouchSurfaceView.this.getWidth();
			mScreenHeight = TouchSurfaceView.this.getHeight();
			
			gl.glDisable(GL10.GL_DITHER);
			gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

			gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glDisable(GL10.GL_CULL_FACE);
			gl.glShadeModel(GL10.GL_SMOOTH);
			gl.glDisable(GL10.GL_DEPTH_TEST);
		}

		public void updatePaddlePosition(final float x, final float y) {
			queueEvent(new Runnable() {
				@Override
				public void run() {
					mGame.updatePaddleXPosition(x);
				}
			} );
		}
	}

	public TouchSurfaceView(Context context) {
		super(context);
		mRenderer = new Renderer();
		setRenderer(mRenderer);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			final float screenX = e.getX();
			final float screenY = mScreenHeight - e.getY();

			final int[] viewport = {
					0, 0, mScreenWidth, mScreenHeight
			};

			float[] resultWorldPos = {
					0.0f, 0.0f, 0.0f, 0.0f
			};

			GLU.gluUnProject(screenX, screenY, 0, mUnprojectViewMatrix, 0, mUnprojectProjMatrix, 0, viewport, 0, resultWorldPos, 0);
			resultWorldPos[0] /= resultWorldPos[3];
			resultWorldPos[1] /= resultWorldPos[3];
			resultWorldPos[2] /= resultWorldPos[3];
			resultWorldPos[3] = 1.0f;

			mRenderer.updatePaddlePosition(resultWorldPos[0], resultWorldPos[1]);
			break;
		}
		return true;
	}
}