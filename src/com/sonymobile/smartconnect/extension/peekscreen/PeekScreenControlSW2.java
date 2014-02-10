/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2011-2013, Sony Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB / Sony Mobile
 Communications AB nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonymobile.smartconnect.extension.peekscreen;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlView;
import com.sonyericsson.extras.liveware.extension.util.control.ControlViewGroup;
import com.sonyericsson.extras.liveware.extension.util.control.ControlView.OnClickListener;
import com.sonymobile.smartconnect.extension.peekscreen.R;


class PeekScreenControlSW2 extends ControlExtension {

    private static final int ANIMATION_DELTA_MS = 500;
    private static final int MENU_ITEM_0 = 0;
    private static final int MENU_ITEM_1 = 1;
    
    private Handler mHandler;

    private boolean mIsShowingAnimation = false;

    private Animation mAnimation = null;

    private ControlViewGroup mLayout = null;
    Bundle[] mMenuItemsText = new Bundle[2];
    
    private Bitmap mRotateBitmap = null;

	public PowerManager pm;
	public PowerManager.WakeLock wl ; 
	public KeyguardManager km;
	public Process sh;
	public OutputStream os;
	public boolean isReady=true;
	// Offset values for screenshot positioning
	public int offsetX,offsetY;   

	// Gets FrameSize value from settings, If it is not set set 128
	int sizeW=660;
	int sizeH=176*(sizeW/220);

	int displayWidth=0;
    PeekScreenControlSW2(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;
        initializeMenus();
        
        WindowManager window = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE); 
        Display display = window.getDefaultDisplay();
        displayWidth = display.getWidth();
        
