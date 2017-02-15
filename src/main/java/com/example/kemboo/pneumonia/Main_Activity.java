package com.example.kemboo.pneumonia;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main_Activity extends AppCompatActivity {

/*Setting instance variables for microphone methods*/

    private Button startBtn;
    private Button playBtn;
    private boolean recordbuttonstatus = true;
    private boolean playbuttonstatus = true;
    private static final String AUDIO_RECORDER_FILE = "Pneumonia_Audio.wav";
    private static final String APP_FOLDER = "Pneumonia";
    private static final String AUDIO_RECORDER_TEMP_FILE = "Pneumonia_Audio.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private AudioRecord audioRecord = null;
    private int bufferSize = 0;
    private static final String LOG_TAG = "Audio";


    /*Setting instance variables for accelerometer methods*/

    private TextView currentX, currentY, currentZ;
    private SensorManager sensorManager;
    private Sensor sensor;
    private FileWriter writer;
    private static final String LOG_TAG2 = "Accelerometer";
    private static final String ACCELEROMETERVALFILE = "Accelerometer.csv";
    private static final String ACCELEROMETERVALWAV = "Accelerometer.wav";
    private static final int SAMPLEDELAYMICROSEC = 20000;
    private File CSVfile;

    /*Setting time display variables*/

    private TextView currenttimedisplay;
    long start,time;


/*OnCreate is the first function the program will run upon starting the main activity. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        /*Setup buttons.*/

        startBtn = (Button) findViewById(R.id.start);
        playBtn = (Button) findViewById(R.id.play);

        /*Setup accelerometer.*/

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        /*Make Folder to store data.*/

        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, APP_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        /*Setup accelerometer labelling*/

        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);

        /*Setup time display*/

        currenttimedisplay = (TextView)findViewById(R.id.timepassed);

        /*Set buffersize*/

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    /* The program will set up the accelerometer so that it will be ready to take and record
    information when the startBtn is pressed.
    *
    *
    * The first thing it will do is set a sensor event listener so the phone can collect data from
    * its accelerometer.*/

    SensorEventListener accelListener = new SensorEventListener() {

        /*Nothing will happen when the accuracy of the accelerometer changes.*/

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        /* As they are updated by the accelerometer listener, the x,y, and z accelerometer values
        will be translated to strings that will be shown on the app screen.
        Also a writer will record the accelerometer values to a file*/
        @Override
        public void onSensorChanged(SensorEvent event) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            time=SystemClock.uptimeMillis()-start;
            currenttimedisplay.setText(Long.toString(time));


            currentX.setText(Float.toString(x));
            currentY.setText(Float.toString(y));
            currentZ.setText(Float.toString(z));

            try {
                writer.write(time+ ","+ y + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG2, "Accelwriter failed");
            }
        }
    };

    /*When the app is running nothing special will happen.*/

    public void onResume() {
        super.onResume();
    }

    /* The accelerometer sensors will turn off when the app is off or idle. */

    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(accelListener);

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG2, "AccelWriter never on");
            }
        }
    }

    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(accelListener);

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG2, "AccelWriter never on");
            }
        }
    }


    /*The Recordbutton method is activated when the startBtn is pressed.  It starts recording audio
    and accelerometer values if it is pressed when non-active and stops recording audio and
    accelerometer info if it is pressed when active.  If it is the first time recording audio since
     the app is activated then it will also enable the play button, playBtn.
     */

    public void Recordbutton(View view) {

        if (recordbuttonstatus) {

            /* Checks and changes statuses of buttons. */

            playBtn.setEnabled(true);
            recordbuttonstatus = false;
            startBtn.setText(getString(R.string.stoprecording));

            /* This sets up the recorder.*/

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
            audioRecord.startRecording();

            Thread recordingThread = new Thread(new Runnable() {

                @Override

                /*This thread creates a raw audio file to the app folder while the microphone is
                collecting data.*/

                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();

            /* This sets up the timer and accelerometer. */

            time=0;
            start= SystemClock.uptimeMillis();

            Thread accelerometerrecord = new Thread(new Runnable() {

                @Override

                /*This thread will record the accelerometer values to a csv file in the app's folder.*/

                public void run() {

                    sensorManager.registerListener(accelListener, sensor, SAMPLEDELAYMICROSEC);
                    CSVfile = new File(Environment.getExternalStorageDirectory()+"/"+ APP_FOLDER, ACCELEROMETERVALFILE);
                    try {
                        CSVfile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG2, "Couldn't create accelvalfile");
                    }
                    try {
                        writer = new FileWriter(CSVfile, false); /*When false, the file is overwritten, when true, the file is appended.*/
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG2, "Writer failed to open");
                    }
                }
            });

            accelerometerrecord.start();


        } else {
            startBtn.setText(getString(R.string.newrecording));
            recordbuttonstatus = true;
            sensorManager.unregisterListener(accelListener);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try{
                writer.close();}
            catch (IOException e){
                Log.e(LOG_TAG2,"Accelwriter failed to close");
            }

            /*The raw audio file will be copied to a WAV file (which will be saved to the app's folder
             and subsequently deleted.*/

            copyWaveFile(getTempFilename(), getFilename());
            deleteTempFile();

            /*The accelerometer values will be placed in a 50 Hz WAV file*/
            CSVToWAV();
        }
    }

     /*The Playbutton method is activated when the playBtn is pressed.  It plays back the recorded
     audio if it is pressed when non-active and it stops playing the recorded audio if it is pressed
     when active.*/

    public void Playbutton(View view) {
        if (playbuttonstatus) {
            playBtn.setText(getString(R.string.stop));
            playbuttonstatus = false;

            Thread playThread = new Thread(new Runnable() {
                @Override

                /*This thread plays the WAV file that was stored to the app's folder.*/

                public void run() {
                    int minBufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);
                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, minBufferSize, AudioTrack.MODE_STREAM);
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();

                    int i;
                    byte[] s = new byte[bufferSize];
                    try {
                        FileInputStream fin = new FileInputStream(filepath + "/"+APP_FOLDER+"/"+AUDIO_RECORDER_FILE);
                        DataInputStream dis = new DataInputStream(fin);

                        audioTrack.play();
                        while((i = dis.read(s, 0, bufferSize)) > -1){
                            audioTrack.write(s, 0, i);
                            if (playbuttonstatus){
                                audioTrack.stop();
                                audioTrack.flush();
                                audioTrack.release();
                                dis.close();
                                fin.close();}
                        }
                        audioTrack.stop();
                        audioTrack.release();
                        dis.close();
                        fin.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Pneumonia Audio file not found");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Error with audiotrack");
                    }
                }
            });

            playThread.start();

        } else {
            playBtn.setText(getString(R.string.play));
            playbuttonstatus = true;
        }
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, APP_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,APP_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read;

        if(null != os){
            while(!recordbuttonstatus){
                read = audioRecord.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Couldn't write audio data");
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Fileoutputstream never on");
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        int bitspersample = 16;
        long byteRate = bitspersample * RECORDER_SAMPLERATE * channels/8;


        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate,bitspersample);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Pneumonia Audio file not found");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Writing WAV file failed");
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate, int bitspersample) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (1 * 16 / 8); // block align (num of channels * Bitspersample/8)
        header[33] = 0;
        header[34] = (byte) (bitspersample); // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
    public void CSVToWAV(){
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = 50;
        int channels = 1;
        int bitspersample = 8;
        long byteRate = bitspersample * 50 * channels / 8;
        try {
            FileInputStream csvStream = new FileInputStream(CSVfile);
            File accelwav = new File(Environment.getExternalStorageDirectory()+"/"+APP_FOLDER, ACCELEROMETERVALWAV);
            FileOutputStream towav = new FileOutputStream(accelwav);
            InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
            BufferedReader reader = new BufferedReader(csvStreamReader);
            String line;

            totalAudioLen = csvStream.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(towav, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate,bitspersample);

            while ((line = reader.readLine()) != null) {
                String[] lineData = line.split(",");
                Float y = Float.parseFloat(lineData[1])*1000;
                towav.write(y.byteValue());
                Log.i(lineData[1],Byte.toString(y.byteValue()));
            }

            csvStream.close();
            towav.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}