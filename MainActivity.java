package no.plotteriet.mintimeplan;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.time.*;
import java.util.*;

public class MainActivity extends Activity {
    final String[] days={"Mandag","Tirsdag","Onsdag","Torsdag","Fredag"};
    final int START=8*60+30, END=16*60, COUNT=(END-START)/30;
    LinearLayout root, slots; Spinner daySpinner; TextView status;
    android.content.SharedPreferences prefs; Handler handler=new Handler(Looper.getMainLooper());
    int selectedDay=0; String lastNotice="";
    String time(int i){ int m=START+i*30;return String.format(java.util.Locale.ROOT,"%02d:%02d",m/60,m%60); }
    String key(int d,int i,String type){return "d"+d+"_s"+i+"_"+type;}
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("plan",MODE_PRIVATE); selectedDay=Math.min(4,Math.max(0,LocalDate.now().getDayOfWeek().getValue()-1));
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("plan","Timeplanvarsler",NotificationManager.IMPORTANCE_HIGH));
        build();handler.post(new Runnable(){public void run(){checkNotification();handler.postDelayed(this,15000);}});
    }
    void build(){ ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,28,22,28);root.setBackgroundColor(Color.rgb(249,248,244));scroll.addView(root);setContentView(scroll);
        TextView title=text("Min timeplan",28,true);root.addView(title);root.addView(text("Halvtimeplan med lokale notater og varsler",14,false));
        daySpinner=new Spinner(this);daySpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,days));root.addView(daySpinner);
        slots=new LinearLayout(this);slots.setOrientation(1);root.addView(slots);daySpinner.setSelection(selectedDay);daySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int p,long id){selectedDay=p;showSlots();}});
        status=text("Varsler sendes når appen er åpen. Bakgrunnsvarsler krever en senere versjon.",13,false);root.addView(status);showSlots();
    }
    TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.rgb(35,43,43));t.setTextSize(size);if(bold)t.setTypeface(null,1);t.setPadding(4,8,4,8);return t;}
    void showSlots(){slots.removeAllViews();for(int i=0;i<COUNT;i++){final int slot=i;LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(14,12,14,12);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,7,0,7);card.setBackgroundColor(Color.WHITE);slots.addView(card,cp);
        card.addView(text(time(i)+"–"+time(i+1),15,true));EditText student=new EditText(this);student.setSingleLine(true);student.setTextSize(17);student.setHint("Elev / pause / aktivitet");student.setText(prefs.getString(key(selectedDay,i,"name"),""));card.addView(student);
        EditText note=new EditText(this);note.setMinLines(1);note.setMaxLines(4);note.setTextSize(14);note.setHint("Beskjed for denne halvtimen");note.setText(prefs.getString(key(selectedDay,i,"note"),""));card.addView(note);
        student.addTextChangedListener(watch(s->prefs.edit().putString(key(selectedDay,slot,"name"),s).apply()));note.addTextChangedListener(watch(s->prefs.edit().putString(key(selectedDay,slot,"note"),s).apply()));
    }}
    interface Save {void save(String s);} TextWatcher watch(Save s){return new TextWatcher(){public void beforeTextChanged(CharSequence c,int st,int count,int after){}public void onTextChanged(CharSequence c,int st,int before,int count){s.save(c.toString());}public void afterTextChanged(Editable e){}};}
    void checkNotification(){LocalDateTime now=LocalDateTime.now();int dow=now.getDayOfWeek().getValue()-1;int minute=now.getHour()*60+now.getMinute();if(dow<0||dow>4||minute<START||minute>=END||now.getMinute()%30!=0)return;int slot=(minute-START)/30;String token=now.toLocalDate()+"/"+slot;if(token.equals(lastNotice))return;lastNotice=token;
        String name=prefs.getString(key(dow,slot,"name"),"");String note=prefs.getString(key(dow,slot,"note"),"");if(name.trim().isEmpty())return;
        Notification.Builder n=new Notification.Builder(this,"plan").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(time(slot)+" – "+name).setContentText(note.isEmpty()?"Ny halvtime":note).setStyle(new Notification.BigTextStyle().bigText(note.isEmpty()?"Ny halvtime":note)).setAutoCancel(true);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(slot,n.build());
    }
}
