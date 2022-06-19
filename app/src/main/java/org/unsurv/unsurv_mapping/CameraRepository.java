package org.unsurv.unsurv_mapping;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Used for SurveillanceCamera database access with mostly with Lists.
 * I put db access in AsyncTasks to allow access in ui threads.
 */

public class CameraRepository {

  private CameraDao mCameraDao;
  private LiveData<List<SurveillanceCamera>> mAllSurveillanceCameras;
  // private List<SurveillanceCamera> mSurveillanceCamerasInArea;

  public CameraRepository(Context applicationContext) {
    CameraRoomDatabase db = CameraRoomDatabase.getDatabase(applicationContext);
    mCameraDao = db.surveillanceCameraDao();
    mAllSurveillanceCameras = mCameraDao.getAllCamerasAsLiveData();
  }

  LiveData<List<SurveillanceCamera>> getAllCamerasAsLiveData() {
      return mAllSurveillanceCameras;
  }

  public long insert (SurveillanceCamera surveillanceCamera) {

    try {
      return new insertAsyncTask(mCameraDao).execute(surveillanceCamera).get();
    } catch (Exception e) {
      Log.i("Background insert Error: " , e.toString());
      return 0;
    }


  }

  int getCamerasAddedByUserCount(){
    try {
      return new getCountAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }
  }

  public void updateCameras (SurveillanceCamera... surveillanceCameras) {
    new updateAsyncTask(mCameraDao).execute(surveillanceCameras);
  }

  void deleteCamera(SurveillanceCamera... surveillanceCameras) {

    new deleteAsyncTask(mCameraDao).execute(surveillanceCameras);
  }


  public List<SurveillanceCamera> getAllCameras(){

    try {
      List<SurveillanceCamera> a = new getAllCamerasAsyncTask(mCameraDao).execute().get();
      return a;

    } catch (Exception e) {
      Log.i("Background getAllCameras Error: " , e.toString());
      return null;
    }

  }

  List<SurveillanceCamera> getCamerasForUpload(){

    try {
      // .get() accesses the return value of doInBackground() from the async task
      return new getCamerasForUploadAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background getIdsForImageUpload Error: " , e.toString());
      return null;
    }

  }

  List<SurveillanceCamera> getCamerasForImageUpload(){

    try {

      return new getCamerasForImageUploadAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background getIdsForImageUpload Error: " , e.toString());
      return null;
    }

  }

  public SurveillanceCamera findByDbId(long dbId){

    try {

      return new findByIDAsyncTask(mCameraDao).execute(dbId).get();

    } catch (Exception e) {
      Log.i("Background findByDbId Error: " , e.toString());
      return null;
    }

  }

  SurveillanceCamera getLastCamera(){

    try {

      return new getLastCameraAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByDbId Error: " , e.toString());
      return null;
    }

  }



  private static class insertAsyncTask extends AsyncTask<SurveillanceCamera, Void, Long> {

    private CameraDao mAsyncTaskDao;

    insertAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Long doInBackground(final SurveillanceCamera... params) {
      return mAsyncTaskDao.insert(params[0]);
    }
  }

  private static class getCountAsyncTask extends AsyncTask<Void, Void, Integer> {

    private CameraDao mAsyncTaskDao;

    getCountAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Integer doInBackground(final Void... params) {
      return mAsyncTaskDao.getTotalCamerasAddedByUser();
    }
  }

  private static class updateAsyncTask extends AsyncTask<SurveillanceCamera, Void, Void> {

    private CameraDao mAsyncTaskDao;

    updateAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SurveillanceCamera... params) {
      mAsyncTaskDao.updateCameras(params);
      return null;
    }
  }

  private static class deleteAsyncTask extends AsyncTask<SurveillanceCamera, Void, Void> {

    private CameraDao mAsyncTaskDao;

    deleteAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SurveillanceCamera... params) {

      int deletedFiles = MapStorageUtils.deleteImagesForCamera(params[0]);
      Log.i("deleteCameraAsync", "deleted " + deletedFiles + " files");

      mAsyncTaskDao.deleteCameras(params);
      return null;
    }
  }

  private static class getAllCamerasAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getAllCamerasAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getAllCameras();
    }
  }

  private static class getCamerasForUploadAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getCamerasForUploadAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getCamerasToUpload();
    }
  }


  private static class getCamerasForImageUploadAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getCamerasForImageUploadAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getCamerasForImageUpload();
    }
  }

  private static class findByIDAsyncTask extends AsyncTask<Long, Void, SurveillanceCamera> {

    private CameraDao mAsyncTaskDao;

    findByIDAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SurveillanceCamera doInBackground(final Long... params) {

      return mAsyncTaskDao.findById(params[0]);
    }

  }


  private static class getLastCameraAsyncTask extends AsyncTask<Void, Void, SurveillanceCamera> {

    private CameraDao mAsyncTaskDao;

    getLastCameraAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SurveillanceCamera doInBackground(Void... params) {

      return mAsyncTaskDao.getLastCamera();
    }

  }


}
