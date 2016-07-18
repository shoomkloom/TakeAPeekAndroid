package com.takeapeek.capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sets up a listener to listen for noise level.
 */
public class AudioListener
{
    static private final Logger logger = LoggerFactory.getLogger(AudioListener.class);
    
	private boolean is_running = true;
	private int buffer_size = -1;
	private AudioRecord ar = null;
	private Thread thread = null;

	public static interface AudioListenerCallback
    {
		public abstract void onAudio(int level);
	}

	/** Create a new AudioListener. The caller should call the start() method to start listening.
	 */
	public AudioListener(final AudioListenerCallback cb)
    {
        logger.debug("AudioListener(.) Invoked.");
        
		final int sample_rate = 8000;
		int channel_config = AudioFormat.CHANNEL_IN_MONO;
		int audio_format = AudioFormat.ENCODING_PCM_16BIT;
		try
        {
			buffer_size = AudioRecord.getMinBufferSize(sample_rate, channel_config, audio_format);
			//buffer_size = -1; // test
			logger.info("buffer_size: " + buffer_size);

			if( buffer_size <= 0 )
            {
                if( buffer_size == AudioRecord.ERROR )
                {
                    Helper.Error(logger, "getMinBufferSize returned ERROR");
                }
                else if( buffer_size == AudioRecord.ERROR_BAD_VALUE )
                {
                    Helper.Error(logger, "getMinBufferSize returned ERROR_BAD_VALUE");
                }

				return;
			}

			ar = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, channel_config, audio_format, buffer_size);
		}
		catch(Exception e)
        {
			Helper.Error(logger, "failed to create audiorecord", e);
			return;
		}

		final short[] buffer = new short[buffer_size];
		ar.startRecording();

		this.thread = new Thread()
        {
			@Override
			public void run()
            {
				/*int sample_delay = (1000 * buffer_size) / sample_rate;
				if( MyDebug.LOG )
					Log.e(TAG, "sample_delay: " + sample_delay);*/

				while( is_running )
                {
					/*try
					{
						Thread.sleep(sample_delay);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}*/
					try
                    {
					    int n_read = ar.read(buffer, 0, buffer_size);
					    if( n_read > 0 )
                        {
						    int average_noise = 0;
						    int max_noise = 0;
						    for(int i=0;i<n_read;i++)
                            {
						    	int value = Math.abs(buffer[i]);
						    	average_noise += value;
						    	max_noise = Math.max(max_noise, value);
						    }
						    average_noise /= n_read;
							/*if( MyDebug.LOG )
							{
								logger.info("n_read: " + n_read);
								logger.info("average noise: " + average_noise);
								logger.info("max noise: " + max_noise);
							}*/
							cb.onAudio(average_noise);
					    }
					    else
                        {
                            logger.info("n_read: " + n_read);
                            if( n_read == AudioRecord.ERROR_INVALID_OPERATION )
                            {
                                Helper.Error(logger, "read returned ERROR_INVALID_OPERATION");
                            }
                            else if( n_read == AudioRecord.ERROR_BAD_VALUE )
                            {
                                Helper.Error(logger, "read returned ERROR_BAD_VALUE");
                            }
					    }
					}
					catch(Exception e)
                    {
						Helper.Error(logger, "failed to read from audiorecord", e);
					}
				}
				ar.release();
				ar = null;
			}
		};
		// n.b., not good practice to start threads in constructors, so we require the caller to call start() instead
	}

	/** Start listening.
	 */
	void start()
    {
        logger.debug("start() Invoked.");
        
		if( thread != null )
        {
			thread.start();
		}
	}
	
	/** Stop listening and release the resources.
	 */
	void release()
    {
        logger.debug("release() Invoked.");
        
		is_running = false;
		thread = null;
	}
	
	boolean hasAudioRecorder()
    {
        logger.debug("hasAudioRecorder() Invoked.");
        
		return ar != null;
	}
}
