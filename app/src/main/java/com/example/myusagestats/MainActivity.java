package com.example.myusagestats;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int USAGE_PAGE = 1;
    public static final int APP_PAGE = 2;
    public static final String PAGE_KEY = "com.example.myusagestats.PAGE_KEY";


    private ArrayList<AppInfoWrapper> list;
    private CustomListAdapter listAdapter;
    private RecyclerView recyclerView;

    private ViewPager viewPager;
    private MyPagerAdapter myPagerAdapter;
    private ConstraintLayout tabParent;

    private MenuItem showByUsageItem;
    private MenuItem showByAppItem;

    private int currentPage = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate!");
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        currentPage = sharedPref.getInt(PAGE_KEY,USAGE_PAGE);
        if(currentPage == USAGE_PAGE){
            System.out.println("CURRENT_PAGE => USAGE_PAGE");
        }else{
            System.out.println("CURRENT_PAGE => APP_PAGE");
        }

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        tabParent = findViewById(R.id.tabParent);
        myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager = findViewById(R.id.viewPager);

        list = new ArrayList<>();
        listAdapter = new CustomListAdapter(list);
        recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(listAdapter);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);
        showByUsageItem = menu.getItem(0);
        showByAppItem = menu.getItem(1);
        if(currentPage == USAGE_PAGE){
            System.out.println("USAGE_PAGE => VISIBLE");
            showByUsageItem.setChecked(true);
            showByAppItem.setChecked(false);

            tabParent.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }else{
            System.out.println("APP_PAGE => VISIBLE");
            showByAppItem.setChecked(true);
            showByUsageItem.setChecked(false);

            recyclerView.setVisibility(View.VISIBLE);
            tabParent.setVisibility(View.GONE);

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.showByUsage:
                //if user clicks this item then it will become checked
                //if already checked then do nothing
                if(!showByUsageItem.isChecked()){
                    showByUsageItem.setChecked(true);
                    showByAppItem.setChecked(false);

                    currentPage = USAGE_PAGE;

                    tabParent.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
                return true;
            case R.id.showByApp:
                if(!showByAppItem.isChecked()){
                    showByAppItem.setChecked(true);
                    showByUsageItem.setChecked(false);

                    currentPage = APP_PAGE;

                    tabParent.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    listAdapter.notifyDataSetChanged();
                    recyclerView.invalidate();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            if(list.size() == 0) {
                new GetAppListTask(getPackageManager()).execute();
            }
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
        getPreferences(Context.MODE_PRIVATE).edit().putInt(PAGE_KEY,currentPage).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy!");
    }

    private class GetAppListTask extends AsyncTask<Void,AppInfoWrapper,Void>{
        private PackageManager packageManager;

        public GetAppListTask(PackageManager packageManager){
            this.packageManager = packageManager;
        }

        @Override
        protected void onProgressUpdate(AppInfoWrapper... values) {
            //what to do now?
            list.add(values[0]);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            apps.forEach(x ->{
                if((x.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
                    String isSystem = x.sourceDir.split("/")[1];
                    if (!isSystem.equals("system") && !x.packageName.equals("com.sec.android.launcher")) {
                        publishProgress(new AppInfoWrapper((String) packageManager.getApplicationLabel(x), x.packageName, packageManager.getApplicationIcon(x)));
                    }
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            listAdapter.notifyDataSetChanged();
        }
    }

}
