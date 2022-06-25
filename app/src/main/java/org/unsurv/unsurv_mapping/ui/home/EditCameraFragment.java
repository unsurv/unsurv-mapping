package org.unsurv.unsurv_mapping.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;


import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;


import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unsurv_mapping.R;
import com.squareup.picasso.Picasso;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.unsurv.unsurv_mapping.BottomAnchorIconOverlay;
import org.unsurv.unsurv_mapping.CameraRepository;
import org.unsurv.unsurv_mapping.MainActivity;
import org.unsurv.unsurv_mapping.MapLocationUtils;
import org.unsurv.unsurv_mapping.MapStorageUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.unsurv.unsurv_mapping.SurveillanceCamera;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditCameraFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "cameraId";

    // TODO: Rename and change types of parameters

    private long mParam1;


    CameraRepository cameraRepository;


    SurveillanceCamera cameraToEdit;
    SharedPreferences sharedPreferences;
    boolean offlineMode;

    Drawable cameraMarkerIcon;

    ImageView detailCameraImageView;
    MapView map;

    EditText cameraOwner;
    TextView timestampTextView;

    Spinner cameraTypeSpinner;

    SeekBar directionSeekBar;
    TextView directionTextView;

    Spinner areaSpinner;

    SeekBar heightSeekBar;
    TextView heightTextView;

    Spinner mountSpinner;

    SeekBar angleSeekBar;
    TextView angleTextView;

    CheckBox hasSignCheckbox;
    CheckBox hasCompleteSignCheckbox;

    TextView hasSignStatusTextView;
    TextView hasCompleteSignStatusTextView;

    Button saveButton;
    Button editButton;
    ImageButton resetMapButton;
    ImageButton takePictureButton;
    ImageView editLocationMarker;

    File cameraImage;
    IMapController mapController;

    IconOverlay iconOverlay;
    GeoPoint cameraLocation;

    Polyline line = new Polyline();
    Polygon polygon = new Polygon();

    int cameraType;

    boolean isBeingEdited = false;

    Context context;
    Resources resources;

    private static String captureImagePath = MapStorageUtils.CAMERA_CAPTURES_PATH;


    public EditCameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EditCameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EditCameraFragment newInstance(String param1, String param2, int cameraId) {
        EditCameraFragment fragment = new EditCameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            mParam1 = getArguments().getLong(ARG_PARAM1);

        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_edit_camera, container, false);

        // TextView textView = v.findViewById(R.id.test);
        // textView.setText(String.valueOf(mParam1));
        requireContext().getApplicationContext();
        // Inflate the layout for this fragment

        context = getContext();
        resources = getResources();
        Context app = MainActivity.applicationContext;

        cameraRepository = new CameraRepository(app);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        offlineMode = sharedPreferences.getBoolean("offlineMode", true);

        map = v.findViewById(R.id.edit_camera_map);

        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
        map.getOverlays().add(copyrightOverlay);

        // user edit area
        cameraTypeSpinner = v.findViewById(R.id.edit_camera_type_selection);

        directionSeekBar = v.findViewById(R.id.edit_camera_direction_seekbar);
        directionSeekBar.setMax(360);
        directionTextView = v.findViewById(R.id.edit_camera_direction_text);

        areaSpinner = v.findViewById(R.id.edit_camera_area_selection);

        heightSeekBar = v.findViewById(R.id.edit_camera_height_seekbar);
        heightSeekBar.setMax(20);
        heightTextView = v.findViewById(R.id.edit_camera_height_text);

        angleSeekBar = v.findViewById(R.id.edit_camera_angle_seekbar);
        angleSeekBar.setMax(75);
        angleTextView = v.findViewById(R.id.edit_camera_angle_text);

        mountSpinner = v.findViewById(R.id.edit_camera_mount_selection);

        hasSignCheckbox = v.findViewById(R.id.edit_camera_has_signage_checkbox);
        hasCompleteSignCheckbox = v.findViewById(R.id.edit_camera_complete_signage_checkbox);

        hasSignStatusTextView = v.findViewById(R.id.edit_camera_signage_status);
        hasCompleteSignStatusTextView = v.findViewById(R.id.edit_camera_complete_signage_status);

        timestampTextView = v.findViewById(R.id.edit_camera_timestamp_text);

        saveButton = v.findViewById(R.id.edit_camera_save_button);
        editButton = v.findViewById(R.id.edit_camera_edit_button);
        resetMapButton = v.findViewById(R.id.edit_camera_reset_map_position);
        editLocationMarker = v.findViewById(R.id.edit_camera_center_marker);
        takePictureButton = v.findViewById(R.id.edit_camera_take_picture_button);

        // Fragment gets started with db id in arguments, get id
        long dbId = mParam1; // TODO off by one SOMEWHERE

        List<SurveillanceCamera>  asd = cameraRepository.getAllCameras();
        cameraToEdit = cameraRepository.findByDbId(dbId);

        cameraType = cameraToEdit.getCameraType();
        String thumbnailPath = cameraToEdit.getThumbnailPath();


        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int b = 1;
            }
        });


        //example data: ""[asd.jpg, bsd.jpg]""
        String[] filenames = cameraToEdit.getThumbnailFilesAsStringArray();




        map.setTilesScaledToDpi(true);
        map.setClickable(false);
        map.setMultiTouchControls(true);



        // remove big + and - buttons at the bottom of the map
        final CustomZoomButtonsController zoomController = map.getZoomController();
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = map.getController();

        double lat = cameraToEdit.getLatitude();
        double lon = cameraToEdit.getLongitude();

        // Setting starting position and zoom level.
        cameraLocation = new GeoPoint(lat, lon);
        mapController.setZoom(17.0);
        mapController.setCenter(cameraLocation);

        int hotPink = Color.argb(127, 255, 0, 255);
        polygon.setFillColor(hotPink);
        polygon.setStrokeColor(hotPink);

        drawCameraArea(cameraLocation,
                cameraToEdit.getDirection(),
                cameraToEdit.getHeight(),
                cameraToEdit.getAngle(),
                cameraToEdit.getCameraType());

        // Spinner for camera type selection
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.edit_camera_type, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinners
        cameraTypeSpinner.setAdapter(typeAdapter);

        cameraTypeSpinner.setSelection(cameraType);

        cameraTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                ((TextView) adapterView.getChildAt(0))
                        .setTextColor(getResources().getColor(R.color.white, null));

                cameraToEdit.setCameraType(i);

                cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(resources,
                        MapStorageUtils.chooseMarker(i, cameraToEdit.getArea()), 12, null);

                map.getOverlays().remove(iconOverlay);

                iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

                // reactivate all edits
                directionSeekBar.setEnabled(true);
                angleSeekBar.setEnabled(true);

                drawCameraArea(cameraLocation,
                        cameraToEdit.getDirection(),
                        cameraToEdit.getHeight(),
                        cameraToEdit.getAngle(),
                        cameraToEdit.getCameraType());

                List<Overlay> asdf = map.getOverlays();
                map.getOverlays().add(iconOverlay);
                map.invalidate();


            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        int cameraDirection = cameraToEdit.getDirection();

        directionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                directionTextView.setText(String.valueOf(i));


                drawCameraArea(
                        new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                        i,
                        cameraToEdit.getHeight(),
                        cameraToEdit.getAngle(),
                        cameraToEdit.getCameraType());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                cameraToEdit.setDirection(progress);
            }
        });

        if (cameraDirection != -1) {
            directionTextView.setText(String.valueOf(cameraDirection));
            directionSeekBar.setProgress(cameraDirection);
        } else {
            directionTextView.setText("?");
            directionSeekBar.setProgress(0);
        }


        int area = cameraToEdit.getArea();

        // Spinner for camera type selection
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> areaAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.edit_camera_area, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinners
        areaSpinner.setAdapter(areaAdapter);

        areaSpinner.setSelection(area);

        areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                ((TextView) adapterView.getChildAt(0))
                        .setTextColor(getResources().getColor(R.color.white, null));

                cameraToEdit.setArea(i);

                cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(resources,
                        MapStorageUtils.chooseMarker(cameraToEdit.getCameraType(), i), 12, null);

                map.getOverlays().remove(iconOverlay);
                List<Overlay> asdf = map.getOverlays();

                iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

                // reactivate all edits
                directionSeekBar.setEnabled(true);
                angleSeekBar.setEnabled(true);

                drawCameraArea(cameraLocation,
                        cameraToEdit.getDirection(),
                        cameraToEdit.getHeight(),
                        cameraToEdit.getAngle(),
                        cameraToEdit.getCameraType());

                map.getOverlays().add(iconOverlay);
                map.invalidate();


            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        int cameraHeight = cameraToEdit.getHeight();

        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                heightTextView.setText(String.valueOf(i));

                drawCameraArea(
                        new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                        cameraToEdit.getDirection(),
                        i,
                        cameraToEdit.getAngle(),
                        cameraToEdit.getCameraType());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                cameraToEdit.setHeight(progress);
            }
        });

        if (cameraHeight != -1) {
            heightTextView.setText(String.valueOf(cameraHeight));
            heightSeekBar.setProgress(cameraHeight);
        } else {
            heightTextView.setText("?");
            heightSeekBar.setProgress(0);
        }


        int cameraAngle = cameraToEdit.getAngle();

        angleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                angleTextView.setText(String.valueOf(i + 15));

                drawCameraArea(
                        new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                        cameraToEdit.getDirection(),
                        cameraToEdit.getHeight(),
                        i + 15, // + 15 because we cant set seekbarmin in API 24
                        cameraToEdit.getCameraType());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                cameraToEdit.setAngle(progress + 15);

            }
        });

        if (cameraAngle != -1) {
            angleTextView.setText(String.valueOf(cameraAngle));

            // seekbar has only 0-75 range, cameraAngle from 15 -90
            angleSeekBar.setProgress(cameraAngle - 15);
        } else {
            angleTextView.setText("?");
            angleSeekBar.setProgress(0);
        }


        int mount = cameraToEdit.getMount();

        // Spinner for camera type selection
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> mountAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.edit_camera_mount, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinners
        mountSpinner.setAdapter(mountAdapter);

        mountSpinner.setSelection(mount);

        mountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                ((TextView) adapterView.getChildAt(0))
                        .setTextColor(getResources().getColor(R.color.white, null));

                cameraToEdit.setMount(i);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        timestampTextView.setText(cameraToEdit.getTimestamp());


        int hasSign = cameraToEdit.getHasSignage();

        switch (hasSign) {

            case MapStorageUtils.SIGN_UNDEFINED:
                hasSignStatusTextView.setText(getString(R.string.sign_status_no_info));
                break;

            case MapStorageUtils.SIGN_PRESENT:
                hasSignStatusTextView.setText(getString(R.string.sign_status_yes));
                hasSignCheckbox.setChecked(true);
                break;

            case MapStorageUtils.SIGN_NOT_PRESENT:
                hasSignStatusTextView.setText(getString(R.string.sign_status_no));
                hasSignCheckbox.setChecked(false);
                break;
        }

        hasSignCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b) {
                    cameraToEdit.setHasSignage(MapStorageUtils.SIGN_PRESENT);
                    hasSignStatusTextView.setText(getString(R.string.sign_status_yes));

                }
                else {
                    cameraToEdit.setHasSignage(MapStorageUtils.SIGN_NOT_PRESENT);
                    hasSignStatusTextView.setText(getString(R.string.sign_status_no));

                }


            }
        });

        int hasCompleteSign = cameraToEdit.getCompleteSignage();

        switch (hasSign) {

            case MapStorageUtils.SIGN_UNDEFINED:
                hasCompleteSignStatusTextView.setText(getString(R.string.sign_status_no_info));
                break;

            case MapStorageUtils.SIGN_PRESENT:
                hasCompleteSignStatusTextView.setText(getString(R.string.sign_status_yes));
                hasCompleteSignCheckbox.setChecked(true);
                break;

            case MapStorageUtils.SIGN_NOT_PRESENT:
                hasCompleteSignStatusTextView.setText(getString(R.string.sign_status_no));
                hasCompleteSignCheckbox.setChecked(false);
                break;
        }

        hasCompleteSignCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b) {
                    cameraToEdit.setCompleteSignage(MapStorageUtils.COMPLETE_SIGN_PRESENT);
                    hasCompleteSignStatusTextView.setText(getString(R.string.sign_status_yes));

                }
                else {
                    cameraToEdit.setCompleteSignage(MapStorageUtils.COMPLETE_SIGN_NOT_PRESENT);
                    hasCompleteSignStatusTextView.setText(getString(R.string.sign_status_no));

                }


            }
        });


        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isBeingEdited) {
                    // stop editing
                    isBeingEdited = false;
                    resetMap();

                } else {
                    isBeingEdited = true;

                    map.getOverlays().removeAll(map.getOverlays());
                    map.invalidate();

                    Picasso.get().load(
                                    MapStorageUtils.chooseMarker(cameraToEdit.getCameraType(), cameraToEdit.getArea()))
                            .into(editLocationMarker);

                    editLocationMarker.setVisibility(View.VISIBLE);

                }

            }
        });



        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isBeingEdited) {

                    IGeoPoint center = map.getMapCenter();
                    double newLat = center.getLatitude();
                    double newLon = center.getLongitude();

                    cameraToEdit.setLatitude(newLat);
                    cameraToEdit.setLongitude(newLon);

                    editLocationMarker.setVisibility(View.INVISIBLE);

                    generateMarkerOverlayWithCurrentLocation();

                    drawCameraArea(new GeoPoint(newLat, newLon),
                            cameraToEdit.getDirection(),
                            cameraToEdit.getHeight(),
                            cameraToEdit.getAngle(),
                            cameraToEdit.getCameraType());

                    cameraRepository.updateCameras(cameraToEdit);

                    isBeingEdited = false;


                } else {

                    // set direction / angle to unknown if type DOME
                    if (cameraToEdit.getCameraType() == MapStorageUtils.DOME_CAMERA) {
                        cameraToEdit.setDirection(-1); // unknown
                        cameraToEdit.setAngle(-1); // unknown

                    }
                    cameraRepository.updateCameras(cameraToEdit);

                    resetMap();


                }

                int  a = getParentFragmentManager().getBackStackEntryCount();
                List<Fragment>  b = getParentFragmentManager().getFragments();

                NavController navController =  NavHostFragment.findNavController(getParentFragment());



            }
        });


        resetMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetMap();
            }
        });



        return v;
    }

    @Override
    public void onResume() {

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
                                        6,
                                        16,
                                        256,
                                        ".png",
                                        new String[]{""}));

                            } catch (Exception ex) {
                                Toast.makeText(context, "Could not load offline tiles", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }

        } else {

            // MAPNIK fix
            Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
            // TODO add choice + backup strategy here
            map.setTileSource(TileSourceFactory.MAPNIK);
        }




        super.onResume();
    }


    void drawCameraArea(GeoPoint currentPos, int direction, int height, int horizontalAngle, int cameraType) {

        int baseViewDistance = 15; // in m

        // if height entered by user
        if (height >= 0) {
            // TODO use formula from surveillance under surveillance https://sunders.uber.space
            // add 30% viewdistance per meter of height

            double heightFactor = 1 + (0.3 * height);
            baseViewDistance *= heightFactor;
        }

        if (horizontalAngle != -1) {
            // TODO use formula from surveillance under surveillance https://sunders.uber.space

            // about the same as SurveillanceUnderSurveillance https://sunders.uber.space
            double angleFactor = Math.pow(25f / horizontalAngle, 2) * 0.4;
            baseViewDistance *= angleFactor;
        }

        // remove old drawings
        map.getOverlayManager().remove(polygon);
        map.getOverlayManager().remove(line);

        List<GeoPoint> geoPoints;

        if (cameraType == MapStorageUtils.FIXED_CAMERA || cameraType == MapStorageUtils.PANNING_CAMERA) {

            int viewAngle;

            // calculate geopoints for triangle

            double startLat = currentPos.getLatitude();
            double startLon = currentPos.getLongitude();

            geoPoints = new ArrayList<>();


            if (cameraType == MapStorageUtils.FIXED_CAMERA) {
                viewAngle = 60; // fixed camera

                // triangle sides compass direction
                int direction1 = direction - viewAngle / 2;
                int direction2 = direction + viewAngle / 2;

                // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
                double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
                double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

                double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
                double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;


                Location endpoint1 = MapLocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
                Location endpoint2 = MapLocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);



                geoPoints.add(new GeoPoint(startLat, startLon));
                geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
                geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));


            } else {
                viewAngle = 120; // panning camera

                // using two 60 degree cones instead of one 120 cone

                // triangle sides compass direction
                int direction1 = direction - viewAngle / 2;
                int direction2 = direction;
                int direction3 = direction + viewAngle / 2;


                // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
                double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
                double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

                double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
                double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;

                double xDiff3 = Math.cos(Math.toRadians(90 - direction3)) * baseViewDistance;
                double yDiff3 = Math.sin(Math.toRadians(90 - direction3)) * baseViewDistance;


                Location endpoint1 = MapLocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
                Location endpoint2 = MapLocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);
                Location endpoint3 = MapLocationUtils.getNewLocation(startLat, startLon, yDiff3, xDiff3);

                geoPoints.add(new GeoPoint(startLat, startLon));
                geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
                geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));
                geoPoints.add(new GeoPoint(endpoint3.getLatitude(), endpoint3.getLongitude()));


            }





        } else {

            // circle for dome cameras
            geoPoints = Polygon.pointsAsCircle(currentPos, height * 7);

        }

        polygon.setPoints(geoPoints);
        map.getOverlayManager().add(polygon);
        map.invalidate();

    }


    void generateMarkerOverlayWithCurrentLocation() {

        // Setting starting position and zoom level.
        GeoPoint cameraLocation = new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude());
        mapController.setZoom(17.0);
        mapController.setCenter(cameraLocation);

        map.getOverlays().remove(iconOverlay);

        iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

        map.getOverlays().add(iconOverlay);
        map.invalidate();

    }



    void resetMap() {
        if (isBeingEdited) {
            isBeingEdited = false;
        }
        editLocationMarker.setVisibility(View.INVISIBLE);
        generateMarkerOverlayWithCurrentLocation();
    }

}