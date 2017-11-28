package com.example.ocrsearch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.ocrsearch.camera.CameraManager;
import com.example.ocrsearch.common.DirTraversal;
import com.example.ocrsearch.common.ZipUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

public class MainActivity extends Activity {
	public static final boolean debug = true;
	private static final String TESSBASE_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/tessdata/";
	private static final String DEFAULT_LANGUAGE = "eng";
	//private static final String IMAGE_PATH = "/mnt/sdcard/test1.jpg";
	private static final String EXPECTED_FILE = TESSBASE_PATH + "tessdata/"
			+ DEFAULT_LANGUAGE + ".traineddata";
	private TessBaseAPI baseApi;
	private SurfaceView mainSurface;
	private SurfaceHolder msurfaceHolder;
	private CameraManager cameraManager;
	private OcrFinderView orcFindView;
	private boolean hasSurface = false;
	private static String TAG = MainActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.capture);
		baseApi = new TessBaseAPI();
		File file = new File(EXPECTED_FILE);
		file.mkdirs();
		File parent = file.getParentFile();
		if (parent.listFiles().length < 2) {

			// 把 Assert里的字库copy到文件下
			try {
				InputStream in = getResources().getAssets()
						.open("tessdata.zip");
				File zip = new File(TESSBASE_PATH + "tessdata.zip");
				OutputStream out = new FileOutputStream(zip);
				byte[] temp = new byte[1024];
				int size = -1;
				while ((size = in.read(temp)) != -1) {
					out.write(temp, 0, size);
				}
				out.flush();
				out.close();
				in.close();
				// 解压其文件夹
				try {
					ZipUtils.upZipFile(zip, TESSBASE_PATH);
				} catch (ZipException e) {
					// TODO
					e.printStackTrace();
				} catch (IOException e) {
					// TODO
					e.printStackTrace();
				}
				// 删除压缩文件.zip
				zip.deleteOnExit();
			} catch (IOException e) {
				// TODO
				e.printStackTrace();
			}
		}
		baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
		Bitmap mp = BitmapFactory.decodeResource(getResources(),
				R.drawable.number1);

		mp = mp.copy(Bitmap.Config.ARGB_8888, false);
		baseApi.setImage(mp);

		String value = baseApi.getUTF8Text();
		Log.d("tag", " the value is ===> " + value);
		baseApi.clear();
		baseApi.end();

		mainSurface = (SurfaceView) findViewById(R.id.mainSurface);
		mainSurface.getHolder().setFixedSize(10,5);
		msurfaceHolder = mainSurface.getHolder();

	}

	@Override
	protected void onResume() {
		// TODO
		super.onResume();
		cameraManager = new CameraManager(getApplicationContext(), this);
		orcFindView = (OcrFinderView) findViewById(R.id.ocrFindView);
		orcFindView.setCameraManager(cameraManager);

		if (hasSurface) {
			initCamera(msurfaceHolder);
		} else {
			msurfaceHolder.addCallback(surcallback);
			msurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

	}
	public static final int DECODE = 0;
	public static final int QUIT = DECODE + 1;
	public static final int DECOCE_FAIL = QUIT + 1;
	private Handler decodeHandle = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case DECODE :
					// decode((byte[]) msg.obj, msg.arg1, msg.arg2);
					break;
				case QUIT :

					break;
				case DECOCE_FAIL :
					cameraManager.requestPreviewFrame(decodeHandle, DECODE,
							new PreviewCallback());
					break;
				default :
					break;
			}
		};
	};
	public String decodeBitmapValue(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}
		
		// 灰度化图片
        // bitmap = bitmap2Gray(bitmap);
		//二值化图片
	//	 bitmap = gray2Binary(bitmap);
		
		
		baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);

		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
		baseApi.setImage(bitmap);

		String value = baseApi.getUTF8Text();
		Log.d("tag", " the value is ===> " + value);
		baseApi.clear();
		baseApi.end();
		return value;
	}
	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	// private void decode(byte[] data, int width, int height) {}

	class PreviewCallback implements Camera.PreviewCallback {

		// private static final String TAG =
		// PreviewCallback.class.getSimpleName();

		// private CameraConfigurationManager configManager;
		private Handler previewHandler;
		private int previewMessage;

		PreviewCallback() {
			// this.configManager = configManager;
		}

		void setHandler(Handler previewHandler, int previewMessage) {
			this.previewHandler = previewHandler;
			this.previewMessage = previewMessage;
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {

			if (data != null) {
				Camera.Parameters parameters = camera.getParameters();
				int imageFormat = parameters.getPreviewFormat();
				Log.i("map", "Image Format: " + imageFormat
						+ " ImageFormat.NV21 is " + ImageFormat.NV21);

				Log.i("CameraPreviewCallback", "data length:" + data.length);

				if (imageFormat == ImageFormat.NV21) {
					Log.i("map", "Image Format: " + imageFormat);
					// get full picture
					Bitmap image = null;
					int w = parameters.getPreviewSize().width;
					int h = parameters.getPreviewSize().height;
					// PlanarYUVLuminanceSource source =
					// buildLuminanceSource(data, w,
					// h);
					// Rect rectv = cameraManager.getFramingRectInPreview();
					// LuminanceSource lumin = source.crop(0, 0, rectv.width(),
					// rectv.height());
					// byte[] txt = lumin.getMatrix();

					Rect rect = new Rect(0, 0, w, h);

					YuvImage img = new YuvImage(data, ImageFormat.NV21, w, h,
							null);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					boolean show = false;
					if (img.compressToJpeg(rect, 100, baos)) {
						image = BitmapFactory.decodeByteArray(
								baos.toByteArray(), 0, baos.size());
						image = cutBitmap(image,
								cameraManager.getFramingRectInPreview(),
								Bitmap.Config.ARGB_8888);

						// imageView.setImageBitmap(image);
						if (image != null) {
							if (debug) {
								//ImageView vv = (ImageView) findViewById(R.id.igv);
								//vv.setImageBitmap(image);
							}

							String v = decodeBitmapValue(image);
							if (isNumber(v)) {
								show = true;
								AlertDialog d = new AlertDialog.Builder(
										MainActivity.this)
										.setTitle(R.string.app_name)
										.setMessage(v)
										.setPositiveButton(
												"ok",
												new DialogInterface.OnClickListener() {

													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
														// TODO
														decodeHandle
																.removeMessages(DECOCE_FAIL);
														Message msg = Message
																.obtain(decodeHandle,
																		DECOCE_FAIL);
														// msg.sendToTarget();
														decodeHandle
																.sendMessageDelayed(
																		msg,
																		800);
													}
												}).create();
								d.show();
							}
						}

					}
					if (!show) {
						decodeHandle.removeMessages(DECOCE_FAIL);
						Message msg = Message.obtain(decodeHandle, DECOCE_FAIL);
						// msg.sendToTarget();
						decodeHandle.sendMessageDelayed(msg, 800);
					}
				}
			} else {
				Log.i("CameraPreviewCallback", "data is null :");
			}

			// Point cameraResolution = configManager.getCameraResolution();
			// Handler thePreviewHandler = previewHandler;
			// if (cameraResolution != null && thePreviewHandler != null) {
			// Message message = thePreviewHandler.obtainMessage(previewMessage,
			// cameraResolution.x,
			// cameraResolution.y, data);
			// message.sendToTarget();
			// previewHandler = null;
			// Log.d(TAG, "send the data ..... ok ");
			// } else {
			// Log.d(TAG,
			// "Got preview callback, but no handler or resolution available");
			// }
		}

	}
	private boolean isNumber(String str) {
		//Pattern pattern = Pattern.compile("[0-9]+");
		Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
		//Pattern pattern = Pattern.compile("[a-z0-9]+");
		Matcher matcher = pattern.matcher((CharSequence) str);
		boolean result = matcher.matches();
		if (result) {
			System.out.println("true");
		} else {
			System.out.println("false");
		}
		return result;
		//return true;
	}

	public static Bitmap cutBitmap(Bitmap mBitmap, Rect r, Bitmap.Config config) {
		int width = r.width();
		int height = r.height();

		Bitmap croppedImage = Bitmap.createBitmap(width, height, config);

		Canvas cvs = new Canvas(croppedImage);
		Rect dr = new Rect(0, 0, width, height);

		cvs.drawBitmap(mBitmap, r, dr, null);

		return croppedImage;
	}
	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Rect rect = cameraManager.getFramingRectInPreview();
		if (rect == null) {
			return null;
		}
		// Go ahead and assume it's YUV rather than die.
		return new PlanarYUVLuminanceSource(data, width, height, rect.left,
				rect.top, rect.width(), rect.height(), false);
	}

	@Override
	protected void onPause() {
		cameraManager.stopPreview();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.mainSurface);
 
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(surcallback);
		}
		super.onPause();
	}

	private SurfaceHolder.Callback surcallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO
			hasSurface = false;
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO
			if (holder == null) {
				Log.e(TAG,
						"*** WARNING *** surfaceCreated() gave us a null surface!");
			}
			if (!hasSurface) {
				hasSurface = true;
				initCamera(holder);
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO
			//mainSurface.setD.setDisplay(holder);
		}
	};

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			cameraManager.startPreview();
			cameraManager.requestPreviewFrame(decodeHandle, DECODE,
					new PreviewCallback());
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			// if (handler == null) {
			// handler = new CaptureActivityHandler(this, decodeFormats,
			// characterSet, cameraManager);
			// }
			// decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			// displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			// displayFrameworkBugMessageAndExit();
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	 // 该函数实现对图像进行二值化处理  
    public Bitmap gray2Binary(Bitmap graymap) {  
        //得到图形的宽度和长度  
        int width = graymap.getWidth();  
        int height = graymap.getHeight();  
        //创建二值化图像  
        Bitmap binarymap = null;  
        binarymap = graymap.copy(Config.ARGB_8888, true);  
        //依次循环，对图像的像素进行处理  
        for (int i = 0; i < width; i++) {  
            for (int j = 0; j < height; j++) {  
                //得到当前像素的值  
                int col = binarymap.getPixel(i, j);  
                //得到alpha通道的值  
                int alpha = col & 0xFF000000;  
                //得到图像的像素RGB的值  
                int red = (col & 0x00FF0000) >> 16;  
                int green = (col & 0x0000FF00) >> 8;  
                int blue = (col & 0x000000FF);  
                // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB  
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);  
                //对图像进行二值化处理  
                if (gray <= 130) {  
                    gray = 0;  
                } else {  
                    gray = 255;  
                }  
                // 新的ARGB  
                int newColor = alpha | (gray << 16) | (gray << 8) | gray;  
                //设置新图像的当前像素值  
                binarymap.setPixel(i, j, newColor);  
            }  
        }  
        return binarymap;  
    }  
    
    public Bitmap bitmap2Gray(Bitmap bmSrc) {  
        // 得到图片的长和宽  
        int width = bmSrc.getWidth();  
        int height = bmSrc.getHeight();  
        // 创建目标灰度图像  
        Bitmap bmpGray = null;  
        bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);  
        // 创建画布  
        Canvas c = new Canvas(bmpGray);  
        Paint paint = new Paint();  
        ColorMatrix cm = new ColorMatrix();  
        cm.setSaturation(0);  
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);  
        paint.setColorFilter(f);  
        c.drawBitmap(bmSrc, 0, 0, paint);  
        return bmpGray;  
    }  
}
