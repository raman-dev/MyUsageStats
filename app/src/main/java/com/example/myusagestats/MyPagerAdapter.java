package com.example.myusagestats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MyPagerAdapter extends FragmentPagerAdapter {

    public static final int NUM_PAGES = 3;

    public MyPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case 0:
                return "24 HOURS";
            case 1:
                return "7 DAYS";
            case 2:
                return "30 DAYS";
            default:
                return super.getPageTitle(position);
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0:
                return ChartFragment.newInstance(UsageStatsCollectorX.DAY_MS);
            case 1:
                return ChartFragment.newInstance(UsageStatsCollectorX.WEEK_MS);
            case 2:
                return ChartFragment.newInstance(UsageStatsCollectorX.THIRY_DAYS);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}
