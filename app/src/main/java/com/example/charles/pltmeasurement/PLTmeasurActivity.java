package com.example.charles.pltmeasurement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
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

    private static final String TAG = "PLTmeasurement";
    private static final String DIR = Environment.getExternalStorageDirectory().getPath() + "/PLT";

    private static final int TIMEOUT_COUNTER = 30000;  // Stop loading more results after 30 second until the last website is loaded
    private static final int LOADING_INTERVAL = 5000;
    private static final ArrayList<String> urlList = new ArrayList<String>();
    private static final String js_forNT = "javascript:(\n function() { \n"
            + "setTimeout(function(){var source = window.location.href;\n"
            + "console.log('PLTresults' + ':' + source + ';');\n"
            + "var perfOBJ = performance.timing;\n"
            + "for (var prop in perfOBJ){\n"
            + "console.log(prop + ':' + perfOBJ[prop] + ';'); }}, 500);\n"
            + " })()\n";
    private static boolean TIMEOUT = false;  // To see if expired


    private static HashMap<String, String> measurementResults = new HashMap<String, String>();
    private static String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.3; en-us; SCH-I535 Build/JSS15J)" +
            " AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    // For mode 1: fetch a large number of websites
    private int currentUrlIndex = 0;
    private String currentHandlingUrl = "";

    // For mode 2: fetch a single webpage for a large number of times
    private static final int REPEAT = 1;
    private int currentTimes = 0;

    private boolean readUrlFromFile(String urllist) {
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(getAssets().open(urllist)));
            String mLine;
            while ((mLine = inputReader.readLine()) != null) {
                if (mLine.isEmpty())
                    continue;
                //Log.d(TAG, "Read URL:" + mLine);
                if (!mLine.startsWith("http://"))
                    mLine = "http://www." + mLine;
                urlList.add(mLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to open website list " + urllist);
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
        // Init, check if result folder exist
        File resultDir = new File(DIR);
        if (!resultDir.exists())
        {
            try
            {
                if (!resultDir.createNewFile())
                    Log.e(TAG, "Cannot create result folder!");
            }
            catch (IOException e)
            {
                Log.e(TAG, "Cannot create result folder!");
            }
        }

        if (REPEAT > 0) {
            if (urlList.size() == 1)
            {
                String url = urlList.get(0);
                for (int i = 0; i < (REPEAT-1); i++) {
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

                    //Boolean loadingFinished = true;
                    //Boolean redirect = false;

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        //Log.d(TAG, url + " onPageStarted!!!");
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        view.loadUrl(js_forNT);
                    }
                });

                // set up webchromeclient
                mywebview.setWebChromeClient(new WebChromeClient() {

                    private void saveNewResults(String[] allPairs) {

                        String[] urlPair = allPairs[0].split(":");
                        String url = urlPair[1];
                        String newRecord = "";
                        for (int i=1; i < allPairs.length; i++)
                        {
                            newRecord += allPairs[i].split(":")[1] + " ";
                        }
                        try {
                            File resultFile = new File(DIR + "/" + url);
                            if (!resultFile.exists()) {
                                if (!resultFile.createNewFile())
                                {
                                    Log.e(TAG, "Cannot create result file for " + url);
                                    return;
                                }
                            }
                            // Otherwise, append new result under existing one
                            FileOutputStream fs = new FileOutputStream(resultFile, true);
                            fs.write(newRecord.getBytes());
                            fs.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Fail to save results, because: " + e.getMessage());
                        }
                    }

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
                        // Got new result, handle it, several possibilities
                        // the loadEventEnd > 0, write result, loadNext website
                        // the loadEventEnd == 0, if timeout, write result, loadNext website
                        // Otherwise, wait 0.5s, run JS again, try to get the right load end value
                        WebView view =
                        if (message.startsWith("PLTresults")) {
                            String[] allPairs = message.split(";");
                            try {
                                String[] loadEventEndPair = allPairs[1].split(":");
                                double loadEventEnd = Integer.parseInt(loadEventEndPair[1]);
                                if (loadEventEnd > 0 || TIMEOUT)
                                {
                                    saveNewResults(allPairs);
                                    loadNext();
                                }
                                else
                                {
                                    // wait 0.5s and run js again
                                    view.loadUrl(js_forNT);
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Cannot parse " + message);
                            }
                        }



                        if (!TIMEOUT) {
                            // You can have two choice: only load next page when last page is correctly handled
                            // Or you can ignore it.. by remove the if condition
                            //Log.d(TAG, parsedurl);
                            // Using host is not a good idea. what if two websites have the same host ?
                            String actualURL = mywebview.getUrl();
                            //Log.d(TAG, actualURL + ' ' + currentHandlingUrl);
                            // Important ! Here the logic is tricky !~~~~, think it through when you got time. but now it is fine
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
                        }
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
                Log.d(TAG, (currentUrlIndex + 1) + "'s url: " + currentHandlingUrl + " Total is: " + urlList.size());
            }
        });
    }
}
