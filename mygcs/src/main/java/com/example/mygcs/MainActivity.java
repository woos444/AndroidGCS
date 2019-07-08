package com.example.mygcs;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    boolean a = true;
    NaverMap naverMap;
    MapFragment mNaverMapFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        mNaverMapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mNaverMapFragment == null) {
            mNaverMapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mNaverMapFragment).commit();
        }
        mNaverMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        naverMap.setMapType(NaverMap.MapType.Hybrid);
        //마커생성
        final Marker ksU_M = new Marker();
        final Marker ksS_M = new Marker();
        final Marker jbnu_M = new Marker();
        final Marker wku_M = new Marker();


        final InfoWindow kunU = new InfoWindow();
        kunU.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "군산대";
            }
        });

        final InfoWindow kunS = new InfoWindow();
        kunS.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "군산시청";
            }
        });
        final InfoWindow jbU = new InfoWindow();
        jbU.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "전북대";
            }
        });
        final InfoWindow wkU = new InfoWindow();
        wkU.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "원광대";
            }
        });

        final InfoWindow Lal = new InfoWindow();
        Lal.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {

                String point = Lal.getPosition().latitude + " , " + Lal.getPosition().longitude;

                return point;
            }
        });


        //좌표설정

        LatLng kunsanU = new LatLng(35.945508, 126.682185);
        LatLng kunsanS = new LatLng(35.967672, 126.736811);
        LatLng jbnu = new LatLng(35.846976, 127.129410);
        LatLng wku = new LatLng(35.969457, 126.957314);

        LatLngBounds bounds1 = new LatLngBounds(jbnu, kunsanU);
        LatLngBounds bounds2 = new LatLngBounds(wku, kunsanS);
        LatLngBounds bounds = new LatLngBounds(bounds1.getCenter(), bounds2.getCenter());

        CameraPosition cameraPosition = new CameraPosition(bounds.getCenter(), 9);//카메라중앙설정 및 줌값 설정
        naverMap.setCameraPosition(cameraPosition);//네이버 맵에 카메라설정값 적용

        //마커위치 등록
        ksU_M.setPosition(kunsanU);
        ksS_M.setPosition(kunsanS);
        jbnu_M.setPosition(jbnu);
        wku_M.setPosition(wku);

        //마커끼리 선연결
        final PathOverlay path = new PathOverlay();
        path.setCoords(Arrays.asList(kunsanU, kunsanS, wku, jbnu));
        path.setWidth(30);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (a == true) {
                    //네이버 맵에 마커 표시
                    ksU_M.setMap(naverMap);
                    ksS_M.setMap(naverMap);
                    jbnu_M.setMap(naverMap);
                    wku_M.setMap(naverMap);
                    path.setMap(naverMap);

                    //마커에 설명문 추가
                    kunU.open(ksU_M);
                    kunS.open(ksS_M);
                    jbU.open(jbnu_M);
                    wkU.open(wku_M);

                    button.setText("on");
                    a = false;
                } else if (a == false) {
                    ksU_M.setMap(null);
                    ksS_M.setMap(null);
                    jbnu_M.setMap(null);
                    wku_M.setMap(null);

                    path.setMap(null);

                    button.setText("off");
                    a = true;
                }

                naverMap.setOnMapLongClickListener((point , crood)->{
                    LatLng la=crood;
                    Lal.setPosition(la);
                    Lal.open(naverMap);
                });

            }
        });
    }

}













