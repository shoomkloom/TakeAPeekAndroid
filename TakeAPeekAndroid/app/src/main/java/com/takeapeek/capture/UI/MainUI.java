package com.takeapeek.capture.UI;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.capture.PreferenceKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This contains functionality related to the main UI.
 */
public class MainUI
{
    static private final Logger logger = LoggerFactory.getLogger(MainUI.class);

	private CaptureClipActivity main_activity = null;

	private boolean popup_view_is_open = false;
//@@    private PopupView popup_view = null;

    private int current_orientation = 0;
	private boolean ui_placement_right = true;

	private boolean immersive_mode = false;
    private boolean show_gui = true; // result of call to showGUI() - false means a "reduced" GUI is displayed, whilst taking photo or video

	public MainUI(CaptureClipActivity main_activity)
    {
        logger.debug("MainUI(.) Invoked.");

		this.main_activity = main_activity;
	}

    public void layoutUI()
    {
        logger.debug("layoutUI() Invoked.");

		//this.preview.updateUIPlacement();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
		String ui_placement = sharedPreferences.getString(PreferenceKeys.getUIPlacementPreferenceKey(), "ui_right");
    	// we cache the preference_ui_placement to save having to check it in the draw() method
		this.ui_placement_right = ui_placement.equals("ui_right");
		logger.info("ui_placement: " + ui_placement);
		// new code for orientation fixed to landscape	
		// the display orientation should be locked to landscape, but how many degrees is that?
	    int rotation = main_activity.getWindowManager().getDefaultDisplay().getRotation();
	    int degrees = 0;
	    switch (rotation)
        {
	    	case Surface.ROTATION_0: degrees = 0; break;
	        case Surface.ROTATION_90: degrees = 90; break;
	        case Surface.ROTATION_180: degrees = 180; break;
	        case Surface.ROTATION_270: degrees = 270; break;
    		default:
    			break;
	    }
	    // getRotation is anti-clockwise, but current_orientation is clockwise, so we add rather than subtract
	    // relative_orientation is clockwise from landscape-left
    	//int relative_orientation = (current_orientation + 360 - degrees) % 360;
    	int relative_orientation = (current_orientation + degrees) % 360;

        logger.info("    current_orientation = " + current_orientation);
        logger.info("    degrees = " + degrees);
        logger.info("    relative_orientation = " + relative_orientation);

		int ui_rotation = (360 - relative_orientation) % 360;
        if(main_activity.getPreview() != null)
        {
            main_activity.getPreview().setUIRotation(ui_rotation);
        }
		int align_left = RelativeLayout.ALIGN_LEFT;
		int align_right = RelativeLayout.ALIGN_RIGHT;
		//int align_top = RelativeLayout.ALIGN_TOP;
		//int align_bottom = RelativeLayout.ALIGN_BOTTOM;
		int left_of = RelativeLayout.LEFT_OF;
		int right_of = RelativeLayout.RIGHT_OF;
		int above = RelativeLayout.ABOVE;
		int below = RelativeLayout.BELOW;
		int align_parent_left = RelativeLayout.ALIGN_PARENT_LEFT;
		int align_parent_right = RelativeLayout.ALIGN_PARENT_RIGHT;
		int align_parent_top = RelativeLayout.ALIGN_PARENT_TOP;
		int align_parent_bottom = RelativeLayout.ALIGN_PARENT_BOTTOM;
		if( !ui_placement_right )
        {
			//align_top = RelativeLayout.ALIGN_BOTTOM;
			//align_bottom = RelativeLayout.ALIGN_TOP;
			above = RelativeLayout.BELOW;
			below = RelativeLayout.ABOVE;
			align_parent_top = RelativeLayout.ALIGN_PARENT_BOTTOM;
			align_parent_bottom = RelativeLayout.ALIGN_PARENT_TOP;
		}

/*@@
        {
			// we use a dummy button, so that the GUI buttons keep their positioning even if the Settings button is hidden (visibility set to View.GONE)
			View view = main_activity.findViewById(R.id.gui_anchor);
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, 0);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);

			view = main_activity.findViewById(R.id.popup);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			//@@layoutParams.addRule(left_of, R.id.gallery);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.exposure_lock);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.popup);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.exposure);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.exposure_lock);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.switch_video);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.exposure);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.switch_camera);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, 0);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.switch_video);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.audio_control);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, 0);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.switch_camera);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.trash);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.audio_control);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);
	
			view = main_activity.findViewById(R.id.share);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, R.id.trash);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);

			view = main_activity.findViewById(R.id.take_photo);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
			view.setRotation(ui_rotation);

			view = main_activity.findViewById(R.id.zoom);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_top, 0);
			layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
			view.setRotation(180.0f); // should always match the zoom_seekbar, so that zoom in and out are in the same directions

			view = main_activity.findViewById(R.id.focus_seekbar);
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_left, R.id.preview);
			layoutParams.addRule(align_right, 0);
			layoutParams.addRule(left_of, R.id.zoom_seekbar);
			layoutParams.addRule(right_of, 0);
			layoutParams.addRule(align_parent_top, 0);
			layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
		}
@@*/

        {
			// set seekbar info
			int width_dp = 0;
			if( ui_rotation == 0 || ui_rotation == 180 )
            {
				width_dp = 300;
			}
			else
            {
				width_dp = 200;
			}
			int height_dp = 50;
			final float scale = main_activity.getResources().getDisplayMetrics().density;
			int width_pixels = (int) (width_dp * scale + 0.5f); // convert dps to pixels
			int height_pixels = (int) (height_dp * scale + 0.5f); // convert dps to pixels

/*@@
			View view = main_activity.findViewById(R.id.exposure_seekbar);
			view.setRotation(ui_rotation);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);

			view = main_activity.findViewById(R.id.exposure_seekbar_zoom);
			view.setRotation(ui_rotation);
			view.setAlpha(0.5f);

			// n.b., using left_of etc doesn't work properly when using rotation (as the amount of space reserved is based on the UI elements before being rotated)
			if( ui_rotation == 0 )
            {
				view.setTranslationX(0);
				view.setTranslationY(height_pixels);
			}
			else if( ui_rotation == 90 )
            {
				view.setTranslationX(-height_pixels);
				view.setTranslationY(0);
			}
			else if( ui_rotation == 180 )
            {
				view.setTranslationX(0);
				view.setTranslationY(-height_pixels);
			}
			else if( ui_rotation == 270 )
            {
				view.setTranslationX(height_pixels);
				view.setTranslationY(0);
			}

			view = main_activity.findViewById(R.id.iso_seekbar);
			view.setRotation(ui_rotation);
			lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);

			view = main_activity.findViewById(R.id.exposure_time_seekbar);
			view.setRotation(ui_rotation);
			lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);
			if( ui_rotation == 0 )
            {
				view.setTranslationX(0);
				view.setTranslationY(height_pixels);
			}
			else if( ui_rotation == 90 )
            {
				view.setTranslationX(-height_pixels);
				view.setTranslationY(0);
			}
			else if( ui_rotation == 180 )
            {
				view.setTranslationX(0);
				view.setTranslationY(-height_pixels);
			}
			else if( ui_rotation == 270 )
            {
				view.setTranslationX(height_pixels);
				view.setTranslationY(0);
			}
@@*/
		}

		//@@setTakePhotoIcon();
		// no need to call setSwitchCameraContentDescription()
    }

