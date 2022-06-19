package org.unsurv.unsurv_mapping.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.unsurv_mapping.R;

import org.unsurv.unsurv_mapping.CameraListAdapter;
import org.unsurv.unsurv_mapping.CameraViewModel;
import org.unsurv.unsurv_mapping.MainActivity;
import org.unsurv.unsurv_mapping.SurveillanceCamera;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class HistoryFragment extends Fragment {

    CameraViewModel mCameraViewModel;
    int readStoragePermission;
    int writeStoragePermission;
    RecyclerView recyclerView;

    private SharedPreferences sharedPreferences;
    private CameraListAdapter adapter;
    private Context context;



    public HistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                    } else {
                        Toast.makeText(requireContext(),
                                "Storage permission needed for database and export.",
                                Toast.LENGTH_LONG).show();
                    }
                });

        // check permissions on resume
        readStoragePermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        writeStoragePermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);


        List<String> permissionList = new ArrayList<>();

        if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE

            );
        }

        if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

            );
        }


    }

    @Override
    public void onResume() {






        // redraw / refresh recyclerview items by resetting adapter
        recyclerView.setAdapter(null);
        recyclerView.setAdapter(adapter);


        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        // main view for captures
        recyclerView = v.findViewById(R.id.camera_recyclerview);

        // enables access to db
        mCameraViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()).create(CameraViewModel.class);

        // needed in adapter to show PopupWindows
        LayoutInflater layoutInflater = requireActivity().getLayoutInflater();

        adapter = new CameraListAdapter(requireContext(), requireActivity().getApplication(), layoutInflater, mCameraViewModel, getParentFragment());

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // LiveData to immediately include new data in recyclerview
        mCameraViewModel.getAllCameras().observe(getViewLifecycleOwner(), new Observer<List<SurveillanceCamera>>() {
            @Override
            public void onChanged(@Nullable List<SurveillanceCamera> surveillanceCameras) {
                adapter.setCameras(surveillanceCameras);
            }
        });

        return v;
    }
}