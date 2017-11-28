package com.example.orcsearch;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {
	private static final String TESSBASE_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/tessdata/";
	private static final String DEFAULT_LANGUAGE = "eng";
	private static final String IMAGE_PATH = "/mnt/sdcard/test1.jpg";
	private static final String EXPECTED_FILE = TESSBASE_PATH + "tessdata/"
			+ DEFAULT_LANGUAGE + ".traineddata";
	private TessBaseAPI baseApi;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		baseApi = new TessBaseAPI();
		baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
		baseApi.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.number));
		String value = baseApi.getUTF8Text();
		Log.d("tag", " the value is ===> "+value);
		baseApi.clear();
		baseApi.end();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
