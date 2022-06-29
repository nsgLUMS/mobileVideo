package com.example.signalcapturer;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class sendToServer extends JobService {
    int serverResponseCode = 0;
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        String filename = jobParameters.getExtras().getString("fileName");
        final String deviceConfig = jobParameters.getExtras().getString("deviceConfig");

        class OneShotTask implements Runnable {
            String filename;
            OneShotTask(String s) { filename = s; }
            public void run() {
                if (deviceConfig.equals("false")){
                    uploadFile("ENTER URI HERE" ,filename); // ENTER SERVER'S ADDRESS HERE
                } else {
                    uploadConfig("ENTER URI HERE" ,filename); // ENTER SERVER'S ADDRESS HERE
                }
            }
        }
        new Thread(new OneShotTask(filename)).start();

        return false;
    }
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public int uploadFile(String sourceFileUri, String uploadFileName) {
//        Log.e("Network","Sending to server");

        String fileName = uploadFileName;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(getExternalFilesDir(null) +  uploadFileName);

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist :"
                    +getExternalFilesDir(null) + "" + uploadFileName);

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(sourceFileUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name="+"uploaded_file"+";filename="
                        + fileName + "" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);



                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
                if(serverResponseCode == 200){
                    String path = this.getExternalFilesDir(null)+uploadFileName;
//                    System.out.println(new File(path).getAbsoluteFile().delete());
                    FileWriter fw = new FileWriter(path,false);
                    fw.close();
                }

            } catch (MalformedURLException ex) {


                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {


                Log.e("Upload", "Exception : "
                        + e.getMessage(), e);
            }
            return serverResponseCode;

        } // End else block
    }

    public int uploadConfig(String sourceFileUri, String uploadFileName) {
        Log.e("Network","Sending Config to server " + uploadFileName );

        String fileName = uploadFileName;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File( getExternalFilesDir(null) + uploadFileName);

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist :" + uploadFileName);

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(sourceFileUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name="+"uploaded_file"+";filename="
                        + fileName + "" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);



                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
//                if(serverResponseCode == 200){
//                    String path = this.getExternalFilesDir(null)+uploadFileName;
////                    System.out.println(new File(path).getAbsoluteFile().delete());
//                    FileWriter fw = new FileWriter(path,false);
//                    fw.close();
//                }

            } catch (MalformedURLException ex) {


                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {


                Log.e("Upload", "Exception : "
                        + e.getMessage(), e);
            }
            return serverResponseCode;

        } // End else block
    }
}