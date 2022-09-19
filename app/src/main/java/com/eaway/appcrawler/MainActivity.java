package com.eaway.appcrawler;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    public static final String TAG = "AppCrawlerTool";

    private static final String APP_NAME = "name";
    private static final String APP_PKG = "pkg";
    private static final String APP_ICON = "icon";

    private static final String PKG_PREFIX_ANDROID = "com.android";
    private static final String PKG_PREFIX_GOOGLE = "com.google";

    private EditText editText;
    private ListView mListView;
    private RadioButton mRadioBtnPackage;
    private RadioButton mRadioBtnName;
    private CheckBox mCheckBoxHideAndroid;
    private CheckBox mCheckBoxHideGoogle;

    private PackageManager mPkgMgr;
    private PackageInfo mPkgInfo;

    public static List<PackageInfo> sPkgInfoList;
    public static List<TargetApp> sSelectedAppList;
    private ArrayList<HashMap<String, Object>> appList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPkgMgr = getPackageManager();
        try {
            mPkgInfo = mPkgMgr.getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        editText = (EditText) findViewById(R.id.search);
        mListView = (ListView) findViewById(R.id.appList);
        mRadioBtnPackage = (RadioButton) findViewById(R.id.radioPackage);
        mRadioBtnName = (RadioButton) findViewById(R.id.radioName);
        mCheckBoxHideAndroid = (CheckBox) findViewById(R.id.checkBoxHideAndroid);
        mCheckBoxHideGoogle = (CheckBox) findViewById(R.id.checkBoxHideGoogle);

        appList = new ArrayList<>();

        refreshAppListView(true);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                refreshAppListView(false);
                    ((SimpleAdapter) mListView.getAdapter()).getFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void onHideButtonClick(View view) {
        refreshAppListView(true);
    }

    public void onSortButtonClick(View view) {
        if (sPkgInfoList != null) {
            if (mRadioBtnPackage.isChecked()) {
                sPkgInfoList.sort((p1, p2) -> p1.packageName.compareTo(p2.packageName));
            } else {
                sPkgInfoList.sort((p1, p2) ->
                        ((String) p1.applicationInfo.loadLabel(mPkgMgr))
                                .compareTo(((String) p2.applicationInfo.loadLabel(mPkgMgr))));
            }
            refreshAppListView(false);
        }
    }



    public void onStartButtonClick(View view) {

        // Get selected app list
        sSelectedAppList = new ArrayList<>();
        for (int i = 0; i < mListView.getChildCount(); i++) {
            LinearLayout itemLayout = (LinearLayout) mListView.getChildAt(i);
            CheckBox cb = (CheckBox) itemLayout.findViewById(R.id.checkBox);
            if (cb.isChecked()) {
                TextView pkg = (TextView) itemLayout.findViewById(R.id.appPackage);
                TextView name = (TextView) itemLayout.findViewById(R.id.appName);
                TargetApp app = new TargetApp((String) name.getText(), (String) pkg.getText());
                sSelectedAppList.add(app);
            }
        }

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private boolean refreshAppListView(boolean force) {

        if(!force && mPkgMgr.getInstalledPackages(0).size() == appList.size() - 1)
            return refreshAppWithCriteria();

        // Get installed packages
        appList.clear();
        sPkgInfoList = mPkgMgr.getInstalledPackages(0);

        for (PackageInfo pkg : sPkgInfoList) {

            // Skip Ourself
            if (pkg.packageName.equalsIgnoreCase(getPackageName()))
                continue;

            // Skip Android packages
            if (mCheckBoxHideAndroid.isChecked()) {
                if (pkg.packageName.contains(PKG_PREFIX_ANDROID))
                    continue;
            }

            // Skip Google packages
            if (mCheckBoxHideGoogle.isChecked()) {
                if (pkg.packageName.contains(PKG_PREFIX_GOOGLE))
                    continue;
            }

            HashMap<String, Object> mapApp = new HashMap<>();
            mapApp.put(APP_PKG, pkg.packageName);
            mapApp.put(APP_NAME, pkg.applicationInfo.loadLabel(mPkgMgr));
            mapApp.put(APP_ICON, pkg.applicationInfo.loadIcon(mPkgMgr));
            appList.add(mapApp);
        }

        return refreshAppWithCriteria();
    }

    private boolean refreshAppWithCriteria(){
        String search = editText.getText().toString().toLowerCase();

        // Bind ListView with content adapter
        SimpleAdapter appAdapter = new SimpleAdapter(this, appList, R.layout.app_list_item,
                new String[] {
                        APP_NAME, APP_PKG, APP_ICON
                },
                new int[] {
                        R.id.appName, R.id.appPackage, R.id.appIcon
                });

        appAdapter.setViewBinder((view, data, textRepresentation) -> {
            if (view instanceof ImageView && data instanceof Drawable) {
                ImageView iv = (ImageView) view;
                iv.setImageDrawable((Drawable) data);
                return true;
            }
            else
                return false;
        });

        mListView.setAdapter(appAdapter);
        return true;
    }

    private ArrayList<HashMap<String, Object>> filter(){
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        for(HashMap<String, Object> app : appList) {
            if (!editText.getText().toString().equals("")) {
                String appName = app.get(APP_NAME).toString().toLowerCase();
                String text = editText.getText().toString().toLowerCase();
                if (appName.contains(text))
                    result.add(app);
            }else
                return appList;
        }
        return result;
    }

}