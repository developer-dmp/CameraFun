package com.dmp.camerapractice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_IMAGE = 1;
    private static final int PICK_IMAGE_FROM_GALLERY = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_E_STORAGE = 10;

    private static final String TAG = "CameraAcvtivity";
    private static final String PERMISSION_DENIED = "Permission denied";

    private ImageView mImageView;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        configure();
    }

    /**
     * Helper method to configure the layout and assign listeners for views.
     */
    private void configure() {
        mImageView = (ImageView)findViewById(R.id.main_image_view);
        mImageView.setOnClickListener(this);

        Button captureImageButton = (Button) findViewById(R.id.capture_image_button);
        captureImageButton.setOnClickListener(this);
    }

    /**
     * Invoked by the OS since we implement {@link View.OnClickListener} interface
     *
     * @param v the view that was clicked by the user
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.capture_image_button:
                dispatchTakePictureIntent();
                break;
            case R.id.main_image_view:
                imageViewPressed();
                break;
        }
    }

    /**
     * Invoked by the OS when we start an activity for result.
     *
     * @param requestCode the unique code assigned to the intent
     * @param resultCode displays success of the overall action
     * @param data the returning intent used to finish the job
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            addPicToGallery();
            setPicFromCamera(mImageView);
        } else if (requestCode == PICK_IMAGE_FROM_GALLERY && resultCode == RESULT_OK) {
            if (data != null) {
                setPicFromGallery(data.getData(), mImageView);
            } else {
                toast("Oh no, could not load image");
            }
        }
    }

    /**
     * Used to handle the user's interaction with the runtime-permission request
     *
     * @param requestCode unique code for permissions
     * @param permissions permission asked for
     * @param grantResults success of the overall action
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_E_STORAGE) {
            if (grantResults[0] ==  PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                toast("Permission denied");
            }
        }
    }

    /**
     * Helper method to create and launch a new intent prompting the user to take
     * a picture via the Camera application.  We receive and parse the result.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage());
                // if we failed because of permission issue, we prompt
                if (ex.getMessage().equals(PERMISSION_DENIED)) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_E_STORAGE);
                }
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.dmp.camerapractice.fileprovider",
                        photoFile);
                //Uri photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE);
            }
        }
    }

    /**
     * Method to determine what to do when the {@link ImageView} is pressed.
     */
    private void imageViewPressed() {

        // if there is no image in it, prompt them via the camera to take one
        if (mImageView.getDrawable() == null) {
            dispatchTakePictureIntent();
            return;
        }

        // otherwise we allow them to choose what they should do
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = MyDialog.newInstance(R.layout.dialog_image_view_pressed, new DialogListener() {
            @Override
            public void onPositiveClick() {
                // replace from camera
                dispatchTakePictureIntent();
            }

            @Override
            public void onNegativeClick() {
                // select from gallery
//                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                getIntent.setType("image/*");
//
//                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                pickIntent.setType("image/*");
//
//                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
//                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
//
//                startActivityForResult(chooserIntent, PICK_IMAGE_FROM_GALLERY);

                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, PICK_IMAGE_FROM_GALLERY);
            }
        });
        newFragment.show(ft, "dialog");
    }

    /**
     * Interface definition to understand which button was selected
     * by the user when the {@link MyDialog} is presented to them.
     */
    public interface DialogListener {
        void onPositiveClick();
        void onNegativeClick();
    }

    /**
     * Helper method to create a unique file name for each picture taken
     * by the user.
     *
     * @return the {@link File} for the image to be saved into
     * @throws IOException in the event we cannot create the file for
     * some reason (usually has to do with permission failures)
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir =  getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Helper method to add the recently taken picture to the device's gallery
     */
    private void addPicToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Most of the time when taking a picture from the Camera intent, the {@link Bitmap}
     * returned is not properly oriented and sized.  This helper method will
     * correct any issues pertaining to that.
     *
     * @param iv the {@link ImageView} to load the corrected {@link Bitmap} into
     */
    private void setPicFromCamera(ImageView iv) {
        // Get the dimensions of the View
        int targetW = iv.getWidth();
        int targetH = iv.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        iv.setImageBitmap(configureBitmap(bitmap, mCurrentPhotoPath));
    }

    /**
     * Helper method used to load an image from the device's gallery.
     * Invoked once the user selects an image from their preferred image storage location.
     *
     * @param uri the {@link Uri} returned from the intent the activity received
     * @param iv the {@link ImageView} to load the bitmap into
     */
    private void setPicFromGallery(Uri uri, ImageView iv) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);


            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
            } else {
                toast("Oh no, could not load image");
            }
        }
    }

    /**
     * Helper method to ensure the bitmap loaded from the camera intent will be loaded into
     * the {@link ImageView} in portrait mode.
     *
     * @param bitmap the image to be loaded
     * @param photoPath path to the photo on the device
     * @return the bitmap ready to be loaded into the {@link ImageView}
     */
    private Bitmap configureBitmap(Bitmap bitmap, String photoPath) {
        ExifInterface ei;
        try {
            ei = new ExifInterface(photoPath);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage());
            return null;
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(bitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(bitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(bitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = bitmap;
        }
        return rotatedBitmap;
    }

    /**
     * Helper method to rotate a bitmap representation of an image.
     * Apparently this is needed often when taking pictures with the camera.
     *
     * @param source bitmap to be rotated to portrait
     * @param angle degree the bitmap must be rotated by
     * @return portrait style bitmap image
     */
    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /**
     * Helper method to show a toast
     *
     * @param message the text to display to the user
     */
    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}