    /** Set icon for taking photos vs videos.
	 *  Also handles content descriptions for the take photo button and switch video button.
     */
/*@@
    public void setTakePhotoIcon()
    {
        logger.debug("setTakePhotoIcon() Invoked.");

		if( main_activity.getPreview() != null )
        {
			ImageButton view = (ImageButton)main_activity.findViewById(R.id.take_photo);
			int resource = 0;
			int content_description = 0;
			int switch_video_content_description = 0;
			if( main_activity.getPreview().isVideo() )
            {
				logger.info("set icon to video");
			}
			else
            {
				logger.info("set icon to photo");
			}
		}
    }
@@*/

    public boolean getUIPlacementRight()
    {
        logger.debug("getUIPlacementRight() Invoked.");

        return this.ui_placement_right;
    }

    public void onOrientationChanged(int orientation)
    {
        logger.debug("onOrientationChanged(.) Invoked.");

		if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN )
        {
            return;
        }
		int diff = Math.abs(orientation - current_orientation);
		if( diff > 180 )
        {
            diff = 360 - diff;
        }
		// only change orientation when sufficiently changed
		if( diff > 60 )
        {
		    orientation = (orientation + 45) / 90 * 90;
		    orientation = orientation % 360;
		    if( orientation != current_orientation )
            {
			    this.current_orientation = orientation;

				logger.info("current_orientation is now: " + current_orientation);

				layoutUI();
		    }
		}
	}

    public void setImmersiveMode(final boolean immersive_mode)
    {
        logger.debug("setImmersiveMode(.) Invoked.");

		logger.info("setImmersiveMode: " + immersive_mode);
    	this.immersive_mode = immersive_mode;
		main_activity.runOnUiThread(new Runnable()
        {
			public void run()
            {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
				// if going into immersive mode, the we should set GONE the ones that are set GONE in showGUI(false)
		    	//final int visibility_gone = immersive_mode ? View.GONE : View.VISIBLE;
		    	final int visibility = immersive_mode ? View.GONE : View.VISIBLE;
				logger.info("setImmersiveMode: set visibility: " + visibility);
		    	// n.b., don't hide share and trash buttons, as they require immediate user input for us to continue

                /*@@
                View switchCameraButton = (View) main_activity.findViewById(R.id.switch_camera);
			    View switchVideoButton = (View) main_activity.findViewById(R.id.switch_video);
			    View exposureButton = (View) main_activity.findViewById(R.id.exposure);
			    View exposureLockButton = (View) main_activity.findViewById(R.id.exposure_lock);
			    View audioControlButton = (View) main_activity.findViewById(R.id.audio_control);
			    View popupButton = (View) main_activity.findViewById(R.id.popup);
			    @@*/

			    //@@View galleryButton = (View) main_activity.findViewById(R.id.gallery);
			    //@@View settingsButton = (View) main_activity.findViewById(R.id.settings);
			    //@@View zoomControls = (View) main_activity.findViewById(R.id.zoom);
			    View zoomSeekBar = (View) main_activity.findViewById(R.id.zoom_seekbar);

                /*@@
			    if( main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1 )
			    	switchCameraButton.setVisibility(visibility);
		    	switchVideoButton.setVisibility(View.INVISIBLE);
			    if( main_activity.supportsExposureButton() )
			    	exposureButton.setVisibility(visibility);
			    if( main_activity.getPreview().supportsExposureLock() )
			    	exposureLockButton.setVisibility(View.INVISIBLE);
			    if( main_activity.hasAudioControl() )
			    	audioControlButton.setVisibility(View.INVISIBLE);
		    	popupButton.setVisibility(visibility);
                @@*/

				logger.info("has_zoom: " + main_activity.getPreview().supportsZoom());

/*@@
				if( main_activity.getPreview().supportsZoom() && sharedPreferences.getBoolean(PreferenceKeys.getShowZoomControlsPreferenceKey(), false) )
                {
					zoomControls.setVisibility(visibility);
				}
@@*/
				if( main_activity.getPreview().supportsZoom() && sharedPreferences.getBoolean(PreferenceKeys.getShowZoomSliderControlsPreferenceKey(), true) )
                {
					zoomSeekBar.setVisibility(visibility);
				}
        		String pref_immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");

/*@@
        		if( pref_immersive_mode.equals("immersive_mode_everything") )
                {
    			    View takePhotoButton = (View) main_activity.findViewById(R.id.take_photo);
    			    takePhotoButton.setVisibility(View.INVISIBLE);
        		}
@@*/
				if( !immersive_mode )
                {
					// make sure the GUI is set up as expected
					showGUI(show_gui);
				}
			}
		});
    }
    
    public boolean inImmersiveMode()
    {
        logger.debug("inImmersiveMode() Invoked.");

        return immersive_mode;
    }

    public void showGUI(final boolean show)
    {
        logger.debug("showGUI(.) Invoked.");

		logger.info("showGUI: " + show);
		this.show_gui = show;
		if( inImmersiveMode() )
        {
            return;
        }

/*@@
		if( show && main_activity.usingKitKatImmersiveMode() )
        {
			// call to reset the timer
			main_activity.initImmersiveMode();
		}
@@*/

        main_activity.runOnUiThread(new Runnable()
        {
			public void run()
            {
                logger.debug("runOnUiThread:run() Invoked.");

		    	final int visibility = show ? View.VISIBLE : View.GONE;

/*@@
			    View switchCameraButton = (View) main_activity.findViewById(R.id.switch_camera);
			    View switchVideoButton = (View) main_activity.findViewById(R.id.switch_video);
			    View exposureButton = (View) main_activity.findViewById(R.id.exposure);
			    View exposureLockButton = (View) main_activity.findViewById(R.id.exposure_lock);
			    View audioControlButton = (View) main_activity.findViewById(R.id.audio_control);
			    View popupButton = (View) main_activity.findViewById(R.id.popup);
			    if( main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1 )
                {
                    switchCameraButton.setVisibility(View.INVISIBLE);
                }
			    if( !main_activity.getPreview().isVideo() )
                {
                    switchVideoButton.setVisibility(View.INVISIBLE); // still allow switch video when recording video
                }
			    if( main_activity.supportsExposureButton() && !main_activity.getPreview().isVideo() ) // still allow exposure when recording video
                {
                    exposureButton.setVisibility(View.INVISIBLE);
                }
			    if( main_activity.getPreview().supportsExposureLock() && !main_activity.getPreview().isVideo() ) // still allow exposure lock when recording video
                {
                    exposureLockButton.setVisibility(View.INVISIBLE);
                }
			    if( main_activity.hasAudioControl() )
                {
                    audioControlButton.setVisibility(View.INVISIBLE);
                }
			    if( !show )
                {
			    	closePopup(); // we still allow the popup when recording video, but need to update the UI (so it only shows flash options), so easiest to just close
			    }
			    if( !main_activity.getPreview().isVideo() || !main_activity.getPreview().supportsFlash() )
                {
                    popupButton.setVisibility(View.INVISIBLE); // still allow popup in order to change flash mode when recording video
                }
@@*/
			}
		});
    }

