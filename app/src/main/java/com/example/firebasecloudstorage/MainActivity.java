package com.example.firebasecloudstorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int RC_SIGN_IN = 999;
    private TextView tv_nama;
    private FloatingActionButton btn_add_image;
    private ImageButton btn_logout;
    private StorageReference storageReference;

    private RecyclerView rv_images;
    private ImageAdapter imageAdapter;
    private DatabaseReference databaseReference;
    private List<Image> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_nama = findViewById(R.id.tv_nama);
        btn_add_image = findViewById(R.id.btn_add_image);
        btn_logout = findViewById(R.id.btn_logout);
        rv_images = findViewById(R.id.rv_images);
        rv_images.setHasFixedSize(true);
        rv_images.setLayoutManager(new LinearLayoutManager(this));

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference("image");

        btn_add_image.setOnClickListener(this);
        btn_logout.setOnClickListener(this);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            );
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), RC_SIGN_IN);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            tv_nama.setText(user.getDisplayName());

            StorageReference fotoRef = storageReference.child("image");

            Task<ListResult> listPageTask = fotoRef.list(1);
            listPageTask.addOnSuccessListener(new OnSuccessListener<ListResult>() {
                        @Override
                        public void onSuccess(ListResult listResult) {
                            List<StorageReference> items = listResult.getItems();
                            if(!items.isEmpty()) {
                                Toast.makeText(MainActivity.this, "loading Foto", Toast.LENGTH_LONG).show();
                                items.get(0).getDownloadUrl();
                            } else {
                                Toast.makeText(MainActivity.this, "Belum ada Foto", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "can't get Image, "+e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()) {
                        Image image = ds.getValue(Image.class);
                        images.add(image);
                    }

                    imageAdapter = new ImageAdapter(MainActivity.this, images);
                    rv_images.setAdapter(imageAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Error database!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void selectImage(Context context) {
        final CharSequence[] options = {"Upload gambar dari Galeri", "Batal"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pilih aksi");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Upload gambar dari Galeri")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
                } else if (options[item].equals("Batal")) {
                    dialog.dismiss();
                }
            }
        });

        builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        uploadToStorage(selectedImage);
                    }
                    break;
                case RC_SIGN_IN :
                    if (resultCode == RESULT_OK ) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        tv_nama.setText(user.getDisplayName());

                        storageReference = FirebaseStorage.getInstance().getReference();
                        StorageReference fotoRef = storageReference.child("image");

                        Task<ListResult> listPageTask = fotoRef.list(1);
                        listPageTask.addOnSuccessListener(new OnSuccessListener<ListResult>() {
                            @Override
                            public void onSuccess(ListResult listResult) {
                                List<StorageReference> items = listResult.getItems();
                                if(!items.isEmpty()) {
                                    items.get(0).getDownloadUrl();
                                } else {
                                    Toast.makeText(MainActivity.this, "Belum ada Foto", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "can't get Image, "+e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "We couldn't sign you in. Please try again later.", Toast.LENGTH_LONG).show();
                    }
            }
        }
    }

    public void uploadToStorage(Uri file) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UploadTask uploadTask;
        storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference fotoRef = storageReference.child("image/"+"IMG"+new Date().getTime()+".jpg");
        uploadTask = fotoRef.putFile(file);
        Toast.makeText(MainActivity.this, "uploading Image", Toast.LENGTH_SHORT).show();

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(MainActivity.this, "can't upload Image, "+exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();

                fotoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Image image = new Image(uri.toString());
                        String imageId = databaseReference.push().getKey();
                        databaseReference.child(imageId).setValue(image);
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_add_image) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    String [] Permisions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(Permisions,100);
                } else{
                    selectImage(MainActivity.this);
                }
            } else {
                selectImage(MainActivity.this);
            }
        } else if(v.getId()==R.id.btn_logout) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(MainActivity.this, "You have been signed out.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
            selectImage(MainActivity.this);
        } else {
            Toast.makeText(this,"denied",Toast.LENGTH_LONG).show();
        }
    }
}