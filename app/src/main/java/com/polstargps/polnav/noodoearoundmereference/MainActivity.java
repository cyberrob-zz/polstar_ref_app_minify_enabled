package com.polstargps.polnav.noodoearoundmereference;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.polstargps.polnav.noodoearoundpoi.NearbyPoiQueryManager;
import com.polstargps.polnav.noodoearoundpoi.POIData;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    NearbyPoiQueryManager queryManager = null;
    TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView)findViewById(R.id.result_text);
        Button bindBtn = (Button)findViewById(R.id.bind_btn);
        Button queryBtn = (Button)findViewById(R.id.query_btn);

        resultTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        bindBtn.setOnClickListener(clickBindButton);
        queryBtn.setOnClickListener(clickQueryButton);

        String path = getExternalFilesDir(null).getPath()+"/";
        String dbFilePath = path+"poi.db";
//        String dbFilePath = path+"PolnavDatabase.sqlite";
        Log.d("MainActivity", "NearbyPoiQueryManager, dbFilePath="+dbFilePath);

        //Step1: Set POI DB file path to NearbyPoiQueryManager
        queryManager = NearbyPoiQueryManager.getInstance(dbFilePath);

        //Step2: Must Set Context to NearbyPoiQueryManager
        queryManager.setContext(this);
    }

    public View.OnClickListener clickBindButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TelephonyManager mTelManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            //get IMEI
            String IMEI = mTelManager.getDeviceId();
//            Log.d("MainActivity", "clickBindButton, IMEI="+IMEI);
            bindSyncTask syncTask = new bindSyncTask("abcdefghijkl", IMEI);
            syncTask.execute((Void)null);
        }
    };

    public View.OnClickListener clickQueryButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /* Step4: Query Around Me POI
             *
             * Note: If bindUUID function never be executed, the output value will be null.
             *
             * Input: <Latitude>, <Longitude>, <Radius(KM)>, <Type>, <Limit Count>
             * The maximum query radius is 3 KM
             * Custom can use "|" to choose multiple type:  QUERY_TYPE_CONVINENT_STORE|QUERY_TYPE_GAS_STATION|QUERY_TYPE_KYMCO
             * or use QUERY_TYPE_ALL to query without consider type.
             *
             * Output: POIData array list ordered by distance(Unit: KM).
             * The maximum size of POIData array list is according to limitCount value.
             *
             */
            ArrayList<POIData> result = queryManager.queryPoiData(24.841555, 121.013585, 2, NearbyPoiQueryManager.QUERY_TYPE_CONVINENT_STORE|NearbyPoiQueryManager.QUERY_TYPE_GAS_STATION|NearbyPoiQueryManager.QUERY_TYPE_KYMCO, 50);

            if (result != null) {
                String resultStr = "";
                for (int i=0; i<result.size(); i++) {
                    POIData tmp = result.get(i);
                    if (tmp.getType() == NearbyPoiQueryManager.QUERY_TYPE_CONVINENT_STORE) {
                        Log.d("MainActivity", "clickQueryButton, Convinent Store Type");
                    } else if (tmp.getType() == NearbyPoiQueryManager.QUERY_TYPE_GAS_STATION) {
                        Log.d("MainActivity", "clickQueryButton, Gas Station Type");
                    } else if (tmp.getType() == NearbyPoiQueryManager.QUERY_TYPE_KYMCO) {
                        Log.d("MainActivity", "clickQueryButton, Kymco Type");
                    } else {
                        Log.e("MainActivity", "clickQueryButton, Unknown POI Type");
                    }
                    Log.d("MainActivity", "clickQueryButton, result["+i+"], id="+tmp.getId()+", Logitude="+tmp.getLongitude()+", Latitude="+tmp.getLatitude()+", name="+tmp.getName()+", country="+tmp.getCountry()+", distance="+tmp.getDistance());
                    resultStr += "result["+i+"], id="+tmp.getId()+", Logitude="+tmp.getLongitude()+", Latitude="+tmp.getLatitude()+", name="+tmp.getName()+", country="+tmp.getCountry()+", distance="+tmp.getDistance()+"\n";
                }
                resultTextView.setText(resultStr);
            } else {
                resultTextView.setText("Please bind first!");
            }
        }
    };

    public boolean isOnline(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private class bindSyncTask extends AsyncTask<Void, Void, String>
    {
        String uuidStr, phoneDeviceIdStr;

        public bindSyncTask(String uuid, String phoneDeviceId)
        {
            uuidStr = uuid;
            phoneDeviceIdStr = phoneDeviceId;
        }


        protected void onPreExecute()
        {
        }

        @Override
        protected String doInBackground(Void... params)
        {
            // TODO Auto-generated method stub
            String resultStr = "";
//            if (isOnline(MainActivity.this)) {
                /* Step3: Check the pair <mobile phone's Device ID , motorcycle's UUID> has ever bind. If no, bind to Server.
                 *
                 * Input: uuid, phoneDeviceId
                 * Output:
                 *      1. BIND_UUID_SUCCESS: Query success.
                 *      2. BIND_UUID_ERROR_CONNECTION: Query failed because of network problem.
                 *      3. Error code directly from Server or from HttpRequest(ex:404)
                 */
                int bindResult = queryManager.bindUUID(uuidStr, phoneDeviceIdStr);
                switch (bindResult){
                    case NearbyPoiQueryManager.BIND_UUID_SUCCESS:
                        resultStr = "Bind Success";
                        break;
                    case NearbyPoiQueryManager.BIND_UUID_ERROR_CONNECTION:
                        resultStr = "Connection to server error!\n Please check network status of mobile phone or server!";
                        break;
                    default:
                        resultStr = "Unkown error, error code="+bindResult;
                        break;
                }
//            } else {
//                resultStr = "Please open mobile phone's network";
//            }
            return resultStr;
        }

        protected void onProgressUpdate(Integer... values)
        {

        }

        protected void onPostExecute(String result)
        {
            resultTextView.setText(result);
        }
    }
}
