package com.example.anas.mymap;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private GoogleMap map;
    private LocationManager locationManager;
    private Marker userMarker;
    private Marker[] placeMarkers;
    private final int MAX_PLACES = 20;
    private MarkerOptions[] places;


    private int userIcon;
    private int foodIcon;
    private int shopIcon;
    private int drinkIcon;
    private int otherIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        placeMarkers = new Marker[MAX_PLACES];

        map.setMyLocationEnabled(true);

        userIcon = R.drawable.user_place;
        foodIcon = R.drawable.marker_teal;
        shopIcon = R.drawable.marker_yellow;
        drinkIcon = R.drawable.marker_purple;
        otherIcon = R.drawable.marker_orange;

        updatePlaces();

    }

    //method to update user location
    private void updatePlaces(){
        double lat;
        double lng;
        LatLng lastLatLng;

        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);

        //get the last location
        Location lastLocation =  locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        //save the current latitude and longitude
        lat  = lastLocation.getLatitude();
        lng = lastLocation.getLongitude();

        Log.d("BENZINO", lat+" : "+lng);

        //wrap up in a LatLng object to put it on the marker
        lastLatLng  = new LatLng(lat, lng);

        if( userMarker !=null ) userMarker.remove();

        //add marker to the users location
        userMarker = map.addMarker(new MarkerOptions()
                .position(lastLatLng)
                .title("You are here")
                .icon(BitmapDescriptorFactory.fromResource(userIcon))
                .snippet("Your last recorded position"));

        //Animate the camera to zoom to the users position
        map.animateCamera(CameraUpdateFactory.newLatLng(lastLatLng), 3000, null);

        //Google places search query
        String placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" +
                "json?location="+lat+","+lng+
                "&radius=10000&sensor=true" +
                "&types=food|bar|store|museum|art_gallery|stadium"+
                "&key=AIzaSyD-RNcbWX7PCB6Cz85rD1XrsPIEFdgU5fQ";

        new GetPlaces().execute(placesSearchStr);

    }

    public Location getLastKnownLocation() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    //Async task classes to fetch the places
    private class GetPlaces extends AsyncTask<String, Void, String>{
        StringBuilder placesBuilder;

        @Override
        protected String doInBackground(String... placesURL) {
            //fetch and get places data
            placesBuilder = new StringBuilder();

            //process Search parameter string
            for(String placesSearchURL:placesURL){
                //execute search
                HttpClient placesClient = new DefaultHttpClient();

                try{
                    //try to fetch the data
                    HttpGet placesGet = new HttpGet(placesSearchURL);
                    //retrieve Http response
                    HttpResponse placesResponse = placesClient.execute(placesGet);
                    //Check if we have a positive response HTTP:200 ok
                    StatusLine status = placesResponse.getStatusLine();

                    if(status.getStatusCode() == 200){
                        //retrieve the actual content of the response data
                        HttpEntity placesEntity = placesResponse.getEntity();
                        InputStream placesContent = placesEntity.getContent();

                        //create a reader for this stream
                        InputStreamReader placesInput = new InputStreamReader(placesContent);

                        BufferedReader placesReader = new BufferedReader(placesInput);

                        String line = placesReader.readLine();

                        while (line != null)
                            placesBuilder.append(line);


                    }
                }catch (Exception e){

                }
            }
            Log.d("BENZINO", ":: " + placesBuilder.toString());
            return placesBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            String result = placesBuilder.toString();

            //removing any existing markers
            if(placeMarkers != null)
                for(int i =0; i<placeMarkers.length; i++)
                    if(placeMarkers[i] !=null)
                        placeMarkers[i].remove();

            try{
                //parse JSON
                JSONObject resultObject = new JSONObject(result);

                //places contained in a JSON Array
                JSONArray placesArray = resultObject.getJSONArray("results");

                places = new MarkerOptions[placesArray.length()];

                boolean missingValue = false;

                LatLng placeLatLng = null;
                String placeName = "";
                String vicinity = "";
                int currentIcon = otherIcon;

                Log.d("BENZINO", "PLACES"+placesArray.length());
                //loop through places
                for(int i = 0; i < placesArray.length(); i++){
                    try{
                        //parse each place
                        JSONObject placeObject = placesArray.getJSONObject(i);

                        JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");

                        placeLatLng = new LatLng(
                                Double.valueOf(loc.getString("lat")),
                                Double.valueOf(loc.getString("lng")));

                        JSONArray types = placeObject.getJSONArray("types");
                        Log.d("BENZINO", "FOOD");
                        //loop through the type array
                        for(int t = 0; t < types.length(); t++){
                            String type = types.get(t).toString();

                            //using particular icons for each location
                            if(type.contains("food")){
                                Log.d("BENZINO", "FOOD");
                                currentIcon = foodIcon;
                                break;
                            }else if(type.contains("bar")){
                                Log.d("BENZINO", "BAR");
                                currentIcon = drinkIcon;
                                break;
                            }else if(type.contains("store")){
                                Log.d("BENZINO", "STORE");
                                currentIcon = shopIcon;
                                break;
                            }else if(type.contains("stadium")){
                                Log.d("BENZINO", "STAD");
                                currentIcon = shopIcon;
                                break;
                            }
                        }

                        //get vicinity data
                        vicinity = placeObject.getString("vicinity");

                        //place name
                        placeName = placeObject.getString("name");


                    }catch (JSONException ex){
                        missingValue = true;
                        ex.printStackTrace();
                    }

                    if(missingValue)
                        places[i]=null;
                    else
                        places[i] = new MarkerOptions()
                                .position(placeLatLng)
                                .title(placeName)
                                .icon(BitmapDescriptorFactory.fromResource(currentIcon))
                                .snippet(vicinity);
                }

            }catch (Exception e){
                Log.d("BENZINO", e.getMessage());

            }
            if(places!=null && placeMarkers!=null){
                for(int p=0; p<places.length && p<placeMarkers.length; p++){
                    //will be null if a value was missing
                    if(places[p]!=null)
                        placeMarkers[p]=map.addMarker(places[p]);
                }
            }
        }

        //Log.d("BENZINO", "onPost");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
