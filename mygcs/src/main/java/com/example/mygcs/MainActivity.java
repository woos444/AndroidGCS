package com.example.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.util.MathUtils;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback , DroneListener, TowerListener, LinkListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    WebView RaspberryStream;
    TableRow InfoWindow;
    RecyclerView NotificationWindow;

    boolean MP=true;
    boolean Map_L=true;
    boolean Map_C=false;
    boolean getPoint_AB = true;
    Marker drone_M = new Marker();
    Marker Home_M = new Marker();
    Marker GO_M = new Marker();

    int cameraType = 0;
    int control_mode = 0;
    int missionC=0;

    int M_W=0;
    int M_L=0;

    //조이스틱 조작시 드론의 속도
    double speedYaw =1.25;
    double speedUpDown = 1.5;
    double speedMove = 1.25;

    Marker Point_A = new Marker();
    Marker Point_B = new Marker();
    Marker aaa = new Marker();
    Marker bbb = new Marker();
    LatLng drone_A;
    LatLng Home_A;
    LatLongAlt My_A;

    private RecyclerViewAdapter adapter;
    ArrayList<String> listTitle = new ArrayList<>();

    //초기고도설정값
    double al=3;

    PolylineOverlay Square_line= new PolylineOverlay();
    PolylineOverlay Mission_line = new PolylineOverlay();
    PolylineOverlay line= new PolylineOverlay();

    Mission mission1 = new Mission();

    ArrayList<LatLng> A_line = new ArrayList();
    ArrayList<LatLng> Square_Point = new ArrayList();
    ArrayList<LatLng> Drone_line = new ArrayList();
    ArrayList<LatLng> Mission_Point = new ArrayList();

    private double yaw_value;
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
        Maptype();

        InfoWindow=(TableRow)findViewById(R.id.jeongbochang);
        InfoWindow.bringToFront();
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

        mjpgstream();
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
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);

    }
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        setTakeoff_al();
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
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("드론분리");
               updateConnectedButton(this.drone.isConnected());
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
                updateSpeed();
                break;

            case AttributeEvent.GUIDED_POINT_UPDATED:
                getP();
                break;

            case AttributeEvent.MISSION_SENT:
                alertUser("미션전송완료");
                Button button = (Button)findViewById(R.id.Mission_start);
                button.setText("미션시작");
                missionC=1;

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

                updateDroneLatLng();
                updateHomeLatLng();
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
    }
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
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getRelativeAltitude()) + "m");
    }

    //제거
    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }
    protected void updateSatellitesnum() {
        TextView SatelliteTextView = (TextView) findViewById(R.id.SatellitesnumTextView);
        Gps num = this.drone.getAttribute(AttributeType.GPS);
        SatelliteTextView.setText(String.format("%d", num.getSatellitesCount()) );
    }
    protected void updateVoltage() {
        TextView VoltageTextView = (TextView) findViewById(R.id.VoltageValueTextView);
        Battery Volt = this.drone.getAttribute(AttributeType.BATTERY);
        VoltageTextView.setText(String.format("%.1f", Volt.getBatteryVoltage())+"V" );
    }
    protected void updateYAW() {
        TextView YAWTextView = (TextView) findViewById(R.id.YAWValueTextView);
        Attitude YAW = this.drone.getAttribute(AttributeType.ATTITUDE);
        yaw_value = YAW.getYaw();
        YAWTextView.setText(String.format("%.0f", yaw_value)+"deg");
    }
    protected void updateDistanceFromHome() {
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

    protected void updateDroneLatLng() {
        float drac=0;
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();
        drone_A =change_LongLng(vehiclePosition);
        Drone_line.add(drone_A);

        drone_M.setIcon(OverlayImage.fromResource(R.drawable.fighter_jet_24px));
        drone_M.setFlat(true);
        if(yaw_value>=0){ drac =(float) yaw_value;}
        else if(yaw_value<0) {drac = (float)(180+(180+yaw_value)); }

        drone_M.setAngle(drac);
        drone_M.setPosition(drone_A);
        drone_M.setHeight(300);
        drone_M.setWidth(100);
        drone_M.setAnchor(new PointF((float)0.5,(float)0.85));

        line.setCoords(Drone_line);
        line.setWidth(20);
        line.setColor(Color.YELLOW);
        line.setJoinType(PolylineOverlay.LineJoin.Round);
        line.setMap(naverMap);

        if (Map_L==true)
        {
            naverMap.moveCamera(null);
        }
        else if(Map_L==false)
        {
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(drone_A);
            naverMap.moveCamera(cameraUpdate);
        }
        drone_M.setMap(naverMap);

    }
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
    }
    protected void setTakeoff_al(){
        Button takeoff_al = (Button)findViewById(R.id.tackoff_al);
        String al_text = (int)al+"m\n이륙고도";
        final SpannableStringBuilder sps = new SpannableStringBuilder(al_text);
        sps.setSpan(new AbsoluteSizeSpan(30),1,3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        takeoff_al.setText(sps);
        alertUser(al+"m 이륙고도");

    }
    protected void getP() {
        naverMap.setOnMapLongClickListener((pointF, latLng) -> {
            if(control_mode == 0) {
                Point_A.setMap(null);
                Point_B.setMap(null);
                Square_Point.clear();
                Square_line.setMap(null);
                Mission_Point.clear();
                Mission_line.setMap(null);
                aaa.setMap(null);
                bbb.setMap(null);

                GO_M.setPosition(latLng);
                GO_M.setIcon(OverlayImage.fromResource(R.drawable.empty_flag_64px));
                GO_M.setHeight(70);
                GO_M.setWidth(70);
                GO_M.setMap(naverMap);
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
                @Override
                public void onSuccess() {alertUser("가이드모드");}
            });
                LatLong goA = new LatLong(latLng.latitude, latLng.longitude);

                ControlApi.getApi(this.drone).goTo(goA, true, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() { alertUser("출발"); }
                    @Override
                    public void onError(int executionError) { alertUser("실패"); }
                });
            }
            else if (control_mode == 1){

                VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() {alertUser("가이드모드");}
                });

                ControlApi.getApi(this.drone).turnTo(180,1.0f, true, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() { alertUser("회전"); }
                    @Override
                    public void onError(int executionError) { alertUser("실패"); }
                });

            }
            else if (control_mode == 2){
                if (getPoint_AB == true){
                    GO_M.setMap(null);
                    MP=true;
                    A_line.clear();
                    mission1.clear();
                    Square_Point.clear();
                    Square_line.setMap(null);
                    Mission_Point.clear();
                    Mission_line.setMap(null);
                    aaa.setMap(null);
                    bbb.setMap(null);
                    Point_B.setMap(null);

                    A_line.add(latLng);
                    Point_A.setPosition(A_line.get(0));
                    Point_A.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                    Point_A.setMap(naverMap);
                    Point_B.setMap(null);
                    getPoint_AB = false;
                }
                else if (getPoint_AB == false) {
                    A_line.add(latLng);
                    Point_B.setPosition(A_line.get(1));
                    Point_B.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));
                    Point_B.setMap(naverMap);
                    getPoint_AB = true;

                    show();
                }
            }
            else if (control_mode == 3){

                ControlApi.getApi(this.drone).manualControl(1.0f,1.0f, 1.0f, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() { alertUser("메뉴얼컨트롤"); }
                    @Override
                    public void onError(int executionError) { alertUser("실패"); }

                });
            }
        });
    }

    protected void Send_MiSSION(){
        for(int i = 0; i<Mission_Point.size();i++) {
            mission1.addMissionItem(make_waypoint(Mission_Point.get(i)));
        }
        MissionApi.getApi(this.drone).setMission(mission1,true);
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

    }

    public LatLong change_LngLong(LatLng latLng){
        LatLong latLong = new LatLong(latLng.latitude,latLng.longitude);
        return latLong;
    }
    public LatLng change_LongLng(LatLong latLong){
        LatLng latLng = new LatLng(latLong.getLatitude(),latLong.getLongitude());
        return latLng;
    }
    public Waypoint make_waypoint(LatLng latLng) {
        Waypoint waypoint=new Waypoint();
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        LatLongAlt a =new LatLongAlt(latLng.latitude,latLng.longitude,droneAltitude.getRelativeAltitude());
        waypoint.setDelay(1);
        waypoint.setCoordinate(a);
        return waypoint;
    }

    public void show() {

        final List<String> interval_L = new ArrayList<>();
        interval_L.add("3");
        interval_L.add("5");
        interval_L.add("10");
        interval_L.add("20");
        final CharSequence[] items2 =  interval_L.toArray(new String[ interval_L.size()]);

        final List SelectedItems2  = new ArrayList();
        int defaultItem2 = 0;
        SelectedItems2.add(defaultItem2);
        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);

        /////////////////
        final List<String> interval_W = new ArrayList<>();
        interval_W.add("10");
        interval_W.add("20");
        interval_W.add("50");
        interval_W.add("100");
        final CharSequence[] items1 =  interval_W.toArray(new String[ interval_W.size()]);

        final List SelectedItems1  = new ArrayList();
        int defaultItem1 = 0;
        SelectedItems1.add(defaultItem1);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("거리설정");
        builder1.setSingleChoiceItems(items1, defaultItem1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems1.clear();
                        SelectedItems1.add(which);
                }
                });
        builder1.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg1="";

                        if (!SelectedItems1.isEmpty()) {
                            int index = (int) SelectedItems1.get(0);
                            msg1 = interval_W.get(index);
                            M_W = Integer.parseInt(msg1);
                            builder2.show();
                        }
                    }
                });
        builder1.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder2.setTitle("간격 설정");
        builder2.setSingleChoiceItems(items2, defaultItem2,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems2.clear();
                        SelectedItems2.add(which);
                    }
                });
        builder2.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg2="";

                        if (!SelectedItems2.isEmpty()) {
                            int index = (int) SelectedItems2.get(0);
                            msg2 = interval_L.get(index);
                            M_L = Integer.parseInt(msg2);
                            Log.i("test1","거리"+M_W);
                            Log.i("test2","간격"+M_L);

                            for(int i=0 ; i<=M_W ; i++)
                            {
                                if(i%M_L==0) {
                                    LatLong n = MathUtils.newCoordFromBearingAndDistance(change_LngLong(A_line.get(0)), 90 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(A_line.get(0)), change_LngLong(A_line.get(1))), i);
                                    LatLong m = MathUtils.newCoordFromBearingAndDistance(change_LngLong(A_line.get(1)), 90 + (int) MathUtils.getHeadingFromCoordinates(change_LngLong(A_line.get(0)), change_LngLong(A_line.get(1))), i);
                                    if (MP == true) {
                                        Mission_Point.add(change_LongLng(n));
                                        Mission_Point.add(change_LongLng(m));
                                        MP = false;
                                    } else if (MP == false) {
                                        Mission_Point.add(change_LongLng(m));
                                        Mission_Point.add(change_LongLng(n));
                                        MP = true;
                                    }
                                }

                            }

                        /*Square_Point.add(A_line.get(0));
                                    Square_Point.add(change_LongLng(n));
                                    Square_Point.add(change_LongLng(m));
                                    Square_Point.add(A_line.get(1));*/

                            aaa.setPosition(Mission_Point.get(Mission_Point.size()-1));
                            aaa.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                            bbb.setPosition(Mission_Point.get(Mission_Point.size()-2));
                            bbb.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));

                            bbb.setMap(naverMap);
                            aaa.setMap(naverMap);

                            alertUser("A to B = "+(int)MathUtils.getDistance2D(change_LngLong(A_line.get(0)),change_LngLong(A_line.get(1)))+"m");

                            Mission_line.setCoords(Mission_Point);
                            Mission_line.setWidth(10);
                            Mission_line.setColor(Color.GREEN);
                            Mission_line.setMap(naverMap);


                        }
                    }
                });
        builder2.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder1.show();

    }



    public void Maptype() {
        final Button Maptype= (Button)findViewById(R.id.Maptype_button);
        final Button T_bt = (Button)findViewById(R.id.TerrainMap_bt);
        final Button B_bt = (Button)findViewById(R.id.BasicMap_bt);
        final Button S_bt = (Button)findViewById(R.id.SatlliteMap_bt);
        Maptype.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (T_bt.getVisibility()==View.INVISIBLE) {
                    T_bt.setVisibility(View.VISIBLE);
                    B_bt.setVisibility(View.VISIBLE);
                    S_bt.setVisibility(View.VISIBLE);
                }
                else if(T_bt.getVisibility()==View.VISIBLE)
                {
                    T_bt.setVisibility(View.INVISIBLE);
                    B_bt.setVisibility(View.INVISIBLE);
                    S_bt.setVisibility(View.INVISIBLE);

                }
            }
        });

        T_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Maptype.setText(T_bt.getText());
                naverMap.setMapType(NaverMap.MapType.Terrain);
                alertUser("지형도");
            }
        });
        S_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Maptype.setText(S_bt.getText());
                naverMap.setMapType(NaverMap.MapType.Satellite);
                alertUser("위성지도");
            }
        });
        B_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Maptype.setText(B_bt.getText());
                naverMap.setMapType(NaverMap.MapType.Basic);
                alertUser("일반지도");
            }
        });



    }
    public void onCADAtap(View view) {
        Button CADA = (Button)findViewById(R.id.CadaStral_button);
        if(Map_C==false) {
            CADA.setText("지적도on");
            alertUser("지적도on");
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL,true);
            Map_C=true;
        }
        else if(Map_C==true){
            CADA.setText("지적도off");
            alertUser("지적도off");
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL,false);
            Map_C=false;
        }


    }
    public void onMapMoveTap(View view) {
        Button Maplock= (Button)findViewById(R.id.Maplock_button);
        if(Map_L==true) {
            Map_L=false;
            Maplock.setText("맵잠금");
            alertUser("맵 잠금");
        }
        else if(Map_L==false) {
            Map_L=true;
            Maplock.setText("맵이동");
            alertUser("맵 이동");
        }

    }
    public void onClearButtenTap(View view) {

        line.setMap(null);
        //Drone_line.clear();
        GO_M.setMap(null);
        Point_A.setMap(null);
        Point_B.setMap(null);
        Square_Point.clear();
        Square_line.setMap(null);
        Mission_Point.clear();
        Mission_line.setMap(null);
        aaa.setMap(null);
        bbb.setMap(null);

        alertUser("맵 클리어");

    }

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {

            this.drone.disconnect();

        }
        else {
           // Spinner connectionSelector = (Spinner) findViewById(R.id.selectConnectionType);
           // int selectedConnectionType = connectionSelector.getSelectedItemPosition();

            ConnectionParameter connectionParams = ConnectionParameter.newUsbConnection(null);
            this.drone.connect(connectionParams);
        }

    }

    /*
    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        else {
            Spinner connectionSelector = (Spinner) findViewById(R.id.selectConnectionType);
            int selectedConnectionType = connectionSelector.getSelectedItemPosition();

            ConnectionParameter connectionParams = selectedConnectionType == ConnectionType.TYPE_USB
                ? ConnectionParameter.newUsbConnection(null)
                : ConnectionParameter.newUdpConnection(null);

            this.drone.connect(connectionParams);
        }

    }
     */
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() { alertUser("비행모드변경 성공"); }
            @Override
            public void onError(int executionError) { alertUser("비행모드변경실패: " + executionError); }
            @Override
            public void onTimeout() {
            }
        });
    }
    public void onTakeoffALTap(View view){
        Button al_up = (Button)findViewById(R.id.al_UP);
        Button al_down = (Button)findViewById(R.id.al_DOWN);
                if (al_up.getVisibility()==View.INVISIBLE)
                {
                    al_up.setVisibility(View.VISIBLE);
                    al_down.setVisibility(View.VISIBLE);

                }
                else if (al_up.getVisibility()==View.VISIBLE)
                {
                    al_up.setVisibility(View.INVISIBLE);
                    al_down.setVisibility(View.INVISIBLE);
                }

        al_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                al += 1;
                setTakeoff_al();
            }
        });
        al_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(al>0){al -= 1;}
                setTakeoff_al();
            }
        });

    }
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
                    alertUser("착륙불가");
                }
            });
        } else if (vehicleState.isArmed()) {

            // Take off
            ControlApi.getApi(this.drone).takeoff(al, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("발진!!!");
                }
                @Override
                public void onError(int i) {
                    alertUser("이륙불가");
                }
                @Override
                public void onTimeout() { alertUser("이륙시간초과"); }
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
    }
    public void onControlTypeTap(View view){

        Button normalmode = (Button)findViewById(R.id.normalmode);
        Button Flight_mode = (Button)findViewById(R.id.Flight_mode);
        Button Interval_monitoring = (Button)findViewById(R.id.Interval_monitoring);
        Button Area_monitoring = (Button)findViewById(R.id.Area_monitoring);
        Button control_type = (Button)findViewById(R.id.control_type);

        Button mission_s = (Button)findViewById(R.id.Mission_start);

        if (normalmode.getVisibility()==View.INVISIBLE)
        {
            normalmode.setVisibility(View.VISIBLE);
            Flight_mode.setVisibility(View.VISIBLE);
            Interval_monitoring.setVisibility(View.VISIBLE);
            Area_monitoring.setVisibility(View.VISIBLE);

        }
        else if (normalmode.getVisibility()==View.VISIBLE)
        {
            normalmode.setVisibility(View.INVISIBLE);
            Flight_mode.setVisibility(View.INVISIBLE);
            Interval_monitoring.setVisibility(View.INVISIBLE);
            Area_monitoring.setVisibility(View.INVISIBLE);
        }

        normalmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_mode = 0;
                normalmode.setVisibility(View.INVISIBLE);
                Flight_mode.setVisibility(View.INVISIBLE);
                Interval_monitoring.setVisibility(View.INVISIBLE);
                Area_monitoring.setVisibility(View.INVISIBLE);
                control_type.setText(normalmode.getText());
                mission_s.setVisibility(View.INVISIBLE);
                alertUser("일반모드");


                Log.i("mode","="+control_mode);
            }
        });
        Flight_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_mode = 1;
                normalmode.setVisibility(View.INVISIBLE);
                Flight_mode.setVisibility(View.INVISIBLE);
                Interval_monitoring.setVisibility(View.INVISIBLE);
                Area_monitoring.setVisibility(View.INVISIBLE);
                control_type.setText(Flight_mode.getText());
                alertUser("경로비행");

                Log.i("mode","="+control_mode);

            }
        });
        Interval_monitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                control_mode = 2;
                normalmode.setVisibility(View.INVISIBLE);
                Flight_mode.setVisibility(View.INVISIBLE);
                Interval_monitoring.setVisibility(View.INVISIBLE);
                Area_monitoring.setVisibility(View.INVISIBLE);
                control_type.setText(Interval_monitoring.getText());
                mission_s.setVisibility(View.VISIBLE);
                alertUser("간격감시");

                Log.i("mode","="+control_mode);

            }
        });
        Area_monitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_mode = 3;
                normalmode.setVisibility(View.INVISIBLE);
                Flight_mode.setVisibility(View.INVISIBLE);
                Interval_monitoring.setVisibility(View.INVISIBLE);
                Area_monitoring.setVisibility(View.INVISIBLE);
                control_type.setText(Area_monitoring.getText());
                alertUser("면적감시");

                Log.i("mode","="+control_mode);

            }
        });


    }
    public void onMissionStartTap(View view){
        Button button = (Button)findViewById(R.id.Mission_start);
        if(missionC==0) {
            Send_MiSSION();
        }
        else if(missionC==1) {
            Start_MiSSION();
            missionC=2;
            button.setText("미션중지");
        }
        else if (missionC==2) {
            Stop_MiSSION();
            missionC=0;
            button.setText("미션등록");
        }
    }
    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }



    //수정
    public void onCameraModeChange(View view){
        final Button btnconnect= (Button)findViewById(R.id.btnConnect);
        final Button btntackoffaltitude= (Button)findViewById(R.id.tackoff_al);
        final Button btncontroltype= (Button)findViewById(R.id.control_type);
        final Button btnclear= (Button)findViewById(R.id.Clear_Butten);
        final Button btncadastral= (Button)findViewById(R.id.CadaStral_button);
        final Button btnmaptypechange= (Button)findViewById(R.id.Maptype_button);
        final Button btnmaplock= (Button)findViewById(R.id.Maplock_button);
        final Button btnarm= (Button)findViewById(R.id.btnArmTakeOff);

        final RelativeLayout videocontrolview= findViewById(R.id.VideoControlView);
        if(cameraType==0){
            btncadastral.setVisibility(View.INVISIBLE);
            btnconnect.setVisibility(View.INVISIBLE);
            btntackoffaltitude.setVisibility(View.INVISIBLE);
            btncontroltype.setVisibility(View.INVISIBLE);
            btnclear.setVisibility(View.INVISIBLE);
            btnmaptypechange.setVisibility(View.INVISIBLE);
            btnmaplock.setVisibility(View.INVISIBLE);
            btnarm.setVisibility(View.INVISIBLE);

            videocontrolview.setVisibility(View.VISIBLE);
            cameraType=1;
        }
        else if(cameraType==1)
        {
            btncadastral.setVisibility(View.VISIBLE);
            btnconnect.setVisibility(View.VISIBLE);
            btntackoffaltitude.setVisibility(View.VISIBLE);
            btncontroltype.setVisibility(View.VISIBLE);
            btnclear.setVisibility(View.VISIBLE);
            btnmaptypechange.setVisibility(View.VISIBLE);
            btnmaplock.setVisibility(View.VISIBLE);
            btnarm.setVisibility(View.VISIBLE);

            videocontrolview.setVisibility(View.INVISIBLE);
            cameraType=0;

        }

    }

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
                    } else if(direction == JoyStickClass.STICK_UPRIGHT) {
                        rc_override.chan3_raw = 1500 + (int)YmotorValue;
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_RIGHT) {
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("시계회전");
                    } else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        rc_override.chan4_raw = 1500 + (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_DOWN) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("하강");
                    } else if(direction == JoyStickClass.STICK_DOWNLEFT) {
                        rc_override.chan3_raw = 1500 - (int)YmotorValue;
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));

                    } else if(direction == JoyStickClass.STICK_LEFT) {
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
                        ExperimentalApi.getApi(drone).sendMavlinkMessage(new MavlinkMessageWrapper(rc_override));
                        alertUser("반시계회전");
                    } else if(direction == JoyStickClass.STICK_UPLEFT) {
                        rc_override.chan3_raw = 1500 + (int)YmotorValue;
                        rc_override.chan4_raw = 1500 - (int)XmotorValue;
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

    }


    //
    public void mjpgstream(){
        RaspberryStream = (WebView) findViewById(R.id.webView);

        WebSettings streamingSet = RaspberryStream.getSettings();//Mobile Web Setting
        streamingSet.setJavaScriptEnabled(true);//자바스크립트 허용
        streamingSet.setLoadWithOverviewMode(true);//컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정

        streamingSet.setBuiltInZoomControls(false);
        streamingSet.setUseWideViewPort(true);

        RaspberryStream.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        //pi3 스트리밍 ip
        RaspberryStream.loadUrl("http://192.168.43.84:8090/?action=stream");
    }


}
















