
package com.juanvvc.samegame;

import android.util.Log;

/** Use this class instead of android.util.Log: simplify the process of uploading to Google Play
 * @author juanvi
 */
public class myLog{
	public static final boolean DEBUG=false;
	public static void i(String tag, String msg){
		if(DEBUG) Log.i(tag, msg);
	}
	public static void d(String tag, String msg){
		if(DEBUG) Log.d(tag, msg);
	}
	public static void v(String tag, String msg){
		if(DEBUG) Log.v(tag, msg);
	}
	public static void e(String tag, String msg){
		if(DEBUG) Log.e(tag, msg);
	}
	public static void w(String tag, String msg){
		if(DEBUG) Log.e(tag, msg);
	}
}