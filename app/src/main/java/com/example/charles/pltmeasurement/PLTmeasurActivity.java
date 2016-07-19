package com.example.charles.pltmeasurement;

import android.graphics.Bitmap;
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
    private static final int TIMEOUT_COUNTER = 30000;  // Stop loading more results after 30 second until the last website is loaded
    private static final int LOADING_INTERVAL = 5000;
    private static final ArrayList<String> urlList = new ArrayList<String>();
    private static final String js_forNT = "javascript:(\n function() { \n"
            + "setTimeout(function(){var result='';\n"
            + "var perfOBJ = performance.timing;\n"
            + "var TTFB = perfOBJ.responseStart - perfOBJ.requestStart;\n"
            + "var PRT = perfOBJ.responseEnd - perfOBJ.navigationStart;\n"
            + "var PLT = perfOBJ.loadEventEnd - perfOBJ.navigationStart;\n"
            + "result = TTFB + ',' + PRT + ',' + PLT;\n"
            //+ "console.log('PLTresults:' + result);}, 500);\n"
            + "console.log('PLTresults:' + result);\n"
            + "for (var prop in perfOBJ){\n"
            + "console.log(prop + ':' + perfOBJ[prop]); }}, 500);\n"
            //+ "console.log('Redirection count:' + performance.navigation.redirectCount);\n"
            + " })()\n";
    private static boolean TIMEOUT = false;  // To see if expired
    private static HashMap<String, String> measurementResults = new HashMap<String, String>();
    private static String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.3; en-us; SCH-I535 Build/JSS15J)" +
            " AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    // For mode 1: fetch a large number of websites
    private int currentUrlIndex = 0;
    private String currentHandlingUrl = "";

    // For mode 2: fetch a single webpage for a large number of times
    private static final int REPEAT = 5;
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

                    Boolean loadingFinished = true;
                    Boolean redirect = false;

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        //Log.d(TAG, url + " shouldOverrideUrlLoading!!!");
                        if (!loadingFinished) {
                            redirect = true;
                        }
                        loadingFinished = false;
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        //super.onPageStarted(view, url, favicon);
                        //Log.d(TAG, url + " onPageStarted!!!");
                        loadingFinished = false;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //super.onPageFinished(view, url);
                        //Log.d(TAG, url + " onPageFinished!!!");
                        if (!redirect) {
                            loadingFinished = true;
                        }
                        if (loadingFinished && !redirect) {
                            //Log.d(TAG, url + " Page loading finish !!!");
                            view.loadUrl(js_forNT);
                        } else {
                            redirect = false;
                        }
                    }
                });

                // set up webchromeclient
                mywebview.setWebChromeClient(new WebChromeClient() {
                    private boolean handleMessage(String message) {
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
                    }

                    private void saveResults() {
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
                                        saveResults();
                                    }
                                }
                            }
                        }, LOADING_INTERVAL);
                    }

                    @Override
                    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                        Log.d(TAG, message);
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
                Log.d(TAG, (currentUrlIndex + 1) + "'s url: " + currentHandlingUrl + " Total: " + urlList.size());
            }
        });
    }
}
