package com.example.my;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.sothree.slidinguppanel.PanelState;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    //https://popcorn16.tistory.com/76 - db참고

    SlidingUpPanelLayout slide ;
    MaterialCalendarView cal;
    TextView month, total;
    Button open;
    Button [] btns;

    DBHelper helper;
    SQLiteDatabase db;

    int x;

    String[] versionArray;
    int[] colors;

    int[] sel;

    int[][] MON; //월별 결석일수와 지각조퇴일수

    int yy;
    int mm;
    int dd;

    //실제 강의는 6/28까지
    boolean MONTH2 ; //2월19~3월18
    boolean MONTH3 ; //3월19~4월18
    boolean MONTH4 ; //4월19~5월18
    boolean MONTH5 ; //5월19~6월18
    boolean MONTH6 ; //6월19~7월18

    int col;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        month = (TextView)findViewById(R.id.month);
        total = (TextView)findViewById(R.id.total);

        btns = new Button[6];

        Typeface tf = ResourcesCompat.getFont(this, R.font.gmarket_sans_light);

        int[] ids={R.id._2,R.id._3,R.id._4,R.id._5,R.id._6};
        for(int i=1;i<btns.length;i++){
            btns[i]=(Button) findViewById(ids[i-1]);
            btns[i].setText(Integer.toString(i+1)+"회차");
            btns[i].setTypeface(tf);
            btns[i].setTextSize(20);
            btns[i].setGravity(Gravity.CENTER);
            btns[i].setBackgroundColor(Color.parseColor("#55967E"));
            setClick(btns[i] ,i);
        }

        slide = (SlidingUpPanelLayout)findViewById(R.id.main_frame);
        open=(Button)findViewById(R.id.btn_open);

        open.setBackgroundColor(Color.parseColor("#55967E"));



        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PanelState state =  slide.getPanelState();
                if(state == PanelState.COLLAPSED){                 //닫혀있을경우
                    slide.setPanelState(PanelState.EXPANDED);
                }else{                                          //열려있을경우
                    slide.setPanelState(PanelState.COLLAPSED);
                }
            }
        });
        versionArray = new String[] {"결석","조퇴","지각"};
        colors = new int[] {Color.RED,Color.GREEN,Color.BLUE};
        sel = new int[1];
        sel[0]=0;
        col=0;

        MON = new int[6][3]; //[월][결석 , 조퇴지각, 총결석인정일수]
        for(int i =0;i<MON.length;i++){
            for(int j=0;j<MON[i].length;j++){
                MON[i][j]=0;
            }
        }

        helper = new DBHelper(MainActivity.this, "newdb.db", null, 1);
        db = helper.getWritableDatabase();
        helper.onCreate(db);

        cal = findViewById(R.id.calendarView);
        cal.setDateTextAppearance(R.style.customTextViewFontStyle);
        cal.setWeekDayTextAppearance(R.style.customTextViewFontStyle);
        cal.setHeaderTextAppearance(R.style.customTextViewFontStyle);

        cal.setSelectionColor(Color.parseColor("#55967E"));

        cal.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                showDialog(date);
            }
        });

        //db에 맞게 점 표시
        showSel();

    }

    public void showSel(){
        String sql = "select * from mytable;";
        Cursor c = db.rawQuery(sql, null);
        for(int i =0;i<MON.length;i++){
            for(int j=0;j<MON[i].length;j++){
                MON[i][j]=0;
            }
        }
        while(c.moveToNext()){
            String txt=c.getString(c.getColumnIndex("date_sel"));

            txt=txt.replaceAll("[a-z]","");
            txt=txt.replaceAll("[A-Z]","");
            txt=txt.replace("{","");
            txt=txt.replace("}","");
            String[] selDay=txt.split("-");
            yy = Integer.parseInt(selDay[0]);
            mm = Integer.parseInt(selDay[1]);
            dd = Integer.parseInt(selDay[2]);
            col = Integer.parseInt(selDay[3]);

            cal.addDecorator(new EventDecorator(colors[col], Collections.singleton(CalendarDay.from(yy,mm,dd))));

            MONTH2 = ((mm==2)&&(dd>18))||((mm==3)&&(dd<=18)); //2월19~3월18
            MONTH3 = ((mm==3)&&(dd>18))||((mm==4)&&(dd<=18)); //3월19~4월18
            MONTH4 = ((mm==4)&&(dd>18))||((mm==5)&&(dd<=18)); //4월19~5월18
            MONTH5 = ((mm==5)&&(dd>18))||((mm==6)&&(dd<=18)); //5월19~6월18
            MONTH6 = ((mm==6)&&(dd>18))||((mm==7)&&(dd<=18)); //6월19~7월18

            boolean[] MONTHS = {MONTH2,MONTH3,MONTH4,MONTH5,MONTH6};

            for(int i=0;i<MONTHS.length;i++){
                if(MONTHS[i]){
                    switch (col){
                        case 0: MON[i+1][0] += 1;break;
                        case 1: MON[i+1][1] +=1; break;
                        case 2: MON[i+1][1] +=1; break;
                    }
                }
            }

        }
        for(int i=0;i<MON.length;i++){  //[월][결석 , 조퇴지각, 총결석인정일수]
            if(MON[i][1]>2) {    //지각조퇴 3번이상일경우
                MON[i][2] = MON[i][0] + (MON[i][1] / 3);
            }else{
                MON[i][2] += MON[i][0];
            }
        }

    }

    public void delDeco(String ddd){
        String sdate=ddd;
        sdate=sdate.replaceAll("[a-z]","");
        sdate=sdate.replaceAll("[A-Z]","");
        sdate=sdate.replace("{","");
        sdate=sdate.replace("}","");
        String[] delday=sdate.split("-");
        yy = Integer.parseInt(delday[0]);
        mm = Integer.parseInt(delday[1]);
        dd = Integer.parseInt(delday[2]);
        cal.removeDecorators();
    }

    public void setClick(Button btn ,int i){
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                month.setText(Integer.toString(i+1)+"회차 총 결석일");
                total.setText(Integer.toString(MON[i][2]));
                slide.setPanelState(PanelState.COLLAPSED);
            }
        });
    }

    public void showDialog(CalendarDay date){
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this,R.style.MyDialogTheme);

        dlg.setSingleChoiceItems(versionArray, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    sel[0] =which;
            }
        });

        dlg.setNeutralButton("삭제", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                delData(date);
                delDeco(date.toString());
                showSel();
            }
        });

