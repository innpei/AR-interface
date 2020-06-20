package com.example.cvcamera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Profile extends AppCompatActivity implements DialogRemove.DialogListener{
    int btn_num = 0;
    Button photo_btn;
    ArrayList<Button> arrButton = new ArrayList<>();
    ArrayList<String> buttonNames;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loadData();
        photo_btn = findViewById(R.id.photo_btn);
        photo_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Profile.this,MainActivity.class);
                startActivity(intent);
            }
        });

        if(buttonNames != null)
        {
            for(int i=0;i<buttonNames.size();++i)
            {
                arrButton.add(makeButton(buttonNames.get(i)));
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            System.out.println("Received info");
            String desName = extras.getString("String");
            buttonNames.add(desName);
            arrButton.add(makeButton(desName));

        }

        for(int i=0;i<arrButton.size();++i)
        {
            final Button b = arrButton.get(i);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FileInputStream fis = null;
                    try {
                        fis = openFileInput(b.getText().toString());
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        ArrayList<ArrayList<Double>> savedPoints = (ArrayList<ArrayList<Double>>) ois.readObject();
                        ois.close();
                        openActivity3(savedPoints);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void openActivity3(ArrayList<ArrayList<Double>> savedPoints) {
        Intent intent = new Intent(this,Main3Activity.class);
        intent.putExtra("points", savedPoints);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.item:
                DialogRemove dialog = new DialogRemove();
                dialog.show(getSupportFragmentManager(),"dialog");
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void applyTexts(String removeName) {
        for(int i=0;i<arrButton.size();++i)
        {
            Button b = arrButton.get(i);
            if(b.getText().toString().equals(removeName))
            {
                ((ViewGroup) arrButton.get(i).getParent()).removeView(arrButton.get(i));
                File dir = getFilesDir();
                File file = new File(dir, removeName);
                boolean deleted = file.delete();
                arrButton.remove(i);
                buttonNames.remove(i);
                Toast.makeText(this,removeName+" is removed",Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        System.out.println("I am saving");
        outState.putStringArrayList("Button names",buttonNames);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(buttonNames);
        editor.putString("Button names",json);
        editor.apply();
        Log.d("STATE", "onPause called");
    }

    private void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences",MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("Button names",null);
        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        buttonNames = gson.fromJson(json,type);

        if(buttonNames==null){
            System.out.println("Button names null");
            buttonNames = new ArrayList<>();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("STATE", "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("STATE","onDestroy called");
    }

    private Button makeButton(String desName) {
        LinearLayout layout = findViewById(R.id.layout);
        //layout.setWeightSum(buttonNames.size());
        Button button = new Button(this);
        button.setBackgroundColor(getResources().getColor(R.color.colorGreenCyan));
        button.setBackgroundResource(R.drawable.profile_green);
        button.setText(desName);
        //button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        Typeface typeface = getResources().getFont(R.font.farsanregular);
        button.setTypeface(typeface);
        button.setTextSize(20);
        button.setTransformationMethod(null);
        button.setId(btn_num);
        button.setPadding(4,4,4,4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //params.setMargins(40,20,40,30);
        //params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if(btn_num==0)
            params.setMargins(40,80,40,30);
        else
            params.setMargins(40,20,40,30);
        button.setLayoutParams(params);
        layout.addView(button);
        ++btn_num;
        return button;
    }
}
