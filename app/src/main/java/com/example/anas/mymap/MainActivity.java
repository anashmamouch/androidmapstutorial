package com.example.anas.mymap;

import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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


public class MainActivity extends AppCompatActivity {

    private GoogleMap map;
    private LocationManager locationManager;
    private Marker userMarker;

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

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //get the last location
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        //save the current latitude and longitude
        lat  = lastLocation.getLatitude();
        lng = lastLocation.getLongitude();

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
