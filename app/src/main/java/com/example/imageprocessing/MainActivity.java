package com.example.imageprocessing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final int CAMERA_PERMISSION_CODE = 2001;
    static final int CAMERA_INTENT_CODE = 3001;
    static final int GALLERY_SELECT_IMAGE = 4001;
    ImageView imageViewCamera;
    String picturePath;
    Bitmap picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageViewCamera = findViewById(R.id.imageViewCamera);
    }

    public void cameraButton(View view){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestCameraPermission();
        }
        else {
            sendCameraIntent();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void requestCameraPermission(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA
                }, CAMERA_PERMISSION_CODE);
            }
            else {
                sendCameraIntent();
            }
        }
        else {
            Toast.makeText(MainActivity.this, "Não há câmeras disponíves.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                sendCameraIntent();
            }
            else {
                Toast.makeText(MainActivity.this, "Permissão de uso da câmera negada.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ABRE A CÂMERA E PERMITE TIRAR FOTO
    void sendCameraIntent(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        if(intent.resolveActivity(getPackageManager()) != null){
            // new Date() retorna a hora atual
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(new Date());
            String picName = "pic_" +  timeStamp;
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File pictureFile = null;
            try {
                pictureFile = File.createTempFile(picName, ".jpg",dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(pictureFile != null){
                picturePath = pictureFile.getAbsolutePath();
                Uri photouri = FileProvider.getUriForFile(
                        MainActivity.this,
                        "com.example.imageprocessing.fileprovider",
                        pictureFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT,photouri);
                startActivityForResult(intent, CAMERA_INTENT_CODE);
            }
        }
    }

    // ADICIONA A FOTO NA GALERIA DO CELULAR
    public void galleryAddPic(String file) {
        File f = new File(file);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // USA A FOTO TIRADA PELO APLICATIVO
        if(requestCode == CAMERA_INTENT_CODE){
            if(resultCode == RESULT_OK){
                File file = new File(picturePath);
                if(file.exists()){
                    Uri image = Uri.fromFile(file);
                    imageViewCamera.setImageURI(image);
                    galleryAddPic(picturePath);
                    try {
                        // Pega a imagem tirada pela câmera e converte em bitmap
                        InputStream is = getContentResolver().openInputStream(image);
                        picture = BitmapFactory.decodeStream(is);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            }
            else {
                Toast.makeText(MainActivity.this, "Problema ao pegar a imagem da câmera.",
                        Toast.LENGTH_LONG).show();
            }
        }
        // USA FOTO SELECIONADA PELO USUÁRIO NA GALERIA
        else if(requestCode == GALLERY_SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                String[] path = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, path, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(path[0]);
                picturePath = cursor.getString(columnIndex);
                picture = BitmapFactory.decodeFile(picturePath);

                imageViewCamera.setImageBitmap(picture);
                cursor.close();
            }
        }
    }

    // ABRE A GALERIA DO CELULAR
    public void openGallery(View view) {
        Intent gallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, GALLERY_SELECT_IMAGE);
    }

    // CONVERTER FOTO PARA ESCALA DE CINZA USANDO SATURAÇÃO
    public void convertSaturationButton(View view){
        if(picture != null) {
            Bitmap grayPic = grayScaleSaturation(picture);
            imageViewCamera.setImageBitmap(grayPic);
        }
        else {
            Toast.makeText(MainActivity.this, "Nenhuma imagem selecionada.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap grayScaleSaturation(Bitmap picture){
        int width, height;

        // Pega as dimensões da imagem
        height = picture.getHeight();
        width = picture.getWidth();

        // RGB_565: elimina o canal de transparência reduzindo o range de valores para os componentes RGB
        Bitmap toGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(toGrayscale);
        Paint paint = new Paint();
        ColorMatrix matrix = new ColorMatrix();
        // Primeira maneira: mudar a saturação para 0
        /*
        Na teoria da cor, a saturação define uma faixa de cor pura (100%) a cinza (0%).
        A saturação é algumas vezes referida como intensidade da cor, uma cor totalmente saturada
        é uma cor pura, enquanto uma cor totalmente dessaturada aparece como cinza.
         */
        matrix.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(f);
        c.drawBitmap(picture, 0, 0, paint);

        return toGrayscale;
    }

    // CONVERTER FOTO PARA ESCALA DE CINZA USANDO A MÉDIA
    public void convertAverageButton(View view){
        if(picture != null) {
            Bitmap grayPic = grayScaleAverage(picture);
            imageViewCamera.setImageBitmap(grayPic);
        }
        else {
            Toast.makeText(MainActivity.this, "Nenhuma imagem selecionada.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap grayScaleAverage(Bitmap picture){
        int width, height;

        // Pega as dimensões da imagem
        height = picture.getHeight();
        width = picture.getWidth();

        // RGB_565: elimina o canal de transparência reduzindo o range de valores para os componentes RGB
        Bitmap toGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(toGrayscale);
        Paint paint = new Paint();
        // Segunda maneira: calcular a média dos três canais RGB
        /*
        O método da média é o mais simples. Você só precisa tirar a média das três cores.
        Como é uma imagem RGB, isso significa que você adiciona R com G com B, e divide por 3
        para obter a imagem em tons de cinza desejada.
        É feito desta forma:
        Tons de cinza = (R + G + B / 3)
         */
        float[] matrix = new float[]{
                1/3f, 1/3f, 1/3f, 0, 0,
                1/3f, 1/3f, 1/3f, 0, 0,
                1/3f, 1/3f, 1/3f, 0, 0,
                0, 0, 0, 1, 0,};
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(filter);
        c.drawBitmap(picture, 0, 0, paint);

        return toGrayscale;
    }

    // CONVERTER FOTO PARA ESCALA DE CINZA USANDO A MÉDIA
    public void convertWeightedButton(View view){
        if(picture != null) {
            Bitmap grayPic = grayScaleWeighted(picture);
            imageViewCamera.setImageBitmap(grayPic);
        }
        else {
            Toast.makeText(MainActivity.this, "Nenhuma imagem selecionada.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap grayScaleWeighted(Bitmap picture){
        int width, height;

        // Pega as dimensões da imagem
        height = picture.getHeight();
        width = picture.getWidth();

        // RGB_565: elimina o canal de transparência reduzindo o range de valores para os componentes RGB
        Bitmap toGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(toGrayscale);
        Paint paint = new Paint();
        // Terceira maneira: calcular o peso de cada canal RGB de maneira diferente
        /*
        A cor vermelha tem o maior comprimento de onda dentre todas as três cores, e o verde
        é a cor que não só tem menor comprimento de onda do que a cor vermelha, como também é a
        cor que dá mais efeito calmante aos olhos. Isso significa que temos que diminuir a
        contribuição da cor vermelha e aumentar a contribuição da cor verde e colocar a
        contribuição da cor azul entre essas duas. Assim, a nova equação é:
        Nova imagem em tons de cinza = ((0,3 * R) + (0,59 * G) + (0,11 * B)).
         */
        float[] matrix = new float[]{
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0, 0, 0, 1, 0,};
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(filter);
        c.drawBitmap(picture, 0, 0, paint);

        return toGrayscale;
    }
}