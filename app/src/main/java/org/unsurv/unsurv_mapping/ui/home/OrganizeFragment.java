package org.unsurv.unsurv_mapping.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.unsurv_mapping.R;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.tileprovider.util.StorageUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.unsurv.unsurv_mapping.CameraRepository;
import org.unsurv.unsurv_mapping.MapLocationUtils;
import org.unsurv.unsurv_mapping.MapStorageUtils;
import org.unsurv.unsurv_mapping.SurveillanceCamera;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OrganizeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OrganizeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    SharedPreferences sharedPreferences;
    CameraRepository cameraRepository;

    boolean offlineMode;

    List<SurveillanceCamera> cameras;
    List<OverlayItem> overlayItemsToDisplay;
    ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;

    MapView map;
    IMapController mapController;
    OverlayManager overlayManager;

    GeoPoint centerMap;
    boolean lockState;

    List<Polyline> lines = new ArrayList<>();

    SwitchCompat lockSwitch;
    EditText centerLat;
    EditText centerLon;
    EditText gridLength;
    EditText gridHeight;
    EditText gridRows;
    EditText gridColumns;

    Button resetButton;
    Button drawButton;
    Button exportButton;

    Context context;
    Resources resources;

    private MyLocationNewOverlay myLocationOverlay;
    ImageButton myLocationButton;


    public OrganizeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OrganizeFragment newInstance(String param1, String param2) {
        OrganizeFragment fragment = new OrganizeFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // mParam1 = getArguments().getString(ARG_PARAM1);

        }

    }

    @Override
    public void onResume() {

        lockState = sharedPreferences.getBoolean("organizeLockState", false);
        lockSwitch.setChecked(lockState);

        String oldLat = sharedPreferences.getString("gridCenterLat", "");
        String oldLon = sharedPreferences.getString("gridCenterLon", "");
        String oldZoom = sharedPreferences.getString("gridZoom", "");

        if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {
            centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
            mapController.setZoom(Double.parseDouble(oldZoom));
            mapController.setCenter(centerMap);
        }


        // load and show old grid if chosen before
        // values are saved as "centerLat,centerLon,length,height,rows,columns"
        String oldGrid = sharedPreferences.getString("organizeGrid", "");

        if (!oldGrid.isEmpty()) {

            String[] gridValues = oldGrid.split(",");
            double gridLat = Double.parseDouble(gridValues[0]);
            double gridLon = Double.parseDouble(gridValues[1]);

            int gridLength = Integer.parseInt(gridValues[2]);
            int gridHeight = Integer.parseInt(gridValues[3]);
            int gridRows = Integer.parseInt(gridValues[4]);
            int gridColumns = Integer.parseInt(gridValues[5]);

            drawGrid(gridLat, gridLon, gridLength, gridHeight, gridRows, gridColumns);
        }

        cameras = cameraRepository.getAllCameras();

        offlineMode = sharedPreferences.getBoolean("offlineMode", true);


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
                                OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(requireActivity().getApplication()),
                                        new File[]{list[i]});
                                //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                                map.setTileProvider(tileProvider);

                                map.setTileSource(new XYTileSource(
                                        "tiles",
                                        10,
                                        15,
                                        256,
                                        ".png",
                                        new String[]{""}
                                ));

                            } catch (Exception ex) {
                                Toast.makeText(context, "Could not load offline tiles", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }

        } else {

            // MAPNIK fix
            // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
            // TODO add choice + backup strategy here
            map.setTileSource(TileSourceFactory.OpenTopo);
        }


        deleteMarkers();
        populateWithMarkers();


        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_organize, container, false);

        context = getContext();
        assert context != null;
        resources = context.getResources();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        cameraRepository = new CameraRepository(requireActivity().getApplication());

        lockSwitch = v.findViewById(R.id.organize_lock_grid);
        centerLat = v.findViewById(R.id.organize_center_lat_edit);
        centerLon = v.findViewById(R.id.organize_center_lon_edit);

        // default grid is 1000m x 1000m, 5 rows 5 columns
        gridLength = v.findViewById(R.id.organize_length_edit);
        gridLength.setText("3000");

        gridHeight = v.findViewById(R.id.organize_height_edit);
        gridHeight.setText("3000");

        gridRows = v.findViewById(R.id.organize_rows_edit);
        gridRows.setText("5");

        gridColumns = v.findViewById(R.id.organize_columns_edit);
        gridColumns.setText("5");

        resetButton = v.findViewById(R.id.organize_reset);
        drawButton = v.findViewById(R.id.organize_draw);
        exportButton = v.findViewById(R.id.organize_export);

        overlayItemsToDisplay = new ArrayList<>();

        map = v.findViewById(R.id.organize_camera_map);

        mapController = map.getController();
        overlayManager = map.getOverlayManager();

        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
        overlayManager.add(copyrightOverlay);

        map.setTilesScaledToDpi(true);
        map.setClickable(false);
        map.setMultiTouchControls(true);



        // remove big + and - buttons at the bottom of the map
        final CustomZoomButtonsController zoomController = map.getZoomController();
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);


        //standardZoom = 5.0;
        // sharedPreferences.edit().putString("gridZoom", String.valueOf(standardZoom)).apply();

        String oldLat = sharedPreferences.getString("gridCenterLat", "");
        String oldLon = sharedPreferences.getString("gridCenterLon", "");
        String oldZoom = sharedPreferences.getString("gridZoom", "");



        if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {
            centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
            mapController.setZoom(Double.parseDouble(oldZoom));
            mapController.setCenter(centerMap);
        } else {
            // Setting starting position and zoom level.
            centerMap = new GeoPoint(49.9955, 8.2856);
            mapController.setZoom(13.0);
            mapController.setCenter(centerMap);
        }



        // refresh values after 200ms delay
        map.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                IGeoPoint centerAfterScroll = map.getMapCenter();

                centerMap = new GeoPoint(centerAfterScroll);
                sharedPreferences.edit().putString("gridCenterLat", String.valueOf(centerMap.getLatitude())).apply();
                sharedPreferences.edit().putString("gridCenterLon", String.valueOf(centerMap.getLongitude())).apply();


                centerLat.setText(String.valueOf(Math.round(centerAfterScroll.getLatitude())));
                centerLon.setText(String.valueOf(centerAfterScroll.getLongitude()));


                // onZoom triggers only when there is absolutely no movement too, save zoom level here too
                Double zoomLevel = map.getZoomLevelDouble();
                sharedPreferences.edit().putString("gridZoom", String.valueOf(zoomLevel)).apply();

                return false;

            }

            @Override
            public boolean onZoom(ZoomEvent event) {

                Double zoomLevel = map.getZoomLevelDouble();
                sharedPreferences.edit().putString("gridZoom", String.valueOf(zoomLevel)).apply();


                return false;

            }
        }, 200));


        myLocationOverlay = new MyLocationNewOverlay(map);
        myLocationOverlay.enableMyLocation();

        // TODO manage following
        // myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapController.setCenter(myLocationOverlay.getMyLocation());
        map.getOverlays().add(myLocationOverlay);

        // Button to find user location.
        myLocationButton = v.findViewById(R.id.organize_my_location_button);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapController.setCenter(myLocationOverlay.getMyLocation());
                mapController.setZoom(20.0);
            }
        });


        lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                lockState = b;
                sharedPreferences.edit().putBoolean("organizeLockState", b).apply();
            }
        });


        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (lockState) {

                    Toast.makeText(context, "Grid and map locked", Toast.LENGTH_LONG).show();

                } else {

                    new AlertDialog.Builder(context)
                            .setTitle("Clear Data?")
                            .setMessage("Do you want to clear this data?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    sharedPreferences.edit().remove("gridCenterLat").apply();
                                    sharedPreferences.edit().remove("gridCenterLon").apply();
                                    sharedPreferences.edit().remove("gridZoom").apply();

                                    deleteGrid();


                                    double workshopLat = Double.parseDouble(sharedPreferences.getString("workshopLat", "50.1146"));
                                    double workshopLon = Double.parseDouble(sharedPreferences.getString("workshopLon", "8.6822"));

                                    centerMap = new GeoPoint(workshopLat, workshopLon);
                                    mapController.setZoom(14.0);
                                    mapController.setCenter(centerMap);
                                    redrawMap();

                                    Toast.makeText(context, "Successfully cleared data.", Toast.LENGTH_LONG).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();

                }



            }
        });

        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (lockState) {

                    Toast.makeText(context, "Grid and map locked", Toast.LENGTH_LONG).show();

                } else {

                    int length = Integer.parseInt(gridLength.getText().toString());
                    int height = Integer.parseInt(gridHeight.getText().toString());

                    int rows = Integer.parseInt(gridRows.getText().toString());
                    int columns = Integer.parseInt(gridColumns.getText().toString());

                    deleteGrid();
                    drawGrid(centerMap.getLatitude(), centerMap.getLongitude(), length, height, rows, columns);
                }




            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<SurveillanceCamera> allCameras = cameraRepository.getAllCameras();
                if(MapStorageUtils.exportCaptures(allCameras)) {
                    Toast.makeText(getContext(), "Successfully exported data to /Documents/export.txt", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Failed to export data", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Inflate the layout for this fragment
        return v;
    }


    void drawGrid(double centerLat, double centerLon, int length, int height, int rows, int columns){

        // outer rectangle of grid

        // left edge is center lon - length/2
        GeoPoint topLeft = new GeoPoint(MapLocationUtils.getNewLocation(
                centerLat, centerLon, height/2d, -length/2d));

        GeoPoint topRight = new GeoPoint(MapLocationUtils.getNewLocation(
                centerLat, centerLon, height/2d, length/2d));

        GeoPoint bottomRight = new GeoPoint(MapLocationUtils.getNewLocation(
                centerLat, centerLon, -height/2d, length/2d));

        GeoPoint bottomLeft = new GeoPoint(MapLocationUtils.getNewLocation(
                centerLat, centerLon, -height/2d, -length/2d));

        List<GeoPoint> outerRect = new ArrayList<>(Arrays.asList(topLeft, topRight, bottomRight, bottomLeft, topLeft));

        drawLine(outerRect);

        // stepsize of dividing lines
        int partHeight = height / rows;
        int partLength = length / columns;

        GeoPoint tmpRowStartpoint;
        GeoPoint tmpRowEndpoint;

        // one "step" south from topleft, that's the top point of the first divider line for rows
        tmpRowStartpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                topLeft.getLatitude(), topLeft.getLongitude(), -partHeight, 0));

        // rows , ex: we need 4 lines to divide into 5 parts
        for (int i = 0; i < rows - 1; i++) {

            tmpRowEndpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                    tmpRowStartpoint.getLatitude(), tmpRowStartpoint.getLongitude(), 0, length));

            List<GeoPoint> gridLinePoints = new ArrayList<>(Arrays.asList(tmpRowStartpoint, tmpRowEndpoint));

            drawLine(gridLinePoints);

            // new startpoint is one "step" south of previous startpoint
            tmpRowStartpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                    tmpRowStartpoint.getLatitude(), tmpRowStartpoint.getLongitude(), -partHeight, 0));

        }

        GeoPoint tmpColStartpoint;
        GeoPoint tmpColEndpoint;

        tmpColStartpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                topLeft.getLatitude(), topLeft.getLongitude(), 0, partLength));

        // columns
        for (int j = 0; j < columns - 1; j++) {

            tmpColEndpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                    tmpColStartpoint.getLatitude(), tmpColStartpoint.getLongitude(), -height, 0));

            List<GeoPoint> gridLinePoints = new ArrayList<>(Arrays.asList(tmpColStartpoint, tmpColEndpoint));

            drawLine(gridLinePoints);

            // new startpoint is one "step" east of previous startpoint
            tmpColStartpoint = new GeoPoint(MapLocationUtils.getNewLocation(
                    tmpColStartpoint.getLatitude(), tmpColStartpoint.getLongitude(), 0, partLength));

        }

        String gridValues = String.format("%s,%s,%s,%s,%s,%s", centerLat, centerLon, length, height, rows, columns);

        sharedPreferences.edit().putString("organizeGrid", gridValues).apply();
    }

    void drawLine(List<GeoPoint> geoPoints){

        Polyline polyline = new Polyline();

        polyline.setPoints(geoPoints);

        int hotPink = Color.argb(127, 255, 0, 255);

        polyline.setColor(hotPink);

        lines.add(polyline);

        overlayManager.add(polyline);

        redrawMap();

    }

    void redrawMap() {
        map.invalidate();
    }

    void deleteGrid() {
        overlayManager.removeAll(lines);
        redrawMap();
    }


    // use different Markers for different types when moving marker bug is fixed
    void populateWithMarkers() {
        List<SurveillanceCamera> filteredCameras = new ArrayList<>();

        // TODO create db call for non training captures
        // filter for non training captures
        for (SurveillanceCamera camera : cameras) {
            if (!camera.getTrainingCapture()) {
                filteredCameras.add(camera);
            }
        }

        for (SurveillanceCamera nonTrainingCapture : filteredCameras) {

            GeoPoint geoPoint = new GeoPoint(
                    nonTrainingCapture.getLatitude(),
                    nonTrainingCapture.getLongitude());

            OverlayItem marker = new OverlayItem("placeholder", "placeholder", geoPoint);


            overlayItemsToDisplay.add(marker);

        }

        Resources res = context.getResources();
        Drawable cameraMarkerIcon = ResourcesCompat.getDrawable(res, R.drawable.simple_marker_5dpi, null);

        itemizedIconOverlay = new ItemizedIconOverlay<>(overlayItemsToDisplay,
                cameraMarkerIcon,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {


                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(context, "id:" + index +
                                        ", lat:" + item.getPoint().getLatitude() +
                                        ", lon:" + item.getPoint().getLongitude(),
                                Toast.LENGTH_LONG).show();

                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }

                }, context);

        overlayManager.add(itemizedIconOverlay);


    }

    void deleteMarkers(){
        overlayManager.remove(itemizedIconOverlay);
        redrawMap();
    }


}