        setFrameSizeWidth(displayWidth);
    }
    private void setFrameSizeWidth(int w)
    {
    	sizeW=Math.max(10,Math.min(displayWidth, w));
    	sizeH=176*(sizeW/220);
    }
    boolean mTextMenu=false;
    private void toggleMenu() {
        showMenu(mMenuItemsText);
        mTextMenu = !mTextMenu;
    }
    @Override
    public void onMenuItemSelected(final int menuItem) {
        if (menuItem == MENU_ITEM_0) 
        {
        	// Zoom In
        	setFrameSizeWidth((int)(sizeW*0.75f));
        	mAnimation.updateAnimation();
        }
        if (menuItem == MENU_ITEM_1)
        {
        	// Zoom Out
        	setFrameSizeWidth((int)(sizeW*1.25f));
        	mAnimation.updateAnimation();
        }
    }
    private void initializeMenus() {
        mMenuItemsText[0] = new Bundle();
        mMenuItemsText[0].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_0);
        mMenuItemsText[0].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT, "Zoom In");
        mMenuItemsText[1] = new Bundle();
        mMenuItemsText[1].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_1);
        mMenuItemsText[1].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT, "Zoom Out");

    }

	@Override
	public void onStart() {
		pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
		km = (KeyguardManager) mContext.getSystemService(mContext.KEYGUARD_SERVICE);
	}
	
    /**
     * Get supported control width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
    }

    /**
     * Get supported control height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_height);
    }

    @Override
    public void onDestroy() {
        Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWatch onDestroy");
        mHandler = null;
    };


    @Override
    public void onResume() {
        Log.d(SampleExtensionService.LOG_TAG, "Starting animation");



        Bundle[] data = new Bundle[4];

        
        showLayout(R.layout.sample_control_2, data);

        if (!mIsShowingAnimation) {
            mIsShowingAnimation = true;
            mAnimation = new Animation();
            mAnimation.run();
        }
    }

    @Override
    public void onSwipe(int direction) {
        switch (direction) {
	        case Control.Intents.SWIPE_DIRECTION_DOWN:
	        	offsetY-=sizeH/2;
	        break;
	        case Control.Intents.SWIPE_DIRECTION_UP:
	        	offsetY+=sizeH/2;
	        break;
	        case Control.Intents.SWIPE_DIRECTION_LEFT:
	        	offsetX+=sizeW/2;
	        break;
	        case Control.Intents.SWIPE_DIRECTION_RIGHT:
	        	offsetX-=sizeW/2;
	        break;
        }
    }
    @Override
    public void onObjectClick(final ControlObjectClickEvent event) {
        Log.d(SampleExtensionService.LOG_TAG, "onObjectClick() " + event.getClickType());
        if (event.getLayoutReference() != -1) {
            mLayout.onClick(event.getLayoutReference());
        }
    }

    @Override
    public void onKey(final int action, final int keyCode, final long timeStamp) {
        Log.d(SampleExtensionService.LOG_TAG, "onKey()");
        if (action == Control.Intents.KEY_ACTION_RELEASE
                && keyCode == Control.KeyCodes.KEYCODE_OPTIONS) {
            toggleMenu();
        }
        else if (action == Control.Intents.KEY_ACTION_RELEASE
                && keyCode == Control.KeyCodes.KEYCODE_BACK) {
            Log.d(SampleExtensionService.LOG_TAG, "onKey() - back button intercepted.");
        }
    }

    /**
     * The animation class shows an animation on the accessory. The animation
     * runs until mHandler.removeCallbacks has been called.
     */
    private class Animation implements Runnable {

        private int mIndex = 1;
        private boolean mIsStopped = false;

        /**
         * Create animation.
         */
        Animation() {
            mIndex = 1;
        }

        /**
         * Stop the animation.
         */
        public void stop() {
            mIsStopped = true;
        }

        @Override
        public void run() {

            updateAnimation();
            if (mHandler != null && !mIsStopped) {
                mHandler.postDelayed(this, ANIMATION_DELTA_MS);
            }
        }

        /**
         * Update the animation on the accessory. Only updates the part of the
         * screen which contains the animation.
         *
         * @param resourceId The new resource to show.
         */
        private void updateAnimation() {
    		if(!isReady) {return;}

    		// Locks screenshot process until finish
    		isReady=false;

    		// Unlocks keylock & turn on backlight to take screenshot of current app.
    		
    		if ((wl != null) && (wl.isHeld() == false)) { 
    			 wl.acquire();
    			 km.newKeyguardLock("unlock1").disableKeyguard();
    		}
    		
    		// Takes screenshot and saves it
    		 try {
    			sh = Runtime.getRuntime().exec("su", null,null);
 				os = sh.getOutputStream();
    	        os.write(("/system/bin/screencap -p " + Environment.getExternalStorageDirectory()+ File.separator +"img.png").getBytes("ASCII"));
    	        os.flush();
    	        
    	        os.close();
    	        try {
					sh.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	        
    		} catch (IOException e) {
    			Log.d(SampleExtensionService.LOG_TAG, "didnt do it1");
    			e.printStackTrace();
    		}	
    			
    		 BitmapFactory.Options bfOptions=new BitmapFactory.Options();
    		 bfOptions.inDither=false;                     //Disable Dithering mode
    		 bfOptions.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
    		 bfOptions.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
    		 bfOptions.inTempStorage=new byte[32 * 1024]; 
    		 // Loads screenshot as bitmap
    		mRotateBitmap=BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ File.separator +"img.png",bfOptions);
    		if(mRotateBitmap==null) {return;}
    		
    		// Makes sure offset is not smaller than the lowest pixel position possible.
    		offsetY=Math.max(0, offsetY);
    		offsetX=Math.max(0, offsetX);
    		
 
    		// If cropped screenshot hits edges of screen, stop it moving further
    		if(offsetX+sizeW > mRotateBitmap.getWidth())
    		{
    			offsetX=mRotateBitmap.getWidth()-sizeW;
    		}
    		if(offsetY+sizeW > mRotateBitmap.getHeight())
    		{
    			offsetY=mRotateBitmap.getHeight()-sizeW;
    		}		
    		// Crop image with final values
    		mRotateBitmap=Bitmap.createBitmap(mRotateBitmap, offsetX,offsetY,sizeW, sizeH);
    		// Scale image by framesize
    		mRotateBitmap=Bitmap.createScaledBitmap(mRotateBitmap, 220, 176, false);
    		// Send Image
    		
            sendImage(R.id.animatedImage, mRotateBitmap);
            
    		isReady=true;
        }
    };

}