/*@@
    public void audioControlStarted()
    {
        logger.debug("audioControlStarted() Invoked.");

		ImageButton view = (ImageButton)main_activity.findViewById(R.id.audio_control);
		//@@view.setImageResource(R.drawable.ic_mic_red_48dp);
		//@@view.setContentDescription( main_activity.getResources().getString(R.string.audio_control_stop) );
    }

    public void audioControlStopped()
    {
        logger.debug("audioControlStopped() Invoked.");

		ImageButton view = (ImageButton)main_activity.findViewById(R.id.audio_control);
		//@@view.setImageResource(R.drawable.ic_mic_white_48dp);
		//@@view.setContentDescription( main_activity.getResources().getString(R.string.audio_control_start) );
    }

    public void toggleExposureUI()
    {
        logger.debug("toggleExposureUI() Invoked.");

		closePopup();
		SeekBar exposure_seek_bar = ((SeekBar)main_activity.findViewById(R.id.exposure_seekbar));
		int exposure_visibility = exposure_seek_bar.getVisibility();
		SeekBar iso_seek_bar = ((SeekBar)main_activity.findViewById(R.id.iso_seekbar));
		int iso_visibility = iso_seek_bar.getVisibility();
		SeekBar exposure_time_seek_bar = ((SeekBar)main_activity.findViewById(R.id.exposure_time_seekbar));
		int exposure_time_visibility = iso_seek_bar.getVisibility();
		boolean is_open = exposure_visibility == View.VISIBLE || iso_visibility == View.VISIBLE || exposure_time_visibility == View.VISIBLE;
		if( is_open )
        {
			clearSeekBar();
		}
		else if( main_activity.getPreview().getCameraController() != null )
        {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
			String value = sharedPreferences.getString(PreferenceKeys.getISOPreferenceKey(), main_activity.getPreview().getCameraController().getDefaultISO());
			if( main_activity.getPreview().usingCamera2API() && !value.equals(main_activity.getPreview().getCameraController().getDefaultISO()) )
            {
				// with Camera2 API, when using non-default ISO we instead show sliders for ISO range and exposure time
				if( main_activity.getPreview().supportsISORange())
                {
					iso_seek_bar.setVisibility(View.VISIBLE);
					if( main_activity.getPreview().supportsExposureTime() )
                    {
						exposure_time_seek_bar.setVisibility(View.VISIBLE);
					}
				}
			}
			else
            {
				if( main_activity.getPreview().supportsExposures() )
                {
					exposure_seek_bar.setVisibility(View.VISIBLE);
					ZoomControls seek_bar_zoom = (ZoomControls)main_activity.findViewById(R.id.exposure_seekbar_zoom);
					seek_bar_zoom.setVisibility(View.VISIBLE);
				}
			}
		}
    }
@@*/

	public void setSeekbarZoom()
    {
        logger.debug("setSeekbarZoom() Invoked.");

	    SeekBar zoomSeekBar = (SeekBar) main_activity.findViewById(R.id.zoom_seekbar);
		zoomSeekBar.setProgress(main_activity.getPreview().getMaxZoom()-main_activity.getPreview().getCameraController().getZoom());

		logger.info("progress is now: " + zoomSeekBar.getProgress());
	}
	
	public void changeSeekbar(int seekBarId, int change)
    {
        logger.debug("changeSeekbar(..) Invoked.");

		logger.info("changeSeekbar: " + change);
		SeekBar seekBar = (SeekBar)main_activity.findViewById(seekBarId);
	    int value = seekBar.getProgress();
	    int new_value = value + change;
	    if( new_value < 0 )
        {
            new_value = 0;
        }
	    else if( new_value > seekBar.getMax() )
        {
            new_value = seekBar.getMax();
        }

        logger.info("value: " + value);
        logger.info("new_value: " + new_value);
        logger.info("max: " + seekBar.getMax());

	    if( new_value != value )
        {
		    seekBar.setProgress(new_value);
	    }
	}