//                버튼 클릭시 동작
        dlg.setPositiveButton("확인",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                insertDb(date);
                showSel();

            }
        });

        dlg.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showSel();
            }
        });

        dlg.show();
    }

    public void delData(CalendarDay date){

        String sql = "select date_sel from mytable where date_sel like '"+date.toString()+"%';";
        Cursor c = db.rawQuery(sql, null);
        System.out.println("============================");
        while(c.moveToNext()){
            String s=c.getString(c.getColumnIndex("date_sel"));
            if(s.contains(date.toString())){
                String sql2 = "delete from mytable where date_sel like '"+date.toString()+"%';";
                db.execSQL(sql2);
                return;
            }
        }
        Toast.makeText(MainActivity.this,"삭제할게없쪄잉",Toast.LENGTH_SHORT).show();

    }
    public void insertDb(CalendarDay date ){
//        CalendarDay{2023-3-7}-0
//        CalendarDay{2023-3-16}-0
//        CalendarDay{2023-3-7}-1
        String sql = "select date_sel from mytable where date_sel like '"+date.toString()+"%';";
        Cursor c = db.rawQuery(sql, null);
        System.out.println("============================");
        while(c.moveToNext()){
            String s=c.getString(c.getColumnIndex("date_sel"));
            if(s.contains(date.toString())){
                Toast.makeText(MainActivity.this,"삭제하고 다시해주세염",Toast.LENGTH_SHORT).show();
                return;
            }

        }
        ContentValues values = new ContentValues();
        values.put("date_sel",date.toString()+"-"+Integer.toString(sel[0]));
        db.insert("mytable",null,values);
        sel[0]=0;       //다시 결석으로 초기화
    }
}