package org.unsurv.unsurv_mapping.ui.home;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.unsurv_mapping.R;
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
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.unsurv.unsurv_mapping.CameraDao;
import org.unsurv.unsurv_mapping.CameraRepository;
import org.unsurv.unsurv_mapping.CameraRoomDatabase;
import org.unsurv.unsurv_mapping.MainActivity;
import org.unsurv.unsurv_mapping.MapStorageUtils;
import org.unsurv.unsurv_mapping.SurveillanceCamera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MapView mapView;

    private CameraRepository cameraRepository;
    private CameraDao cameraDao;


    private SharedPreferences sharedPreferences;
    private IMapController mapController;
    private boolean offlineMode;

    private ImageButton manualSaveButton;
    private ImageButton addStandardCameraButton;
    private ImageButton addDomeCameraButton;
    private ImageButton addUnknownCameraButton;
    private ImageButton manualToGrid;

    private ImageView marker;

    private int cameraType;
    private FragmentManager fragmentManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        fragmentManager = getActivity().getSupportFragmentManager();

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context app = MainActivity.applicationContext;
        cameraRepository = new CameraRepository(app);
        CameraRoomDatabase db = CameraRoomDatabase.getDatabase(requireActivity().getApplication());
        cameraDao = db.surveillanceCameraDao();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        mapView = binding.manualCaptureMap;

        manualSaveButton = binding.manualSaveButton;
        addStandardCameraButton = binding.manualCaptureAddStandardCameraButton;
        addDomeCameraButton = binding.manualCaptureAddDomeCameraButton;
        addUnknownCameraButton = binding.manualCaptureAddUnknownCameraButton;
        manualToGrid = binding.manualToGrid;

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

        mapView.setTilesScaledToDpi(true);
        mapView.setClickable(true);

        //enable pinch to zoom
        mapView.setMultiTouchControls(true);

        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(getContext());
        mapView.getOverlays().add(copyrightOverlay);

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

        // add different types depending on user choice
        addStandardCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraType = MapStorageUtils.FIXED_CAMERA;
                marker.setImageDrawable(getContext().getDrawable(R.drawable.standard_camera_marker_5_dpi));
            }
        });

        addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraType = MapStorageUtils.DOME_CAMERA;
                marker.setImageDrawable(getContext().getDrawable(R.drawable.dome_camera_marker_5_dpi));

            }
        });

        addUnknownCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraType = MapStorageUtils.PANNING_CAMERA;
                marker.setImageDrawable(getContext().getDrawable(R.drawable.unknown_camera_marker_5dpi));

            }
        });

        manualSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IGeoPoint center = mapView.getMapCenter();

                SimpleDateFormat timestampIso8601 = new SimpleDateFormat("k:m", Locale.US);

                long currentTime = System.currentTimeMillis();

                String currentDate;

                currentDate = timestampIso8601.format(new Date(currentTime));


                // when editing camera location, a new marker is created in the center of the map
                // get center coordinates when save button is pressed
                SurveillanceCamera manualCamera = new SurveillanceCamera(
                        cameraType,
                        MapStorageUtils.AREA_PUBLIC,
                        -1,
                        0,
                        5,
                        15,
                        null,
                        null,
                        null,
                        center.getLatitude(),
                        center.getLongitude(),
                        "",
                        currentDate,
                        null,
                        false,
                        false,
                        true,
                        false,
                        "",
                        "",
                        MapStorageUtils.SIGN_UNDEFINED,
                        MapStorageUtils.COMPLETE_SIGN_UNDEFINED

                );

                // final long dummyInsert = cameraRepository.insert(manualCamera);
                final long idFromInsert = cameraRepository.insert(manualCamera);


                List<SurveillanceCamera>  asd = cameraRepository.getAllCameras();

                // create small notification for history activity
                // BottomNavigationBadgeHelper.incrementBadge(bottomNavigationView, context, R.id.bottom_navigation_history, 1);




                // start editing activity .5 sec after capture to give db some time to save data
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Bundle args = new Bundle();
                        args.putLong("cameraId", idFromInsert);

                        NavController navController = NavHostFragment.findNavController(requireParentFragment());
                        navController.navigate(R.id.editCameraFragment, args);
                    }
                }, 1000);




            }
        });

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

        } else {

            String homeZone = sharedPreferences.getString("area", "51.124, 51.055, 5.823, 14.7 ");

            String[] coordinates = homeZone.split(",");

            double latMin = Double.parseDouble(coordinates[0]);
            double latMax = Double.parseDouble(coordinates[1]);
            double lonMin = Double.parseDouble(coordinates[2]);
            double lonMax = Double.parseDouble(coordinates[3]);

            double centerLat = (latMin + latMax) / 2;
            double centerLon = (lonMin + lonMax) / 2;

            // Setting starting position and zoom level. Use center of homezone for now
            GeoPoint startPoint = new GeoPoint(centerLat, centerLon);
            mapController.setZoom(10.0);
            mapController.setCenter(startPoint);

        }

        offlineMode = sharedPreferences.getBoolean("offlineMode", true);

        marker = getView().findViewById(R.id.manual_capture_marker);

        manualSaveButton = getView().findViewById(R.id.manual_save_button);
        addStandardCameraButton = getView().findViewById(R.id.manual_capture_add_standard_camera_button);
        addDomeCameraButton = getView().findViewById(R.id.manual_capture_add_dome_camera_button);
        addUnknownCameraButton = getView().findViewById(R.id.manual_capture_add_unknown_camera_button);
        manualToGrid = getView().findViewById(R.id.manual_to_grid);


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}