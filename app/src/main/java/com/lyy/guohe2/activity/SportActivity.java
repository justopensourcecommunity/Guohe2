package com.lyy.guohe2.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.flyco.animation.BounceEnter.BounceBottomEnter;
import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.MaterialDialog;
import com.lyy.guohe2.R;
import com.lyy.guohe2.adapter.SportAdapter;
import com.lyy.guohe2.constant.SpConstant;
import com.lyy.guohe2.model.Res;
import com.lyy.guohe2.model.Sport;
import com.lyy.guohe2.utils.HttpUtil;
import com.lyy.guohe2.utils.SpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SportActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;

    //定义listView
    private ListView listView;

    //定义list
    List<Sport> sportList = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    private TextView tv_sport_info;
    private TextView tv_sport_year;

    private SwipeRefreshLayout swipeRefreshLayout;

    private SwipeRefreshLayout.OnRefreshListener listener;

    //接收首页传来的url和type
    /**
     * type:1   俱乐部查询
     * type:2   早操出勤查询
     * type:3   体育成绩查询
     */
    private String url;
    private String type;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeStatusBar();
        setContentView(R.layout.activity_sport);

        mContext = this;

        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        type = intent.getStringExtra("type");

        //设置和toolbar相关的
        Toolbar toolbar = (Toolbar) findViewById(R.id.sport_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        ImageView sport_iv = (ImageView) findViewById(R.id.sport_iv);
        Glide.with(SportActivity.this).load(R.drawable.bg_sport).into(sport_iv);
        FloatingActionButton sport_floating_btn = (FloatingActionButton) findViewById(R.id.sport_floating_btn);
        sport_floating_btn.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.sport_list_view);
        tv_sport_info = (TextView) findViewById(R.id.tv_sport_info);
        tv_sport_year = (TextView) findViewById(R.id.tv_sport_year);

        initSwipeRefresh();
        getSportInfo();

    }

    private void initSwipeRefresh() {
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.sport_refresh);

        // 设置颜色属性的时候一定要注意是引用了资源文件还是直接设置16进制的颜色，因为都是int值容易搞混
        // 设置下拉进度的背景颜色，默认就是白色的
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white);
        // 设置下拉进度的主题颜色
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);

        listener = () -> {
            //TODO
            String username = SpUtils.getString(mContext, SpConstant.STU_ID);
            String pePass = SpUtils.getString(mContext, SpConstant.PE_PASS);
            getSportScore(username, pePass);
        };

        swipeRefreshLayout.setOnRefreshListener(listener);
    }

    private void getSportInfo() {
        String username = SpUtils.getString(mContext, SpConstant.STU_ID);
        String pePass = SpUtils.getString(mContext, SpConstant.PE_PASS);
        mProgressDialog = ProgressDialog.show(SportActivity.this, null, "数据导入中,请稍后……", true, false);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(true);

        if (pePass != null) {
            getSportScore(username, pePass);
        } else {
            mProgressDialog.dismiss();
            final EditText editText = new EditText(SportActivity.this);
            AlertDialog.Builder inputDialog =
                    new AlertDialog.Builder(SportActivity.this);
            inputDialog.setTitle("请输入你的体育学院密码(默认姓名首字母大写)").setView(editText);
            inputDialog.setPositiveButton("确定",
                    (dialog, which) -> {
                        mProgressDialog = ProgressDialog.show(SportActivity.this, null, "密码验证中,请稍后……", true, false);
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(true);
                        final String username1 = SpUtils.getString(mContext, SpConstant.STU_ID);
//                        String url = UrlConstant.CLUB_SCORE;
                        RequestBody requestBody = new FormBody.Builder()
                                .add("username", username1)
                                .add("password", editText.getText().toString())
                                .build();
                        HttpUtil.post(url, requestBody, new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> {
                                    if (mProgressDialog.isShowing())
                                        mProgressDialog.dismiss();
                                    Toasty.error(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    String data = response.body().string();
                                    Res res = HttpUtil.handleResponse(data);
                                    assert res != null;
                                    if (res.getCode() == 200) {
                                        runOnUiThread(() -> {
                                            mProgressDialog.dismiss();
                                            mProgressDialog = ProgressDialog.show(SportActivity.this, null, "验证成功,请稍后……", true, false);
                                            mProgressDialog.setCancelable(true);
                                            mProgressDialog.setCanceledOnTouchOutside(true);
                                            getSportScore(username1, editText.getText().toString());
                                            SpUtils.putString(mContext, SpConstant.PE_PASS, editText.getText().toString());
                                        });
                                    } else {
                                        Looper.prepare();
                                        if (mProgressDialog.isShowing())
                                            mProgressDialog.dismiss();
                                        Toasty.error(mContext, res.getMsg(), Toast.LENGTH_SHORT).show();
                                        Looper.loop();
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        if (mProgressDialog.isShowing())
                                            mProgressDialog.dismiss();
                                        Toasty.error(mContext, "服务器发生异常，请稍后重试", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });
                    }).show();
        }
    }

    private void getSportScore(String username, String pePass) {
        sportList.clear();
        listView.setVisibility(View.GONE);
//        String url = UrlConstant.CLUB_SCORE;
        RequestBody requestBody = new FormBody.Builder()
                .add("username", username)
                .add("password", pePass)
                .build();

        if (type.equals("1")) {
            HttpUtil.post(url, requestBody, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        if (mProgressDialog.isShowing())
                            mProgressDialog.dismiss();
                        Toasty.error(mContext, "服务器异常，请稍后重试", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String data = response.body().string();
                        Res res = HttpUtil.handleResponse(data);
                        assert res != null;
                        if (res.getCode() == 200) {
                            try {
                                JSONArray array = new JSONArray(res.getInfo());
                                JSONObject object = array.getJSONObject(0);
                                final String year = object.getString("year");
                                String name = object.getString("name");
                                String total = object.getString("total");
                                final String sum = object.getString("sum");

                                SpUtils.putString(mContext, "sport_info", total);

                                JSONArray innerArray = array.getJSONArray(1);
                                for (int i = 0; i < innerArray.length(); i++) {
                                    JSONObject innerObject = innerArray.getJSONObject(i);
                                    String number = innerObject.getString("number");
                                    String date = innerObject.getString("date");
                                    String time = innerObject.getString("time");

                                    Sport sport = new Sport(time, number, date);
                                    sportList.add(sport);
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String temp = sum.substring(sum.indexOf(" ") + 1, sum.lastIndexOf(" "));
                                        tv_sport_info.setText(temp.substring(0, temp.indexOf("(")));
                                        String[] s = year.split(" ");
                                        tv_sport_year.setText(s[1]);

                                        SportAdapter sportAdapter = new SportAdapter(SportActivity.this, R.layout.item_sport, sportList);
                                        if (sportList.size() == 0) {
                                            Toasty.warning(mContext, "列表数据为空", Toast.LENGTH_SHORT).show();
                                        }
                                        listView.setAdapter(sportAdapter);
                                        listView.setVisibility(View.VISIBLE);
                                        mProgressDialog.dismiss();
                                        swipeRefreshLayout.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                swipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                    }
                                });

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Looper.prepare();
                            if (mProgressDialog.isShowing())
                                mProgressDialog.dismiss();
                            swipeRefreshLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                            Toasty.error(mContext, res.getMsg(), Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mProgressDialog.isShowing())
                                    mProgressDialog.dismiss();
                                Toasty.error(mContext, "错误" + response.code() + "，请稍后重试", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } else if (type.equals("2")) {
            HttpUtil.post(url, requestBody, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        if (mProgressDialog.isShowing())
                            mProgressDialog.dismiss();
                        Toasty.error(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String data = response.body().string();
                        if (data.length() > 3) {
                            Res res = HttpUtil.handleResponse(data);
                            assert res != null;
                            if (res.getCode() == 200) {
                                try {
                                    JSONArray array = new JSONArray(res.getInfo());
                                    JSONObject object = array.getJSONObject(0);
                                    final String year = object.getString("year");
                                    String name = object.getString("name");
                                    final String total = object.getString("total");

                                    SpUtils.putString(mContext, "exercise_info", name + "\n" + total);

                                    JSONArray innerArray = array.getJSONArray(1);
                                    for (int i = 0; i < innerArray.length(); i++) {
                                        JSONObject innerObject = innerArray.getJSONObject(i);
                                        String number = innerObject.getString("number");
                                        String date = innerObject.getString("date");
                                        String time = innerObject.getString("time");

                                        Sport exercise = new Sport(time, number, date);
                                        sportList.add(exercise);
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String[] s = year.split(" ");
                                            tv_sport_year.setText(s[1]);

                                            String[] s1 = total.split(" ");
                                            tv_sport_info.setText(s1[1] + s1[2]);

                                            SportAdapter exerciseAdapter = new SportAdapter(SportActivity.this, R.layout.item_sport, sportList);
                                            if (sportList.size() == 0) {
                                                Toasty.warning(mContext, "列表数据为空", Toast.LENGTH_SHORT).show();
                                            }
                                            listView.setAdapter(exerciseAdapter);
                                            listView.setVisibility(View.VISIBLE);
                                            mProgressDialog.dismiss();
                                            swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(false));
                                        }
                                    });

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Looper.prepare();
                                if (mProgressDialog.isShowing())
                                    mProgressDialog.dismiss();
                                swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(false));
                                Toasty.error(mContext, res.getMsg(), Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (mProgressDialog.isShowing())
                                mProgressDialog.dismiss();
                            Toasty.error(mContext, "错误" + response.code() + "，请稍后重试", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        }

    }

    private void showMaterialDialogDefault(String msg) {
        final MaterialDialog dialog = new MaterialDialog(SportActivity.this);
        dialog.content(msg)//
                .btnText("取消", "确定")//
                .showAnim(new BounceBottomEnter())//
                .show();

        dialog.setOnBtnClickL(
                new OnBtnClickL() {//left btn click listener
                    @Override
                    public void onBtnClick() {
                        dialog.dismiss();
                    }
                },
                new OnBtnClickL() {//right btn click listener
                    @Override
                    public void onBtnClick() {
                        dialog.dismiss();
                    }
                }
        );
    }

    //将背景图和状态栏融合到一起
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeStatusBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sport_floating_btn:
                String msg = SpUtils.getString(mContext, "sport_info");
                showMaterialDialogDefault(msg);
                break;
        }
    }
}