package org.unsurv.unsurv_mapping;


import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unsurv_mapping.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Collections;
import java.util.List;


/**
 * Used in HistoryActivity to display captures made by users.
 */

public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.CameraViewHolder> {


    class CameraViewHolder extends RecyclerView.ViewHolder {

        // small bar on lefthand side of view for each item in recyclerview
        private final View cameraTypeBar;

        private final ImageView thumbnailImageView;
        private final TextView topTextViewInItem;
        private final TextView middleTextViewInItem;
        private final TextView bottomTextViewInItem;

        private final ImageButton deleteButton;

        private final Context ctx;


        private CameraViewHolder(View itemView) {
            super(itemView);
            cameraTypeBar = itemView.findViewById(R.id.type_bar);
            thumbnailImageView = itemView.findViewById(R.id.thumbnail_image);
            topTextViewInItem = itemView.findViewById(R.id.history_item_text_view_top);
            middleTextViewInItem = itemView.findViewById(R.id.history_item_text_view_middle);
            bottomTextViewInItem = itemView.findViewById(R.id.history_item_text_view_bottom);
            deleteButton = itemView.findViewById(R.id.history_item_delete_button);
            ctx = itemView.getContext();

        }

    }

    private final LayoutInflater mInflater;

    private List<SurveillanceCamera> mSurveillanceCameras;

    private final CameraRepository cameraRepository;
    private final SharedPreferences sharedPreferences;
    private final LayoutInflater layoutInflater;
    private final Context ctx;
    private CameraViewModel cameraViewModel;
    private File imageFile;
    boolean currentCameraUploadComplete;
    SurveillanceCamera current;

    Fragment mParentFragment;

    private BroadcastReceiver br;
    private IntentFilter intentFilter;
    private LocalBroadcastManager localBroadcastManager;


    public CameraListAdapter(Context context, Application application, LayoutInflater layoutInflater, CameraViewModel cameraViewModel, Fragment parentFragment) {
        mInflater = LayoutInflater.from(context);
        cameraRepository = new CameraRepository(application);
        this.cameraViewModel = cameraViewModel;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.layoutInflater = layoutInflater;
        ctx = context;

        mParentFragment = parentFragment;

        localBroadcastManager = LocalBroadcastManager.getInstance(ctx);


    }

    @NonNull
    @Override
    public CameraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View itemView = mInflater.inflate(R.layout.camera_recyclerview_item_history, parent, false);

        return new CameraViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull final CameraViewHolder holder, int position) {

        if (mSurveillanceCameras != null) {
            current = mSurveillanceCameras.get(position);

            currentCameraUploadComplete = current.getUploadCompleted();


            final boolean trainingCapture = current.getTrainingCapture();
            int cameraType = current.getCameraType();
            int cameraArea = current.getArea();


                // not a training capture, use correct storage path
                imageFile = new File(MapStorageUtils.CAMERA_CAPTURES_PATH + current.getThumbnailPath());

                holder.cameraTypeBar.setBackgroundColor(Color.TRANSPARENT);

                switch (cameraArea){
                    case MapStorageUtils.AREA_PUBLIC:
                        holder.cameraTypeBar.setBackgroundColor(Color.parseColor("#ff5555")); // red
                        break;

                    case MapStorageUtils.AREA_OUTDOOR:
                        holder.cameraTypeBar.setBackgroundColor(Color.BLUE);
                        break;

                    case MapStorageUtils.AREA_INDOOR:
                        holder.cameraTypeBar.setBackgroundColor(Color.GREEN);
                        break;
                }

                String timestamp = current.getTimestamp();

                // display type, area and upload date for regular captures
                holder.topTextViewInItem.setText(
                        ctx.getString(R.string.history_type_text, MapStorageUtils.typeList.get(cameraType)));

                // holder.middleTextViewInItem.setText(ctx.getString(R.string.history_area_text, MapStorageUtils.areaList.get(cameraArea)));

                holder.middleTextViewInItem.setText(
                        ctx.getString(R.string.history_timestamp_text, timestamp));

            }


            // String mComment = current.getComment();

            Picasso.get().load(imageFile)
                    .placeholder(R.drawable.ic_camera_alt_grey_50dp)
                    .into(holder.thumbnailImageView);



            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // create popupView to ask user if he wants to delete camera
                    // saves to SharedPreference if "don't ask again" checkbox ticked

                    boolean quickDeleteCameras = sharedPreferences.getBoolean("quickDeleteCameras", false);

                    if (quickDeleteCameras){

                        cameraRepository.deleteCamera(current);
                        notifyItemRemoved(holder.getAdapterPosition());

                    } else {


                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

                        View dontAskAgainLinearLayout = layoutInflater.inflate(R.layout.alert_dialog_dont_ask_again, null);
                        final CheckBox dontAskAgainCheckBox = dontAskAgainLinearLayout.findViewById(R.id.dismiss_popup_dont_show_again_checkbox);
                        alertDialogBuilder.setView(dontAskAgainLinearLayout);

                        alertDialogBuilder.setTitle("Do you want to permanently delete this camera?");

                        alertDialogBuilder.setMessage(null);

                        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (dontAskAgainCheckBox.isChecked()) {
                                    sharedPreferences.edit().putBoolean("quickDeleteCameras", true).apply();
                                }

                                cameraRepository.deleteCamera(current);
                                notifyItemRemoved(holder.getAdapterPosition());
                            }
                        });

                        alertDialogBuilder.setNegativeButton("No", null);
                        alertDialogBuilder.show();

                    }

                }
            });


            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // opens EditCameraActivity if regular capture, DrawOnTrainingImageActivity if item is a training capture.

                    Log.i("holder onClick:", "clicked position: " + holder.getAdapterPosition());

                    int currentPosition = holder.getAdapterPosition();
                    SurveillanceCamera currentCamera = mSurveillanceCameras.get(currentPosition);


                    // camera is captured via obj detection
                    // if delete was last action image view is invisible
                    holder.thumbnailImageView.setVisibility(View.VISIBLE);


                    Bundle args = new Bundle();
                    args.putLong("cameraId", currentCamera.getId());

                    NavController navController = NavHostFragment.findNavController(mParentFragment);
                    navController.navigate(R.id.editCameraFragment, args);

                }
            });

    }


    public void setCameras(List<SurveillanceCamera> cameras){
        mSurveillanceCameras = cameras;
        notifyDataSetChanged();
    }

    void redrawStatus() {

    }

    @Override
    public int getItemCount() {
        if (mSurveillanceCameras != null)
            return mSurveillanceCameras.size();
        else return 0;
    }



}