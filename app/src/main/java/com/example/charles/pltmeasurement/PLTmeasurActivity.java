package com.example.charles.pltmeasurement;

import android.graphics.Bitmap;
import android.net.Uri;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.os.Handler;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class PLTmeasurActivity extends AppCompatActivity {

    private static final String TAG = "PLTmeasure";
    private static final String DIR ="/sdcard/PLT";
    private static final int TIMEOUT_COUNTER = 30000;  // Stop loading more results after 30 second until the last website is loaded
    private static final int LOADING_INTERVAL = 5000;
    private static final ArrayList<String> urlList = new ArrayList<String>();
    private static final String js_forNT = "javascript:(\n function() {\n"
            + "setTimeout(function(){;\n"
            + "var result = 'PLTmeasure:';\n"
            + "var perfOBJ = performance.timing;\n"
            + "for (var prop in perfOBJ){\n"
            + "result += prop + ':' + perfOBJ[prop] + ';'};\n"
            + "console.log(result)}, 2000);\n"
            + " })()\n";
    private static boolean TIMEOUT = false;  // To see if expired
    private static HashMap<String, String> measurementResults = new HashMap<String, String>();
    private static String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.3; en-us; SCH-I535 Build/JSS15J)" +
            " AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    // For mode 1: fetch a large number of websites
    private int currentUrlIndex = 0;
    private String currentHandlingUrl = "";

    // For mode 2: fetch a single webpage for a large number of times
    private static final int REPEAT = 2;
    private int currentTimes = 0;

    private boolean readUrlFromFile(String urllist) {
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(getAssets().open(urllist)));
            String mLine;
            while ((mLine = inputReader.readLine()) != null) {
                if (mLine.isEmpty())
                    continue;
                // If start with www, add protocol
                if (mLine.startsWith("www"))
                    mLine = "http://" + mLine;
                // If not start with www, nor does it start with http, add http://www
                else if (!mLine.startsWith("http"))
                    mLine = "http://www." + mLine;
                urlList.add(mLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to open website list because " + e.getMessage());
            return false;
        }
        return (urlList.size() > 0);
    }

    private String parseUrl(String url) {
        try {
            URL url_parsed = new URL(url);
            return url_parsed.getHost() + url_parsed.getPath();
        } catch (MalformedURLException e) {
            Log.e(TAG, "Cannot parse URL " + url);
            return url;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Read all URLs
        if (!readUrlFromFile("website")) {
            return;
        }

        File resultDir = new File(DIR);

        if (resultDir.exists()) {
            resultDir.delete();
        }

        try {
            if (!resultDir.mkdir())
                Log.e(TAG, "Cannot create result folder!");
        } catch (Exception e) {
            Log.e(TAG, "Cannot create result folder!, because " + e.getMessage());
        }

        if (REPEAT > 0) {
            if (urlList.size() == 1) {
                String url = urlList.get(0);
                for (int i = 0; i < (REPEAT - 1); i++) {
                    urlList.add(url);
                }
            }
        }

        final Button start = (Button) findViewById(R.id.buttonStart);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final WebView mywebview = (WebView) findViewById(R.id.myWebView);
                // Set up webview client
                mywebview.setWebViewClient(new WebViewClient() {

                    Boolean loadingFinished = true;
                    Boolean redirect = false;

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d(TAG, url + " shouldOverrideUrlLoading!!!");
/*                        if (!loadingFinished) {
                            redirect = true;
                        }
                        loadingFinished = false;*/
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        //view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        //super.onPageStarted(view, url, favicon);
                        Log.d(TAG, url + " onPageStarted!!!");
                        //loadingFinished = false;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //super.onPageFinished(view, url);
                        Log.d(TAG, url + " onPageFinished!!!");
                        view.loadUrl(js_forNT);
/*                        if (!redirect) {
                            loadingFinished = true;
                        }
                        if (loadingFinished && !redirect) {
                            //Log.d(TAG, url + " Page loading finish !!!");
                            view.loadUrl(js_forNT);
                        } else {
                            redirect = false;
                        }*/
                    }
                });

                // set up webchromeclient
                mywebview.setWebChromeClient(new WebChromeClient() {
/*                    private boolean handleMessage(String message) {
                        if (message.startsWith("PLTresults")) {
                            message = message.substring(11, message.length());
                            String[] separated = message.split(",");
                            try {
                                double ttfb = Double.parseDouble(separated[0]);
                                double prt = Double.parseDouble(separated[1]);
                                double plt = Double.parseDouble(separated[2]);
                                if (ttfb < 0) ttfb = 0;
                                if (prt < 0) prt = 0;
                                if (plt < 0) plt = 0;
                                // Otherwise no valid data is collected
                                message = Double.toString(ttfb) + " " + Double.toString(prt) + " " +
                                        Double.toString(plt);
                                if (REPEAT == 0)
                                    measurementResults.put(currentHandlingUrl, message);
                                else
                                    measurementResults.put(Integer.toString(currentUrlIndex), message);
                                return true;
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Cannot parse " + message);
                                return false;
                            }
                        }
                        return false;
                    }*/

                    private void saveNewResults(String[] allPairs) {
                        String url = mywebview.getUrl();
                        String[] urlPair = allPairs[0].split(":");
                        String newRecord = "";
                        for (int i = 1; i < allPairs.length; i++) {
                            newRecord += allPairs[i].split(":")[1] + " ";
                        }
                        try {
                            Log.d(TAG, DIR + "/" + url);
                            File resultFile = new File(DIR + "/" + currentUrlIndex);
                            if (!resultFile.exists()) {
                                if (!resultFile.createNewFile()) {
                                    Log.e(TAG, "Cannot create result file for " + url);
                                    return;
                                }
                            }
                            // Otherwise, append new result under existing one
                            FileOutputStream fs = new FileOutputStream(resultFile, true);
                            fs.write(newRecord.getBytes());
                            Log.d(TAG, "Save new record for " + url);
                            fs.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Fail to save results, because: " + e.getMessage());
                        }
                    }

/*                    private void saveResults() {
                        String finalResults = "";
                        Log.d(TAG, "--------------Results summary-------------");
                        for (String url : measurementResults.keySet()) {
                            String newResult = url + " " + measurementResults.get(url);
                            Log.d(TAG, newResult);
                            finalResults += (newResult + '\n');
                        }
                        Log.d(TAG, "----------------Summary end---------------");
                        try {
                            File resultFile = new File("/sdcard/PLTresults.txt");
                            if (!resultFile.exists()) {
                                resultFile.createNewFile();
                            } else {
                                resultFile.delete();
                            }
                            FileOutputStream fs = new FileOutputStream(resultFile);
                            fs.write(finalResults.getBytes());
                            fs.close();
                        } catch (Exception e) {
                            Log.e(TAG, "Fail to save results, because: " + e.getMessage());
                        }
                    }*/

                    private void loadNext() {
                        Handler handler = new Handler();
                        //mywebview.clearCache(true);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if ((currentUrlIndex + 1) < Math.max(urlList.size(), REPEAT)) {
                                    currentUrlIndex += 1;
                                    currentHandlingUrl = urlList.get(currentUrlIndex);
                                    Log.d(TAG, (currentUrlIndex + 1) + "'s url: " + currentHandlingUrl + " Total: " + urlList.size());
                                    mywebview.loadUrl(currentHandlingUrl);
                                } else {
                                    if (!TIMEOUT) {
                                        TIMEOUT = true;
                                        //saveResults();
                                    }
                                }
                            }
                        }, LOADING_INTERVAL);
                    }

                    @Override
                    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                        Log.d(TAG, message);
                        if (message.startsWith(TAG)) {
                            message = message.substring((TAG.length() + 1), message.length());
                            String[] allPairs = message.split(";");
                            try {
                                String[] loadEventEndPair = allPairs[0].split(":");
                                long loadEventEnd = Long.parseLong(loadEventEndPair[1]);
                                // How to decide if (Time out or not?)
                                if (loadEventEnd > 0) {
                                    saveNewResults(allPairs);
                                    loadNext();
                                } else {
                                    // wait 0.5s and run js again

/*                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mywebview.loadUrl(js_forNT);
                                        }
                                    }, 500);*/
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Cannot parse message:" + message);
                            }
                        }




/*                        if (!TIMEOUT) {
                            String actualURL = mywebview.getUrl();
                            if (REPEAT == 0) {
                                if ((measurementResults.get(currentHandlingUrl) == null) && handleMessage(message))
                                    loadNext();
                            }
                            else {
                                if (measurementResults.get(Integer.toString(currentUrlIndex)) != null)
                                    handleMessage(message);
                                else
                                {
                                    if (handleMessage(message))
                                        loadNext();
                                }
                            }
                        }*/
                    }
                });

                // Set web setting
                WebSettings webSettings = mywebview.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAppCacheEnabled(false);
                webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                //webSettings.setUserAgentString(USER_AGENT);
                currentHandlingUrl = urlList.get(currentUrlIndex);
                mywebview.loadUrl(currentHandlingUrl);
                Log.d(TAG, (currentUrlIndex + 1) + "'s url: " + currentHandlingUrl + " Total: " + urlList.size());
            }
        });
    }
}
