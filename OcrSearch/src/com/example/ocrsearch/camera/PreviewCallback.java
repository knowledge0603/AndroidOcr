/*
 * Copyright (C) 2010 ZXing authors
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

package com.example.ocrsearch.camera;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class PreviewCallback implements Camera.PreviewCallback {

  private static final String TAG = PreviewCallback.class.getSimpleName();

  private final CameraConfigurationManager configManager;
  private Handler previewHandler;
  private int previewMessage;

  PreviewCallback(CameraConfigurationManager configManager) {
    this.configManager = configManager;
  }

  void setHandler(Handler previewHandler, int previewMessage) {
    this.previewHandler = previewHandler;
    this.previewMessage = previewMessage;
  }

  @Override
  public void onPreviewFrame(byte[] data, Camera camera) {
	  if (data != null)
      {
          Camera.Parameters parameters = camera.getParameters();
          int imageFormat = parameters.getPreviewFormat();
          Log.i("map", "Image Format: " + imageFormat+" ImageFormat.NV21 is "+ImageFormat.NV21);

          Log.i("CameraPreviewCallback", "data length:" + data.length);
          if (imageFormat == ImageFormat.NV21)
          {
        	  Log.i("map", "Image Format: " + imageFormat);
              // get full picture
              Bitmap image = null;
              int w = parameters.getPreviewSize().width;
              int h = parameters.getPreviewSize().height;
                
              Rect rect = new Rect(0, 0, w, h); 
              YuvImage img = new YuvImage(data, ImageFormat.NV21, w, h, null);
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              if (img.compressToJpeg(rect, 100, baos)) 
              { 
                  image =  BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
//                  imageView.setImageBitmap(image);
                  if(image==null)
                  {
                	  
                  }
                  
              }
      
          }
      }
	  
	  
    Point cameraResolution = configManager.getCameraResolution();
    Handler thePreviewHandler = previewHandler;
    if (cameraResolution != null && thePreviewHandler != null) {
      Message message = thePreviewHandler.obtainMessage(previewMessage, cameraResolution.x,
          cameraResolution.y, data);
      message.sendToTarget();
      previewHandler = null;
      Log.d(TAG, "send the data ..... ok ");
    } else {
      Log.d(TAG, "Got preview callback, but no handler or resolution available");
    }
  }

}
