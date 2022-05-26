package com.example.unsurv_mapping.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.unsurv_mapping.databinding.FragmentHomeBinding;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private SharedPreferences sharedPreferences;
    private IMapController mapController;
    private boolean offlineMode;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        MapView mapView = binding.manualCaptureMap;

        if (offlineMode) {

            //first we'll look at the default location for tiles that we support
            File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/");
            if (f.exists()) {

                File[] list = f.listFiles();
                if (list != null) {
                    for (int i = 0; i < list.length; i++) {
                        if (list[i].isDirectory()) {
                            continue;
                        }
                        String name = list[i].getName().toLowerCase();
                        if (!name.contains(".")) {
                            continue; //skip files without an extension
                        }
                        name = name.substring(name.lastIndexOf(".") + 1);
                        if (name.length() == 0) {
                            continue;
                        }
                        if (ArchiveFileFactory.isFileExtensionRegistered(name)) {


                            try {
                                //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                                //create the offline tile provider, it will only do offline file archives
                                //again using the first file
                                OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(getContext().getApplicationContext()),
                                        new File[]{list[i]});
                                //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                                mapView.setTileProvider(tileProvider);

                                mapView.setTileSource(new XYTileSource(
                                        "tiles",
                                        6,
                                        16,
                                        256,
                                        ".png",
                                        new String[]{""}));

                            } catch (Exception ex) {
                                Toast.makeText(getContext(), "Could not load offline tiles", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }

        } else {

            // MAPNIK fix
            Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
            // TODO add choice + backup strategy here
            mapView.setTileSource(TileSourceFactory.MAPNIK);
        }

        mapController = mapView.getController();

        //enable pinch to zoom
        mapView.setMultiTouchControls(true);

        // refresh values after 200ms delay
        mapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                IGeoPoint centerAfterScroll = mapView.getMapCenter();


                GeoPoint centerMap = new GeoPoint(centerAfterScroll);
                sharedPreferences.edit().putString("manualCenterLat", String.valueOf(centerMap.getLatitude())).apply();
                sharedPreferences.edit().putString("manualCenterLon", String.valueOf(centerMap.getLongitude())).apply();


                // onZoom triggers only when there is absolutely no movement too, save zoom level here too
                Double zoomLevel = mapView.getZoomLevelDouble();
                sharedPreferences.edit().putString("manualZoom", String.valueOf(zoomLevel)).apply();

                return false;

            }

            @Override
            public boolean onZoom(ZoomEvent event) {

                Double zoomLevel = mapView.getZoomLevelDouble();
                sharedPreferences.edit().putString("manualZoom", String.valueOf(zoomLevel)).apply();

                return false;

            }
        }, 200));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        String oldLat = sharedPreferences.getString("manualCenterLat", "");
        String oldLon = sharedPreferences.getString("manualCenterLon", "");
        String oldZoom = sharedPreferences.getString("manualZoom", "");

        if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {

            GeoPoint centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
            mapController.setZoom(Double.parseDouble(oldZoom));
            mapController.setCenter(centerMap);

        }

        offlineMode = sharedPreferences.getBoolean("offlineMode", true);


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}