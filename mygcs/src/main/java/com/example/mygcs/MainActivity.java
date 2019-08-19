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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
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

    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    boolean MP=true;
    boolean Map_L=true;
    boolean Map_C=false;
    boolean getPoint_AB = true;
    Marker drone_M = new Marker();
    Marker My_M = new Marker();
    Marker GO_M = new Marker();

    int control_mode = 0;

    Marker Point_A = new Marker();
    Marker Point_B = new Marker();
    Marker aaa = new Marker();
    Marker bbb = new Marker();
    LatLng drone_A;
    LatLng Home_A;
    private RecyclerViewAdapter adapter;
    ArrayList<String> listTitle = new ArrayList<>();

    double al=3;

    PolylineOverlay line_AB= new PolylineOverlay();
    PolylineOverlay Square_line= new PolylineOverlay();
    PolylineOverlay Mission_line = new PolylineOverlay();
    PolylineOverlay line= new PolylineOverlay();

    ArrayList<LatLng> A_line = new ArrayList();
    ArrayList<LatLng> Square_Point = new ArrayList();
    ArrayList<LatLng> Drone_line = new ArrayList();
    ArrayList<LatLng> Mission_Point = new ArrayList();
    ArrayList<Waypoint> way = new ArrayList();

    private double yaw_value;
    private Spinner modeSelector;

    boolean a = true;
    NaverMap naverMap;
    MapFragment mNaverMapFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
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
    public void onMapReady(@NonNull final NaverMap naverMap) {
        this.naverMap=naverMap;
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
                getp_AB();
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
                updateSpeed();
                break;

            case AttributeEvent.GUIDED_POINT_UPDATED:
                if(control_mode == 0) {getClick_LatLng();}
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;
            case AttributeEvent.GPS_COUNT:
                updateSatellitesnum();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;
            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;
            case AttributeEvent.GPS_POSITION:
                updateDroneLatLng();
                updateHomeLatLng();
                updateDroneroute();
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
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }
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
        double vehicleAltitude = droneAltitude.getAltitude();

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
    protected void updateHomeLatLng(){

        Home My_H =this.drone.getAttribute(AttributeType.HOME);
        LatLong HomePosition = My_H.getCoordinate();
        Home_A = new LatLng(HomePosition.getLatitude(),HomePosition.getLongitude());

        My_M.setIcon(OverlayImage.fromResource(R.drawable.ethereum_48px));
        My_M.setPosition(Home_A);
        My_M.setMap(naverMap);



    }
    protected void updateDroneLatLng() {
        float drac=0;
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();
        drone_A=new LatLng(vehiclePosition.getLatitude(),vehiclePosition.getLongitude());

        drone_M.setIcon(OverlayImage.fromResource(R.drawable.illuminati_48px));
        drone_M.setFlat(true);
        if(yaw_value>=0){ drac =(float) yaw_value;}
        else if(yaw_value<0) {drac = (float)(180+(180+yaw_value)); }

        drone_M.setAngle(drac);
        drone_M.setPosition(drone_A);
        drone_M.setHeight(230);
        drone_M.setWidth(120);
        drone_M.setAnchor(new PointF((float)0.5,(float)0.85));

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
    protected void updateDroneroute(){

        Drone_line.add(drone_A);
        line.setCoords(Drone_line);
        line.setWidth(10);
        line.setColor(Color.YELLOW);
        line.setJoinType(PolylineOverlay.LineJoin.Round);
        line.setMap(naverMap);
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
    protected void getClick_LatLng(){
            naverMap.setOnMapLongClickListener((pointF, latLng) -> {
                GO_M.setPosition(latLng);
                GO_M.setIcon(OverlayImage.fromResource(R.drawable.empty_flag_64px));
                GO_M.setHeight(50);
                GO_M.setWidth(50);
                GO_M.setMap(naverMap);
            /*VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
                @Override
                public void onSuccess() {alertUser("성공"); }
            });*/
                LatLong goA = new LatLong(latLng.latitude, latLng.longitude);

                alertUser("출발");
                ControlApi.getApi(this.drone).goTo(goA, true, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() { alertUser("도착"); }
                    @Override
                    public void onError(int executionError) { alertUser("실패"); }

                });
            });
    }
    protected void getp_AB() {
        naverMap.setOnMapLongClickListener((pointF, latLng) -> {
            if (getPoint_AB == true) {
                A_line.clear();
                Square_Point.clear();
                Square_line.setMap(null);
                Mission_Point.clear();
                Mission_line.setMap(null);
                line_AB.setMap(null);
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




                for(int i=0 ; i<=50 ; i+=5)
                {
                    LatLong n = MathUtils.newCoordFromBearingAndDistance(change_LngLong(A_line.get(0)),90+(int)MathUtils.getHeadingFromCoordinates(change_LngLong(A_line.get(0)),change_LngLong(A_line.get(1))),i);
                    LatLong m = MathUtils.newCoordFromBearingAndDistance(change_LngLong(A_line.get(1)),90+(int)MathUtils.getHeadingFromCoordinates(change_LngLong(A_line.get(0)),change_LngLong(A_line.get(1))),i);
                    if(MP==true){
                        Mission_Point.add(change_LongLng(n));
                        Mission_Point.add(change_LongLng(m));
                        MP=false;
                    }
                    else if(MP==false){
                        Mission_Point.add(change_LongLng(m));
                        Mission_Point.add(change_LongLng(n));
                        MP=true;
                    }

                    if(i==50)
                    {
                        Square_Point.add(A_line.get(0));
                        Square_Point.add(change_LongLng(n));
                        Square_Point.add(change_LongLng(m));
                        Square_Point.add(A_line.get(1));

                        aaa.setPosition(change_LongLng(n));
                        aaa.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px));
                        bbb.setPosition(change_LongLng(m));
                        bbb.setIcon(OverlayImage.fromResource(R.drawable.icons8_map_pin_24px_3));

                        bbb.setMap(naverMap);
                        aaa.setMap(naverMap);

                    }
                }

                alertUser("A to B = "+(int)MathUtils.getDistance2D(change_LngLong(A_line.get(0)),change_LngLong(A_line.get(1)))+"m");

                Mission_line.setCoords(Mission_Point);
                Mission_line.setWidth(10);
                Mission_line.setColor(Color.GREEN);
                Mission_line.setMap(naverMap);

                Square_line.setCoords(Square_Point);
                Square_line.setWidth(10);
                Square_line.setMap(naverMap);

                line_AB.setCoords(A_line);
                line_AB.setWidth(10);
                line_AB.setMap(naverMap);

            }
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

    // UI Events
    // ==========================================================

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
        Drone_line.clear();
        GO_M.setMap(null);
        Point_A.setMap(null);
        Point_B.setMap(null);
        line_AB.setMap(null);
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
        } else {
           // Spinner connectionSelector = (Spinner) findViewById(R.id.selectConnectionType);
           // int selectedConnectionType = connectionSelector.getSelectedItemPosition();

            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }

    }
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

          /*  AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("발진!");
            alert.setMessage("설정고도:"+al+"\n비행을 시작할까요?");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ControlApi.getApi(drone).takeoff(al, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            alertUser("발진!!!");
                        }
                        @Override
                        public void onError(int i) { alertUser("이륙불가"); }
                        @Override
                        public void onTimeout() { alertUser("이륙시간초과"); }
                    });
                }
            });
            alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { }
            });
            alert.show();
*/
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

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

}
















