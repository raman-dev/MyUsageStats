package com.example.myusagestats;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ViewPager viewPager;
    private MyPagerAdapter myPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate!");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager = findViewById(R.id.viewPager);
        //viewPager.setAdapter(myPagerAdapter);
        //viewPager.setOffscreenPageLimit(MyPagerAdapter.NUM_PAGES);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        tabs.bringToFront();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,"onResume!");


        AppOpsManager appOpsManager = (AppOpsManager)getSystemService(APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),getPackageName());
        boolean granted = false;
        if(mode == AppOpsManager.MODE_DEFAULT){
            granted = (checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        }else{
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        if(!granted){
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }else{
            viewPager.setAdapter(myPagerAdapter);
            viewPager.setOffscreenPageLimit(MyPagerAdapter.NUM_PAGES);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"onPause!");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy!");
    }



}