/*@@
    public void clearSeekBar()
    {
        logger.debug("clearSeekBar() Invoked.");

		View view = main_activity.findViewById(R.id.exposure_seekbar);
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(R.id.iso_seekbar);
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(R.id.exposure_time_seekbar);
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(R.id.exposure_seekbar_zoom);
		view.setVisibility(View.GONE);
    }

    public void setPopupIcon()
    {
        logger.debug("setPopupIcon() Invoked.");

		ImageButton popup = (ImageButton)main_activity.findViewById(R.id.popup);
		String flash_value = main_activity.getPreview().getCurrentFlashValue();
		logger.info("flash_value: " + flash_value);

        if( flash_value != null && flash_value.equals("flash_off") )
        {
    		//@@popup.setImageResource(R.drawable.popup_flash_off);
    	}
    	else if( flash_value != null && flash_value.equals("flash_torch") )
        {
    		//@@popup.setImageResource(R.drawable.popup_flash_torch);
    	}
		else if( flash_value != null && flash_value.equals("flash_auto") )
        {
    		//@@popup.setImageResource(R.drawable.popup_flash_auto);
    	}
    	else if( flash_value != null && flash_value.equals("flash_on") )
        {
    		//@@popup.setImageResource(R.drawable.popup_flash_on);
    	}
    	else if( flash_value != null && flash_value.equals("flash_red_eye") )
        {
    		//@@popup.setImageResource(R.drawable.popup_flash_red_eye);
    	}
    	else
        {
    		//@@popup.setImageResource(R.drawable.popup);
    	}
    }

    public void closePopup()
    {
        logger.debug("closePopup() Invoked.");

		if( popupIsOpen() )
		{
			ViewGroup popup_container = (ViewGroup)main_activity.findViewById(R.id.popup_container);
			popup_container.removeAllViews();
			popup_view_is_open = false;
			/* Not destroying the popup doesn't really gain any performance.
			 * Also there are still outstanding bugs to fix if we wanted to do this:
			 *   - Not resetting the popup menu when switching between photo and video mode. See test testVideoPopup().
			 *   - When changing options like flash/focus, the new option isn't selected when reopening the popup menu. See test
			 *     testPopup().
			 *   - Changing settings potentially means we have to recreate the popup, so the natural place to do this is in
			 *     MainActivity.updateForSettings(), but doing so makes the popup close when checking photo or video resolutions!
			 *     See test testSwitchResolution().
			 /
			destroyPopup();
			main_activity.initImmersiveMode(); // to reset the timer when closing the popup
		}
    }


    public boolean popupIsOpen()
    {
        logger.debug("popupIsOpen() Invoked.");

        return popup_view_is_open;
    }
    
    public void destroyPopup()
    {
        logger.debug("destroyPopup() Invoked.");

        if( popupIsOpen() )
        {
			closePopup();
		}
//@@		popup_view = null;
    }
@@*/

    public void togglePopupSettings()
    {
        logger.debug("togglePopupSettings() Invoked.");

		if( main_activity.getPreview().getCameraController() == null )
        {
			logger.info("camera not opened!");
			return;
		}

		logger.info("open popup");
    }
}
