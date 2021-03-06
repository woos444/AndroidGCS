package com.example.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import com.MAVLink.common.msg_rc_channels_override;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback , DroneListener, TowerListener, LinkListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    TableRow InfoWindow;
    RecyclerView NotificationWindow;

    boolean connectconfirm = false;//드론연결 여부
    boolean pointsetorder = true;
    boolean MapLock = true; //드론위치로 맵 잠금 on,off
    boolean cadastralmap = false; //지적도 on,off
    boolean missionstartwhether = true;//미션시작여부
    boolean missionstartready = false;

    Marker droneMarker = new Marker();//드론의 위치마커

    Marker goalMarker = new Marker();//드론의 이동 목적지 마커

    int connecttype = 1; // 0: USB텔레메트리로 연결 , 1: Wifi모듈 연결

    //변경되는값
    int changmaptype = 0; //0: 위성지도 , 1: 지형도  , 2: 일반지도
    int dronecontroltype = 0; //0: joystick모드  , 1: GCS모드
    int changemissionmode = 0; //0: 일반모드 , 1: 경로비행 , 2: 간격감시 , 3: 면적감시
    int missionC=0;//미션의 진행상황    0: 미션등록대기 , 1: 미션등록완료  , 2: 미션시작
    int waypointCount=0;

    int distancevalue=0;
    int intervalvalue=0;

    //조이스틱 조작시 드론의 속도
    double speedYaw =1.25;
    double speedUpDown = 1.5;
    double speedMove = 1.25;

    Marker missionpoint_A = new Marker();
    Marker missionpoint_B = new Marker();
    Marker addmissionpoint_A = new Marker();
    Marker addmissionpoint_B = new Marker();

    LatLng dronelocation; //드론의 위치좌표
    LatLng Home_A;
    LatLongAlt My_A;
    Marker Home_M = new Marker();

    private RecyclerViewAdapter adapter;
    ArrayList<String> listTitle = new ArrayList<>();

    double altitudevalue=3;//고도설정값

    double distancepoint =0;

    PolylineOverlay Square_line= new PolylineOverlay();
    PolylineOverlay routeline = new PolylineOverlay();
    PolylineOverlay dronepathdisplay= new PolylineOverlay();//드론의 이동경로 표시선

    Mission dronemission = new Mission();

    ArrayList<LatLng> missionpointlist = new ArrayList();//
    ArrayList<LatLng> temporaryPoint = new ArrayList();
    ArrayList<LatLng> movingpoint = new ArrayList();//드론의 지나온 이동 포인트
    ArrayList<LatLng> missionroutelist = new ArrayList();//미션에 들어갈 포인트 리스트

    ArrayList<Marker> waypointmarkerlist = new ArrayList();

    PolygonOverlay missionpolygon = new PolygonOverlay();

    private double yawvalue;
    private Spinner modeSelector;

    NaverMap naverMap;
    MapFragment mNaverMapFragment = null;

    //rc컨트롤러
    msg_rc_channels_override rc_override;

    //조이스틱
    RelativeLayout layout_Leftjoystick,layout_Rightjoystick;
    JoyStickClass jstickLeft,jstickRight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                ((TextView)parent.getChildAt(0)).setTextColor(Color.WHITE);
                ((TextView)parent.getChildAt(0)).setTextSize(10);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        mNaverMapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mNaverMapFragment == null) {
            mNaverMapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mNaverMapFragment).commit();
        }
        mNaverMapFragment.getMapAsync(this);

        NotificationWindow=(RecyclerView)findViewById(R.id.recyclerView);
        NotificationWindow.bringToFront();
        //////
        rc_override = new msg_rc_channels_override();
        rc_override.chan1_raw = 1500; //right; 2000 //left
        rc_override.chan2_raw = 1500;
        rc_override.chan3_raw = 1500; //back; 2000 //forward
        rc_override.chan4_raw = 1500;

        rc_override.target_system = 0;
        rc_override.target_component = 0;

        joystick();
    }

    private void rcv_init() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(adapter);
    }
    private void rcv_getData() {
        for (int i = 0 ;i < listTitle.size();i++) {
            recyclerview_Data data = new recyclerview_Data();
            // 각 List의 값들을 data 객체에 set 해줍니다.
            //recyclerview_Data data = new recyclerview_Data();
            data.setTitle(listTitle.get(i));
            data.setResId(R.drawable.icons8_star_64px);
            // 각 값이 들어간 data를 adapter에 추가합니다.
            adapter.addItem(data);
            // adapter의 값이 변경되었다는 것을 알려줍니다.
        }
        adapter.notifyDataSetChanged();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(listTitle.size()==3)
                {
                    listTitle.set(0,listTitle.get(1));
                    listTitle.set(1,listTitle.get(2));
                    listTitle.remove(2);
                }
                else  if(listTitle.size()==2)
                {
                    listTitle.set(0,listTitle.get(1));
                    listTitle.remove(1);
                }
                else  if(listTitle.size()==1)
                {
                    listTitle.remove(0);
                }

                rcv_init();
                for (int i = 0 ;i < listTitle.size();i++) {
                    recyclerview_Data data = new recyclerview_Data();// 각 List의 값들을 data 객체에 set 해줍니다.
                    data.setTitle(listTitle.get(i));
                    data.setResId(R.drawable.icons8_star_64px);// 각 값이 들어간 data를 adapter에 추가합니다.
                    adapter.addItem(data);// adapter의 값이 변경되었다는 것을 알려줍니다.
                }
                adapter.notifyDataSetChanged();

            }
        },30000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        this.naverMap=naverMap;
        naverMap.setMapType(NaverMap.MapType.Satellite);
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);


    }
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        setTakeoffAltitude();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }
    @Override
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("드론연결");
                updateConnectedButton(this.drone.isConnected());
                connectconfirm = true;
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("드론분리");
                updateConnectedButton(this.drone.isConnected());
                connectconfirm = false;
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                //수정
                //updateSpeed();
                break;

            case AttributeEvent.GUIDED_POINT_UPDATED:
                MissionSelect();
                break;

            case AttributeEvent.MISSION_SENT:
                alertUser("미션전송완료");
                missionstartready = true;
                break;
            case AttributeEvent.MISSION_ITEM_REACHED:
                waypointCount++;
                MissionCount();
                break;


            case AttributeEvent.ALTITUDE_UPDATED:
                //updateDistanceFromHome();
                updateAltitude();
                break;
            case AttributeEvent.GPS_COUNT:
                updateSatellitesnum();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;
            case AttributeEvent.HOME_UPDATED:
               // updateDistanceFromHome();
                break;
            case AttributeEvent.GPS_POSITION:

                updateDroneLocation();
                //updateHomeLatLng();
               // updateDistanceFromHome();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYAW();
                break;

            default:
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
    }
    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }
    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                break;
        }

    }
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }
    @Override
    public void onTowerDisconnected() { }




    /////////////////////////////
    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }//드론과연결
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setBackgroundResource(R.drawable.dronelending);
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setBackgroundResource(R.drawable.dronetakeoff);
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setBackgroundResource(R.drawable.dronearming);
        }
    }//시동
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getRelativeAltitude()) + "m");
    }//고도
    protected void updateSatellitesnum() {
        TextView SatelliteTextView = (TextView) findViewById(R.id.SatellitesnumTextView);
        Gps num = this.drone.getAttribute(AttributeType.GPS);
        SatelliteTextView.setText(String.format("%d", num.getSatellitesCount()) );
    }//위성수
    protected void updateVoltage() {
        TextView VoltageTextView = (TextView) findViewById(R.id.VoltageValueTextView);
        Battery Volt = this.drone.getAttribute(AttributeType.BATTERY);
        VoltageTextView.setText(String.format("%.1f", Volt.getBatteryVoltage())+"V" );
    }//전압
    protected void updateYAW() {
        TextView YAWTextView = (TextView) findViewById(R.id.YAWValueTextView);
        Attitude YAW = this.drone.getAttribute(AttributeType.ATTITUDE);
        yawvalue = YAW.getYaw();
        YAWTextView.setText(String.format("%.0f", yawvalue)+"deg");
    }//드론의방향


    protected void update() {
        //속도함수
    /*protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }*/


        //거리함수
    /*protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);

        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getRelativeAltitude();

        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }
        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }


    protected void updateDistanceFromMe() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
        naverMap.addOnLocationChangeListener(location -> My_A = new LatLongAlt(location.getLatitude(),location.getLongitude(),0));

        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getRelativeAltitude();

        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromMe = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            distanceFromMe = distanceBetweenPoints(My_A, vehicle3DPosition);
        } else {
            distanceFromMe = 0;
        }
        distanceTextView.setText(String.format("%3.1f", distanceFromMe) + "m");
    }
    protected void updateHomeLatLng(){

        Home My_H =this.drone.getAttribute(AttributeType.HOME);
        LatLong HomePosition = My_H.getCoordinate();
        Home_A = new LatLng(HomePosition.getLatitude(),HomePosition.getLongitude());

        Home_M.setIcon(OverlayImage.fromResource(R.drawable.ethereum_48px));
        Home_M.setHeight(70);
        Home_M.setWidth(50);
        Home_M.setPosition(Home_A);
        Home_M.setMap(naverMap);
    }
*/
    }//개발 보류중인 함수

    protected void updateDroneLocation(){
        float droneAngle = 0;

        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong GpsLocation = droneGps.getPosition();
        dronelocation = change_LongLng(GpsLocation);

        //아이콘이 바라보는 방향 조절
        if(yawvalue>=0){ droneAngle =(float) yawvalue;}
        else if(yawvalue<0) {droneAngle = (float)(180+(180+yawvalue)); }

        //드론의 이동경로 표시
        movingpoint.add(dronelocation);

        dronepathdisplay.setCoords(movingpoint);
        dronepathdisplay.setWidth(20);
        dronepathdisplay.setColor(Color.YELLOW);
        dronepathdisplay.setJoinType(PolylineOverlay.LineJoin.Round);


        //드론의 위치 표시
        droneMarker.setPosition(dronelocation);
        droneMarker.setAngle(droneAngle);
        droneMarker.setIcon(OverlayImage.fromResource(R.drawable.fighter_jet_24px));
        droneMarker.setFlat(true);
        droneMarker.setHeight(300);
        droneMarker.setWidth(100);
        droneMarker.setAnchor(new PointF((float)0.5,(float)0.85));
        droneMarker.setMap(naverMap);
        dronepathdisplay.setMap(naverMap);

        //맵잠금시 드론위치로 화면 고정
        if (MapLock==true) { naverMap.moveCamera(null); }
        else if(MapLock==false)
        {
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(dronelocation);
            naverMap.moveCamera(cameraUpdate);
        }


    }//드론의 위치 및 이동경로 표시

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }
    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }
    protected void alertUser(String message) {
        if (listTitle.size() <= 2) {listTitle.add(message);}
        else if(listTitle.size() > 2){
            listTitle.set(0,listTitle.get(1));
            listTitle.set(1,listTitle.get(2));
            listTitle.set(2,message);
        }
        rcv_init();
        rcv_getData();
    }//리사이클러뷰 안내문 출력

    protected void setTakeoffAltitude(){
        Button altitudeset = (Button)findViewById(R.id.btnAltitudeSet);
        String altitudetext = (int)altitudevalue+"m\n이륙고도";
        final SpannableStringBuilder sps = new SpannableStringBuilder(altitudetext);
        sps.setSpan(new AbsoluteSizeSpan(30),1,3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        altitudeset.setText(sps);

    }//이륙고도설정
    public void onTakeoffALTap(View view){
        RelativeLayout tackoffaltitudeset  = (RelativeLayout)findViewById(R.id.TackOffAltitudeSet);
        Button altitudeup = (Button)findViewById(R.id.btnAltitudeUP);
        Button altitudedown = (Button)findViewById(R.id.btnAltitudeDOWN);

        if (tackoffaltitudeset.getVisibility()==View.INVISIBLE)
        {
            tackoffaltitudeset.setVisibility(View.VISIBLE);
        }
        else if (tackoffaltitudeset.getVisibility()==View.VISIBLE)
        {
            tackoffaltitudeset.setVisibility(View.INVISIBLE);
        }

        altitudeup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                altitudevalue += 1;
                setTakeoffAltitude();
                alertUser(altitudevalue+"m 고도");
                //if(비행중일경우)--ControlApi.getApi(this.drone).
                //
            }
        });
        altitudedown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(altitudevalue>0){altitudevalue -= 1;}
                setTakeoffAltitude();
                alertUser(altitudevalue+"m 고도");
            }
        });

        ControlApi.getApi(this.drone).climbTo(5);




    }//이륙 고도값 변경


    protected void MissionSelect() {

        naverMap.setOnMapLongClickListener((pointF, latLng) -> {

            //기본모드 - 목적지 터치시 이동
            if(changemissionmode == 0){

                //터치한곳에 깃발표시
                goalMarker.setPosition(latLng);
                goalMarker.setIcon(OverlayImage.fromResource(R.drawable.empty_flag_64px));
                goalMarker.setHeight(70);
                goalMarker.setWidth(70);
                goalMarker.setMap(naverMap);
                ControlApi.getApi(this.drone).goTo(change_LngLong(latLng), true, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() { alertUser("출발"); }
                    @Override
                    public void onError(int executionError) { alertUser("실패"); }
                });
            }

            //ㄹ경로를 만들고 경로를 따라 주행
            else if (changemissionmode == 1){
                if (missionstartwhether == true){

                    ResetValue();
                    missionpointlist.clear();
                    pointsetorder=true;
                    dronemission.clear();

                    missionpointlist.add(latLng);
                    missionpoint_A.setPosition(missionpointlist.get(0));
                    missionpoint_A.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    missionpoint_A.setMap(naverMap);

                    missionstartwhether = false;
                }
                else if (missionstartwhether == false) {
                    missionpointlist.add(latLng);
                    missionpoint_B.setPosition(missionpointlist.get(1));
                    missionpoint_B.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));
                    missionpoint_B.setMap(naverMap);
                    missionstartwhether = true;

                    PathSetting();
                }
            }

            //여러개의 웨이포인트를 따라 주행
            else if (changemissionmode == 2){
                if(missionstartwhether == true) {

                    missionpointlist.clear();
                    pointsetorder = true;
                    dronemission.clear();

                    missionroutelist.add(latLng);
                    Marker waypoint = new Marker();
                    waypoint.setPosition(latLng);
                    waypoint.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    waypoint.setCaptionText(missionroutelist.size()+"");

                    waypointmarkerlist.add(waypoint);
                    waypointmarkerlist.get(waypointmarkerlist.size()-1).setMap(naverMap);

                    missionstartwhether = false;
                }
                else if(missionstartwhether == false) {

                    missionroutelist.add(latLng);
                    Marker waypoint = new Marker();
                    waypoint.setPosition(latLng);
                    waypoint.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    waypoint.setCaptionText(missionroutelist.size()+"");

                    waypointmarkerlist.add(waypoint);
                    waypointmarkerlist.get(waypointmarkerlist.size()-1).setMap(naverMap);


                    routeline.setCoords(missionroutelist);
                    routeline.setWidth(10);
                    routeline.setColor(Color.GREEN);
                    routeline.setMap(naverMap);

                }


            }
            //면적을 설정하고 경로를 생성하여 주행
            else if (changemissionmode == 3){
                if(missionstartwhether == true) {

                    missionpointlist.clear();
                    pointsetorder = true;
                    dronemission.clear();
                    missionpolygon.setMap(null);

                    missionpointlist.add(latLng);
                    Marker waypoint = new Marker();
                    waypoint.setPosition(latLng);
                    waypoint.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    waypoint.setCaptionText(missionpointlist.size()+"");

                    waypointmarkerlist.add(waypoint);
                    waypointmarkerlist.get(waypointmarkerlist.size()-1).setMap(naverMap);

                    missionstartwhether = false;
                }
                else if(missionstartwhether == false) {

                    missionpointlist.add(latLng);
                    Marker waypoint = new Marker();
                    waypoint.setPosition(latLng);
                    waypoint.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    waypoint.setCaptionText(missionpointlist.size()+"");

                    waypointmarkerlist.add(waypoint);
                    waypointmarkerlist.get(waypointmarkerlist.size()-1).setMap(naverMap);

                    if(missionpointlist.size()>2){
                        missionpolygon.setCoords(missionpointlist);
                        missionpolygon.setOutlineWidth(5);
                        missionpolygon.setOutlineColor(Color.GREEN);
                        missionpolygon.setMap(naverMap);
                    }


                }


            }

        });
    }//드론미션 생성

    public void PathSetting() {


        //간격값 설정창
        final List<String> intervalList = new ArrayList<>();
        intervalList.add("3");
        intervalList.add("5");
        intervalList.add("10");
        intervalList.add("20");
        final CharSequence[] items2 =  intervalList.toArray(new String[ intervalList.size()]);

        final List SelectedItems2  = new ArrayList();
        int defaultItem2 = 0;
        SelectedItems2.add(defaultItem2);
        AlertDialog.Builder spacingsetting = new AlertDialog.Builder(this);

        //거리값 설정창
        final List<String> distanceList = new ArrayList<>();
        distanceList.add("10");
        distanceList.add("20");
        distanceList.add("50");
        distanceList.add("100");
        final CharSequence[] items1 =  distanceList.toArray(new String[ distanceList.size()]);

        final List SelectedItems1  = new ArrayList();
        int defaultItem1 = 0;
        SelectedItems1.add(defaultItem1);


        //거리설정
        AlertDialog.Builder distancesetting = new AlertDialog.Builder(this);
        distancesetting.setTitle("거리설정");
        distancesetting.setSingleChoiceItems(items1, defaultItem1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems1.clear();
                        SelectedItems1.add(which);
                    }
                });
        distancesetting.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg1="";

                        if (!SelectedItems1.isEmpty()) {
                            int index = (int) SelectedItems1.get(0);
                            msg1 = distanceList.get(index);
                            distancevalue = Integer.parseInt(msg1);
                            spacingsetting.show();
                        }
                    }
                });
        distancesetting.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });


        //간격설정
        spacingsetting.setTitle("간격 설정");
        spacingsetting.setSingleChoiceItems(items2, defaultItem2,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems2.clear();
                        SelectedItems2.add(which);
                    }
                });
        spacingsetting.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg2="";

                        if (!SelectedItems2.isEmpty()) {
                            int index = (int) SelectedItems2.get(0);
                            msg2 = intervalList.get(index);
                            intervalvalue = Integer.parseInt(msg2);
                            Log.i("test1","거리"+distancevalue);
                            Log.i("test2","간격"+intervalvalue);

                            for(int i=0 ; i<=distancevalue ; i++)
                            {
                                if(i%intervalvalue==0) {
                                    LatLong n = MathUtils.newCoordFromBearingAndDistance(change_LngLong(missionpointlist.get(0)), 90 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(missionpointlist.get(0)), change_LngLong(missionpointlist.get(1))), i);
                                    LatLong m = MathUtils.newCoordFromBearingAndDistance(change_LngLong(missionpointlist.get(1)), 90 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(missionpointlist.get(0)), change_LngLong(missionpointlist.get(1))), i);
                                    if (pointsetorder == true) {
                                        missionroutelist.add(change_LongLng(n));
                                        missionroutelist.add(change_LongLng(m));
                                        pointsetorder = false;
                                    } else if (pointsetorder == false) {
                                        missionroutelist.add(change_LongLng(m));
                                        missionroutelist.add(change_LongLng(n));
                                        pointsetorder = true;
                                    }
                                }

                            }


                            addmissionpoint_A.setPosition(missionroutelist.get(missionroutelist.size()-1));
                            addmissionpoint_A.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));
                            addmissionpoint_B.setPosition(missionroutelist.get(missionroutelist.size()-2));
                            addmissionpoint_B.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));

                            addmissionpoint_B.setMap(naverMap);
                            addmissionpoint_A.setMap(naverMap);

                            alertUser("A to B = "+(int)MathUtils.getDistance2D(change_LngLong(missionpointlist.get(0)),change_LngLong(missionpointlist.get(1)))+"m");

                            routeline.setCoords(missionroutelist);
                            routeline.setWidth(10);
                            routeline.setColor(Color.GREEN);
                            routeline.setMap(naverMap);

                        }
                    }
                });
        spacingsetting.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        distancesetting.show();

    }//ㄹ경로주행
    public void PathSett(){

        //간격값 설정창
        final List<String> intervalList = new ArrayList<>();
        intervalList.add("3");
        intervalList.add("5");
        intervalList.add("10");
        intervalList.add("20");
        final CharSequence[] items2 =  intervalList.toArray(new String[ intervalList.size()]);

        final List SelectedItems2  = new ArrayList();
        int defaultItem2 = 0;
        SelectedItems2.add(defaultItem2);
        AlertDialog.Builder spacingsetting = new AlertDialog.Builder(this);

        spacingsetting.setTitle("간격 설정");
        spacingsetting.setSingleChoiceItems(items2, defaultItem2,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems2.clear();
                        SelectedItems2.add(which);
                    }
                });
        spacingsetting.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg2="";

                        if (!SelectedItems2.isEmpty()) {
                            int index = (int) SelectedItems2.get(0);
                            msg2 = intervalList.get(index);
                            intervalvalue = Integer.parseInt(msg2);

                            for (int i = 0; i < missionpointlist.size(); i++) {
                                if (i == missionpointlist.size() - 1) { distancepoint = distanceBetweenPoints(change_LngLong(missionpointlist.get(i)), change_LngLong(missionpointlist.get(0)))*1000; }//두 좌표 사이의 거리를 계산
                                else { distancepoint = distanceBetweenPoints(change_LngLong(missionpointlist.get(i)), change_LngLong(missionpointlist.get(i + 1)))*1000; }// 두 좌표 사이의 거리를 계산

                                for(int j = 0 ; j < distancepoint ; j ++ ) {
                                    if(j%intervalvalue ==0) {
                                        LatLong routepoint;
                                        if(i == missionpointlist.size() - 1) {
                                            routepoint = MathUtils.newCoordFromBearingAndDistance(change_LngLong(missionpointlist.get(i)), 0 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(missionpointlist.get(i)), change_LngLong(missionpointlist.get(0))), j);

                                            double dis = distanceBetweenPoints(change_LngLong(missionpointlist.get(i)), routepoint)*1000;


                                            if(dis > distancepoint)
                                            {routepoint = change_LngLong(missionpointlist.get(0));}
                                        }
                                        else{
                                            routepoint = MathUtils.newCoordFromBearingAndDistance(change_LngLong(missionpointlist.get(i)), 0 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(missionpointlist.get(i)), change_LngLong(missionpointlist.get(i+1))), j);
                                            double dis = distanceBetweenPoints(change_LngLong(missionpointlist.get(i)), routepoint)*1000;
                                            if(dis > distancepoint)
                                            {routepoint = change_LngLong(missionpointlist.get(i+1));}

                                        }

                                        temporaryPoint.add(change_LongLng(routepoint));

                                    }
                                }
                            }


                            for(int i = 0; i < temporaryPoint.size()/2 ; i++)
                            {
                                int a= temporaryPoint.size()-1;

                                if (pointsetorder == true) {
                                    missionroutelist.add(temporaryPoint.get(i));
                                    missionroutelist.add(temporaryPoint.get(a - i));
                                    pointsetorder = false;
                                }
                                else if(pointsetorder == false)
                                {
                                    missionroutelist.add(temporaryPoint.get(a - i));
                                    missionroutelist.add(temporaryPoint.get(i));
                                    pointsetorder = true;


                                }
                            }
                            routeline.setCoords(missionroutelist);
                            routeline.setWidth(10);
                            routeline.setColor(Color.BLUE);
                            routeline.setMap(naverMap);



                        }
                    }
                });
        spacingsetting.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        spacingsetting.show();


    }//다각형을 만들고 그 안을 경로주행

    public Waypoint makewaypoint(LatLng latLng) {
        Waypoint waypoint=new Waypoint();
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        LatLongAlt a =new LatLongAlt(latLng.latitude,latLng.longitude,droneAltitude.getRelativeAltitude());
        waypoint.setDelay(1);
        waypoint.setCoordinate(a);
        return waypoint;
    }//미션수행시 드론의 웨이포인트를 생성

    public void setRTLmode(View view){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_RTL, new SimpleCommandListener() {
            @Override
            public void onSuccess() {alertUser("집으로");}
        });
    }//RTL버튼 클릭 이밴트

    public void mapSet(View view) {
        RelativeLayout settingmap = (RelativeLayout)findViewById(R.id.SettingMap);
        Button maptypetext = (Button)findViewById(R.id.btnMaptypeset);
        Button btncadastralmap = (Button) findViewById(R.id.btnCadastralMap);

        if(settingmap.getVisibility()==View.INVISIBLE) { settingmap.setVisibility(View.VISIBLE); }
        else if(settingmap.getVisibility()==View.VISIBLE) { settingmap.setVisibility(View.INVISIBLE); }

        maptypetext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(changmaptype == 0) {
                    maptypetext.setText("지형도");
                    naverMap.setMapType(NaverMap.MapType.Terrain);
                    changmaptype = 1;
                }
                else if(changmaptype == 1) {
                    maptypetext.setText("일반지도");
                    naverMap.setMapType(NaverMap.MapType.Basic);
                    changmaptype = 2;
                }
                else if(changmaptype == 2) {
                    maptypetext.setText("위성지도");
                    naverMap.setMapType(NaverMap.MapType.Satellite);
                    changmaptype = 0;
                }

            }
        });


        btncadastralmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cadastralmap == false) {
                    btncadastralmap.setText("지적도on");
                    alertUser("지적도on");
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    cadastralmap = true;
                } else if (cadastralmap == true) {
                    btncadastralmap.setText("지적도off");
                    alertUser("지적도off");
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    cadastralmap = false;
                }

            }
        });








    }//지적도, 맵타입 설정
    public void onMapMoveTap(View view) {
        Button Maplock= (Button) findViewById(R.id.btnMapLock);
        if(MapLock==true) {
            MapLock=false;
            Maplock.setSelected(true);
            alertUser("맵 잠금");
        }
        else if(MapLock==false) {
            MapLock=true;
            Maplock.setSelected(false);
            alertUser("맵 이동");
        }

    }//맵 고정 버튼 클릭 이밴트
    public void onClearButtenTap(View view) {

        ResetValue();

        movingpoint.clear();
        dronepathdisplay.setMap(null);

        alertUser("맵 클리어");

    }//맵정리 버튼 클릭 이밴트

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {

            this.drone.disconnect();
        }
        else {
            if (connecttype == 0)
            {
                ConnectionParameter connectionParams = ConnectionParameter.newUsbConnection(null);//USB텔레메트리로 연결

                this.drone.connect(connectionParams);
            }
            else if(connecttype == 1)
            {
                ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);// Wifi모듈 연결
                this.drone.connect(connectionParams);

            }
        }

    }//드론과 연결버튼 클릭 이밴트

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
/////테스트 필요


        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("비행모드변경 성공");

                if(vehicleMode.getLabel() == "Guided" || vehicleMode.getLabel() == "Loiter" || vehicleMode.getLabel() == "Alt Hold"  )
                {
                    rc_override.chan1_raw = 1500; //right; 2000 //left
                    rc_override.chan2_raw = 1500;
                    rc_override.chan3_raw = 1500; //back; 2000 //forward
                    rc_override.chan4_raw = 1500;
                    ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                    alertUser("기어중립");
                }
            }
            @Override
            public void onError(int executionError) { alertUser("비행모드변경 실패: " + executionError); }
            @Override
            public void onTimeout() {
            }
        });


    }//드론비행모드 변경확인

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        drone = this.drone;
        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) { alertUser("착륙불가"); }
                @Override
                public void onTimeout() {
                    alertUser("작업시간초과");
                }
            });
        } else if (vehicleState.isArmed()) {

            // Take off
            ControlApi.getApi(this.drone).takeoff(altitudevalue, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("발진!!!");
                }
                @Override
                public void onError(int i) {
                    alertUser("이륙불가");
                }
                @Override
                public void onTimeout() { alertUser("작업시간초과"); }
            });

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("드론에 먼저 연결");
        } else {
            // Connected but not Armed

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("발진!");
            alert.setMessage("시동?");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VehicleApi.getApi(drone).arm(true, false, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            alertUser("시동중...");
                        }
                        @Override
                        public void onError(int i) { alertUser("ARMING 실패"); }
                        @Override
                        public void onTimeout() { alertUser("작업시간초과"); }
                    });
                }
            });
            alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { }
            });
            alert.show();
        }
    }//시동 , 이륙 , 착륙

    //미션설정
    public void onMissionTypeTap(View view){

        Button normalmode = (Button)findViewById(R.id.btnSetNormalMode);
        Button Flight_mode = (Button)findViewById(R.id.btnSetFlightMode);
        Button Interval_monitoring = (Button)findViewById(R.id.btnSetIntervalMonitoring);
        Button Area_monitoring = (Button)findViewById(R.id.btnSetAreaMonitoring);
        RelativeLayout changemissiontype = (RelativeLayout)findViewById(R.id.ControlTypeSet);

        Button control_type = (Button)findViewById(R.id.btnMissionTypeOpen);

        Button btnroutemake = (Button)findViewById(R.id.btnRouteMake);
        Button btnmissionstart = (Button)findViewById(R.id.btnMission);

        if (changemissiontype.getVisibility()==View.INVISIBLE)
        {
            changemissiontype.setVisibility(View.VISIBLE);
        }
        else if (changemissiontype.getVisibility()==View.VISIBLE)
        {
            changemissiontype.setVisibility(View.INVISIBLE);
        }

        normalmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changemissionmode = 0;
                ResetValue();
                changemissiontype.setVisibility(View.INVISIBLE);
                btnmissionstart.setVisibility(View.INVISIBLE);
                btnroutemake.setVisibility(View.INVISIBLE);
                control_type.setText(normalmode.getText());
                alertUser("일반모드");


                //Log.i("mode","="+changemissionmode);
            }
        });
        Flight_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changemissionmode = 1;
                ResetValue();
                changemissiontype.setVisibility(View.INVISIBLE);
                btnmissionstart.setVisibility(View.VISIBLE);
                btnroutemake.setVisibility(View.INVISIBLE);
                control_type.setText(Flight_mode.getText());
                alertUser("경로비행");

                //Log.i("mode","="+changemissionmode);

            }
        });
        Interval_monitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changemissionmode = 2;
                ResetValue();
                changemissiontype.setVisibility(View.INVISIBLE);
                btnmissionstart.setVisibility(View.VISIBLE);
                btnroutemake.setVisibility(View.INVISIBLE);
                control_type.setText(Interval_monitoring.getText());
                alertUser("간격감시");

                //Log.i("mode","="+changemissionmode);

            }
        });
        Area_monitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changemissionmode = 3;
                ResetValue();
                changemissiontype.setVisibility(View.INVISIBLE);
                btnmissionstart.setVisibility(View.VISIBLE);
                btnroutemake.setVisibility(View.VISIBLE);
                control_type.setText(Area_monitoring.getText());
                alertUser("면적감시");

                //Log.i("mode","="+changemissionmode);

            }
        });


    }//사용미션 선택
    public void MissionCount() {
        if(waypointCount == missionroutelist.size())
        {
            //VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED);
            alertUser("미션종료");
            Button button = (Button)findViewById(R.id.btnMission);
            missionC=0;
            waypointCount=0;
            missionstartready = false;
            button.setText("미션등록");


        }

    }//미션종료 확인
    public void onMissionStartTap(View view){
        Button button = (Button)findViewById(R.id.btnMission);
        if(missionC==0) {
            Send_MiSSION();
            button.setText("미션시작");
            missionC=1;
        }
        else if(missionC==1&&missionstartready == true) {
            Start_MiSSION();
            missionC=2;
            button.setText("미션중지");
        }
        else if (missionC==2) {
            Stop_MiSSION();
            missionC=1;
            button.setText("미션시작");
        }
    }//미션 전송, 시작 버튼설정

    //미션 전송,시작,중단 함수
    protected void Send_MiSSION(){
        for(int i = 0; i<missionroutelist.size();i++) {
            dronemission.addMissionItem(makewaypoint(missionroutelist.get(i)));
        }
        MissionApi.getApi(this.drone).setMission(dronemission,true);
    }
    protected void Start_MiSSION(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {alertUser("미션시작"); }
        });
    }
    protected void Stop_MiSSION(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {alertUser("미션중지"); }
        });
        rc_override.chan1_raw = 1500; //right; 2000 //left
        rc_override.chan2_raw = 1500;
        rc_override.chan3_raw = 1500; //back; 2000 //forward
        rc_override.chan4_raw = 1500;
        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
    }
    //

    public void onRouteMakeTap(View view){
        if (changemissionmode == 3){
            if(missionpointlist.size()>2){
                PathSett();
            }
        }

    }//미션에 사용될 경로를 생성
    public void onConnecttypeChange(View view){
        Button button =(Button)findViewById(R.id.btnConnectType);

        if(connectconfirm == false) {
            if (connecttype == 0) {
                button.setText("WIFI");
                connecttype = 1;
            }
            else if (connecttype == 1) {
                button.setText("USB");
                connecttype = 0;
            }
        }

    }//드론과 GCS의 연결 타입 변경
    public void disconnectGCS(View view){
        Intent intent = new Intent(getApplicationContext(), StartActivity.class);
        startActivity(intent);
    }//GCS연결을 끊고 시작화면으로 돌아갑니다


    public void ChangeJoystickMode(View view){
        final RelativeLayout GCSmode = findViewById(R.id.GCSmodeView);
        final RelativeLayout joystickcontrolview= findViewById(R.id.JoystickControlView);
        Button btnchangemode = (Button)findViewById(R.id.btnControllerChange);

        if(dronecontroltype == 0){
            GCSmode.setVisibility(View.INVISIBLE);
            joystickcontrolview.setVisibility(View.VISIBLE);

            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
                @Override
                public void onSuccess() {alertUser("수동조종모드");}
                @Override
                public void onError(int executionError) { alertUser("LOITER전환실패"); }
            });

            rc_override.chan1_raw = 1500; //right; 2000 //left
            rc_override.chan2_raw = 1500;
            rc_override.chan3_raw = 1500; //back; 2000 //forward
            rc_override.chan4_raw = 1500;
            ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

            btnchangemode.setSelected(true);
            dronecontroltype = 1;
        }
        else if(dronecontroltype == 1)
        {
            GCSmode.setVisibility(View.VISIBLE);
            joystickcontrolview.setVisibility(View.INVISIBLE);

            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
                @Override
                public void onSuccess() {alertUser("GCS모드");}
                @Override
                public void onError(int executionError) { alertUser("GUIDED전환실패"); }
            });

            btnchangemode.setSelected(false);
            dronecontroltype = 0;
        }

    }//조이스틱 모드 , GCS모드를 변경합니다.
    public void joystick(){

        layout_Leftjoystick = (RelativeLayout)findViewById(R.id.layout_joystick);
        layout_Rightjoystick = (RelativeLayout)findViewById(R.id.layout_joystick2);

        jstickLeft = new JoyStickClass(getApplicationContext()
                , layout_Leftjoystick, R.drawable.image_button);
        jstickLeft.setStickSize(150, 150);
        jstickLeft.setLayoutSize(500, 500);
        jstickLeft.setLayoutAlpha(150);
        jstickLeft.setStickAlpha(100);
        jstickLeft.setOffset(90);
        jstickLeft.setMinimumDistance(20);

        jstickRight = new JoyStickClass(getApplicationContext()
                , layout_Rightjoystick, R.drawable.image_button);
        jstickRight.setStickSize(150, 150);
        jstickRight.setLayoutSize(500, 500);
        jstickRight.setLayoutAlpha(150);
        jstickRight.setStickAlpha(100);
        jstickRight.setOffset(90);
        jstickRight.setMinimumDistance(20);


        //조이스틱 왼쪽 컨트롤러
        layout_Leftjoystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                jstickLeft.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int Xpoint = jstickLeft.getX();
                    int Ypoint = jstickLeft.getY();

                    if(Xpoint<0){Xpoint= -Xpoint;}
                    else if(Xpoint>200){Xpoint=200;}
                    if(Ypoint<0){Ypoint= -Ypoint;}
                    else if(Ypoint>200){Ypoint=200;}

                    double XmotorValue = Xpoint*speedYaw;
                    double YmotorValue = Ypoint*speedUpDown;

                    if(jstickLeft.getDistance()>200) { float distance = 200; }

                    int direction = jstickLeft.get8Direction();
                    if(direction == JoyStickClass.STICK_UP) {
                        rc_override.chan3_raw = 1500 + (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("상승");
                    }

                    else if(direction == JoyStickClass.STICK_DOWN) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("하강");
                    }
                    else if(direction == JoyStickClass.STICK_RIGHT) {
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("시계회전");
                    }
                    else if(direction == JoyStickClass.STICK_LEFT) {
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("반시계회전");
                    }

                     else if(direction == JoyStickClass.STICK_UPRIGHT) {
                        rc_override.chan3_raw = 1500 + (int)YmotorValue;
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    }
                    else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    }

                    else if(direction == JoyStickClass.STICK_DOWNLEFT) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    }

                    else if(direction == JoyStickClass.STICK_UPLEFT) {
                        rc_override.chan3_raw = 1500 + (int)YmotorValue;
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    }
                    else if(direction == JoyStickClass.STICK_NONE) {
                        rc_override.chan1_raw = 1500;
                        rc_override.chan2_raw = 1500;
                        rc_override.chan3_raw = 1500;
                        rc_override.chan4_raw = 1500;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("정지");
                    }
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    rc_override.chan1_raw = 1500;
                    rc_override.chan2_raw = 1500;
                    rc_override.chan3_raw = 1500;
                    rc_override.chan4_raw = 1500;
                    ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                    alertUser("정지");
                }
                return true;
            }
        });

        //조이스틱 오른쪽 컨트롤러
        layout_Rightjoystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                jstickRight.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int Xpoint = jstickRight.getX();
                    int Ypoint = jstickRight.getY();
                    if(Xpoint < 0){Xpoint = - Xpoint;}
                    else if(Xpoint > 200){Xpoint = 200;}
                    if(Ypoint < 0){Ypoint = - Ypoint;}
                    else if(Ypoint > 200){Ypoint = 200;}

                    double XmotorValue = Xpoint * speedMove;
                    double YmotorValue = Ypoint * speedMove;

                    int direction = jstickRight.get8Direction();
                    if(direction == JoyStickClass.STICK_UP) {
                        rc_override.chan2_raw = 1500 - (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("전진");
                    } else if(direction == JoyStickClass.STICK_UPRIGHT) {
                        rc_override.chan2_raw = 1500 - (int)YmotorValue;
                        rc_override.chan1_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_RIGHT) {
                        rc_override.chan1_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("우회전");

                    } else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
                        rc_override.chan2_raw = 1500 + (int)YmotorValue;
                        rc_override.chan1_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_DOWN) {
                        rc_override.chan2_raw = 1500 + (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("후진");
                    } else if(direction == JoyStickClass.STICK_DOWNLEFT) {
                        rc_override.chan2_raw = 1500 + (int)YmotorValue;
                        rc_override.chan1_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_LEFT) {
                        rc_override.chan1_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("좌회전");

                    } else if(direction == JoyStickClass.STICK_UPLEFT) {
                        rc_override.chan1_raw = 1500 - (int)XmotorValue;
                        rc_override.chan2_raw = 1500 - (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));


                    } else if(direction == JoyStickClass.STICK_NONE) {
                        rc_override.chan1_raw = 1500;
                        rc_override.chan2_raw = 1500;
                        rc_override.chan3_raw = 1500;
                        rc_override.chan4_raw = 1500;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("정지");

                    }
                } else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    rc_override.chan1_raw = 1500;
                    rc_override.chan2_raw = 1500;
                    rc_override.chan3_raw = 1500;
                    rc_override.chan4_raw = 1500;
                    ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                    alertUser("정지");

                }
                return true;
            }
        });

    }//조이스틱 조작시 드론이동설정

    //좌표변수 변수값 변경함수
    public LatLong change_LngLong(LatLng latLng){
        LatLong latLong = new LatLong(latLng.latitude,latLng.longitude);
        return latLong;
    }
    public LatLng change_LongLng(LatLong latLong){
        LatLng latLng = new LatLng(latLong.getLatitude(),latLong.getLongitude());
        return latLng;
    }
    //
    protected double distanceBetweenPoints(LatLong pointA, LatLong pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        return Math.sqrt(dx * dx + dy * dy)*100;

    }//두 좌표사이의 거리
    /*protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }*///두 좌표사이의 거리 고도차이 추가

    public void ResetValue() {
        temporaryPoint.clear();
        missionroutelist.clear();


        missionpolygon.setMap(null);
        goalMarker.setMap(null);
        missionpoint_A.setMap(null);
        missionpoint_B.setMap(null);
        Square_line.setMap(null);
        routeline.setMap(null);
        addmissionpoint_A.setMap(null);
        addmissionpoint_B.setMap(null);

        Button button = (Button)findViewById(R.id.btnMission);
        missionC=0;
        waypointCount=0;
        button.setText("미션등록");

        for(int i = 0; i < waypointmarkerlist.size(); i++)
        {
            waypointmarkerlist.get(i).setMap(null);
        }
        waypointmarkerlist.clear();
        missionstartwhether = true;

    }//여러값들 초기화



}
















