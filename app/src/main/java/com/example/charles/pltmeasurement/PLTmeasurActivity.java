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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PLTmeasurActivity extends AppCompatActivity {

    private static final String TAG = "PLTmeasurement";
    private static final ArrayList<String> urlList = new ArrayList<String>();
    private static ArrayList<String> measurementResults = new ArrayList<String>();
    private static String USER_AGENT="Mozilla/5.0 (Linux; U; Android 4.3; en-us; SCH-I535 Build/JSS15J)" +
            " AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
    private static final String js_forNT = "javascript:(\n function() { \n"
            + "var result='';\n"
            + "var perfOBJ = performance.timing;\n"
            + "var PLT = perfOBJ.loadEventEnd - perfOBJ.navigationStart;\n"
            + "var PIT = perfOBJ.domComplete - perfOBJ.navigationStart;\n"
            + "var PTT = perfOBJ.responseEnd - perfOBJ.navigationStart;\n"
            + "result = PTT + ',' + PIT + ',' + PLT;\n"
            + "console.log(result);\n"
            + "for (var prop in perfOBJ){\n"
            + "console.log(prop + ':' + perfOBJ[prop]); }\n"
            //+ "console.log('Redirection count:' + performance.navigation.redirectCount);\n"
            + " })()\n";

    private int currentUrlIndex = 0;
    private int totalURLNumber = 0;

    private boolean readUrlFromFile(String urllist){
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(getAssets().open(urllist)));
            String mLine;
            while ((mLine = inputReader.readLine()) != null){
                Log.d(TAG, "Read URL:" + mLine);
                if (!mLine.startsWith("http://"))
                    mLine = "http://www." + mLine;
                urlList.add(mLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to open website list " + urllist);
            return false;
        }
        totalURLNumber = urlList.size();
        return (totalURLNumber > 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Read all URLs
        if(!readUrlFromFile("website")){
            return;
        }
        final Button start = (Button) findViewById(R.id.buttonStart);
        start.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                WebView mywebview = (WebView) findViewById(R.id.myWebView);
                // Set up webview client
                mywebview.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url){
                        super.onPageFinished(view, url);
                        Log.d(TAG, url + " finish loading !");
                        new Handler() {
                            private WebView view;
                            public void init(WebView v) {
                                view = v;
                                post(new Runnable () {
                                    @Override
                                    public void run() {
                                        view.loadUrl(js_forNT);
                                        if ((currentUrlIndex + 1) < totalURLNumber)
                                        {
                                            currentUrlIndex += 1;
                                            view.loadUrl(urlList.get(currentUrlIndex));
                                        }
                                    }
                                });
                            }
                        }.init(view);
                    }
                });
                // set up webchromeclient
                mywebview.setWebChromeClient(new WebChromeClient(){
                    @Override
                    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                        String[] separated = message.split(",");
                        //double ptt = Double.parseDouble(separated[0]);
                        //double pit = Double.parseDouble(separated[1]);
                        //double plt = Double.parseDouble(separated[2]);
                        // Otherwise no valid data is collected

                        Log.d(TAG, message);
                        //if ((ptt> 0) && (pit > 0) && (plt > 0))
                        //{
                        //    Log.d(TAG, "Got valid data !");
                        //    measurementResults.add(message);
                        //}
                    }
                });

                // Set web setting
                WebSettings webSettings = mywebview.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAppCacheEnabled(false);
                webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                webSettings.setUserAgentString(USER_AGENT);
                mywebview.loadUrl(urlList.get(currentUrlIndex));
            }
        });
    }
}
