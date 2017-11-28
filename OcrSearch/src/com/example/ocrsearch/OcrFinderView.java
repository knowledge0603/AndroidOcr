package com.example.ocrsearch;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.example.ocrsearch.camera.CameraManager;

public class OcrFinderView extends View {

	private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128,
			64};
	private static final long ANIMATION_DELAY = 80L;
	private static final int CURRENT_POINT_OPACITY = 0xA0;
	private static final int MAX_RESULT_POINTS = 20;
	private static final int POINT_SIZE = 6;

	private final Paint paint;
	private final int maskColor;
	private final int resultColor;
	private final int laserColor;
	private final int resultPointColor;
	private int scannerAlpha;
	private CameraManager cameraManager;
	private Bitmap resultBitmap;
	public OcrFinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}
		Rect frame = cameraManager.getFramingRect();
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		
		// Draw the exterior (i.e. outside the framing rect) darkened
	    paint.setColor(resultBitmap != null ? resultColor : maskColor);
	    canvas.drawRect(0, 0, width, frame.top, paint);
	    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
	    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
	    canvas.drawRect(0, frame.bottom + 1, width, height, paint);
	    
	    if (resultBitmap != null) {
	        // Draw the opaque result bitmap over the scanning rectangle
	        paint.setAlpha(CURRENT_POINT_OPACITY);
	        canvas.drawBitmap(resultBitmap, null, frame, paint);
	      } else {

	        // Draw a red "laser scanner" line through the middle to show decoding is active
	        paint.setColor(laserColor);
	        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
	        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
	        int middle = frame.height() / 2 + frame.top;
	        canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
	        
	        Rect previewFrame = cameraManager.getFramingRectInPreview();
	        float scaleX = frame.width() / (float) previewFrame.width();
	        float scaleY = frame.height() / (float) previewFrame.height();

//	        List<ResultPoint> currentPossible = possibleResultPoints;
//	        List<ResultPoint> currentLast = lastPossibleResultPoints;
//	        int frameLeft = frame.left;
//	        int frameTop = frame.top;
//	        if (currentPossible.isEmpty()) {
//	          lastPossibleResultPoints = null;
//	        } else {
//	          possibleResultPoints = new ArrayList<ResultPoint>(5);
//	          lastPossibleResultPoints = currentPossible;
//	          paint.setAlpha(CURRENT_POINT_OPACITY);
//	          paint.setColor(resultPointColor);
//	          synchronized (currentPossible) {
//	            for (ResultPoint point : currentPossible) {
//	              canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//	                                frameTop + (int) (point.getY() * scaleY),
//	                                POINT_SIZE, paint);
//	            }
//	          }
//	        }
//	        if (currentLast != null) {
//	          paint.setAlpha(CURRENT_POINT_OPACITY / 2);
//	          paint.setColor(resultPointColor);
//	          synchronized (currentLast) {
//	            float radius = POINT_SIZE / 2.0f;
//	            for (ResultPoint point : currentLast) {
//	              canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//	                                frameTop + (int) (point.getY() * scaleY),
//	                                radius, paint);
//	            }
//	          }
//	        }

	        // Request another update at the animation interval, but only repaint the laser line,
	        // not the entire viewfinder mask.
	        postInvalidateDelayed(ANIMATION_DELAY,
	                              frame.left - POINT_SIZE,
	                              frame.top - POINT_SIZE,
	                              frame.right + POINT_SIZE,
	                              frame.bottom + POINT_SIZE);
	      }
	}
	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		invalidate();
	}
}
