package com.example.rami.top10apps;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ListView appList;
    private String feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private final String TEXT_CONTENTS = "TextContents";
    private final String LIMIT_CONTENTS = "LimitContens";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appList = (ListView) findViewById(R.id.xmlListView);

        if(savedInstanceState != null){
            feedLimit = savedInstanceState.getInt(LIMIT_CONTENTS);
            feedURL = savedInstanceState.getString(TEXT_CONTENTS);
        }

        dowloadURL(String.format(feedURL, feedLimit));
        Log.d(TAG, "onCreate: End");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: Start");
        outState.putInt(LIMIT_CONTENTS, feedLimit);
        outState.putString(TEXT_CONTENTS, feedURL);
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: End");
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        Log.d(TAG, "onRestoreInstanceState: Start");
//        super.onRestoreInstanceState(savedInstanceState);
//        feedLimit = savedInstanceState.getInt(LIMIT_CONTENTS);
//        feedURL = savedInstanceState.getString(TEXT_CONTENTS);
//        dowloadURL(String.format(feedURL, feedLimit));
//        Log.d(TAG, "onRestoreInstanceState: End");
//
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: Start");
        getMenuInflater().inflate(R.menu.list_menu, menu);
        if (feedLimit == 10) {
            menu.findItem(R.id.mnuTop10).setChecked(true);
        } else {
            menu.findItem(R.id.mnuTop25).setChecked(true);
        }
        Log.d(TAG, "onCreateOptionsMenu: End");
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mnuFree:
                if(feedURL.equals("http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml")){
                    break;
                }
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                dowloadURL(String.format(feedURL, feedLimit));
                break;
            case R.id.mnuPaid:
                if(feedURL.equals("http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml")){
                    break;
                }
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                dowloadURL(String.format(feedURL, feedLimit));
                break;
            case R.id.mnuSongs:
                if(feedURL.equals("http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml")){
                    break;
                }
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                dowloadURL(String.format(feedURL, feedLimit));
                break;
            case R.id.mnuRefresh:
                dowloadURL(String.format(feedURL, feedLimit));
                break;
            case R.id.mnuTop10:
            case R.id.mnuTop25:
                if (!item.isChecked()) {
                    feedLimit = 35 - feedLimit;
                    item.setChecked(true);
                    dowloadURL(String.format(feedURL, feedLimit));
                } else {
                    Log.d(TAG, "onOptionsItemSelected: it is not changed" + item.getTitle());
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void dowloadURL(String URL) {
        Log.d(TAG, "onCreate: starts the async task");
        DownloadData downloadData = new DownloadData();
        downloadData.execute(URL);
        Log.d(TAG, "onCreate: done");
    }

    private class DownloadData extends AsyncTask<String, Void, String> {

        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute: parameter is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            appList.setAdapter(feedAdapter);
        }

        @Override
        protected String doInBackground(String... params) {
            //      Log.d(TAG, "doInBackground: starts with " + params[0]);
            String rssFeed = downloadXML(params[0]);
            if (rssFeed == null) {
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        private String downloadXML(String urlPath) {

            StringBuilder XMLResult = new StringBuilder();

            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                //   Log.d(TAG, "downloadXML: The connection response was " + response);
                InputStream inputStream = connection.getInputStream();
                InputStreamReader inputStreamreader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamreader);


                int charRead;
                char[] inputBuffer = new char[500];

                while (true) {
                    charRead = reader.read(inputBuffer);
                    if (charRead < 0) {
                        break;
                    }
                    if (charRead > 0) {
                        XMLResult.append(String.copyValueOf(inputBuffer, 0, charRead));
                    }
                }
                reader.close();
                return XMLResult.toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "downloadXML: Invalid URL " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "downloadXML: IO Exception reading Data: " + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "downloadXML: Permesion denied. " + e.getMessage());
                //   e.printStackTrace();
            }
            return null;
        }


    }


}
