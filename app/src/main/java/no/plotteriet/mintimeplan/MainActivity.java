package no.plotteriet.mintimeplan;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

public class MainActivity extends Activity {
 static final String[] DAYS={"Mandag","Tirsdag","Onsdag","Torsdag","Fredag"};
 static final int START=510,END=960,COUNT=15;
 static final int NAVY=Color.rgb(17,28,43),BLUE=Color.rgb(105,160,231),MUTED=Color.rgb(135,152,176),WHITE=Color.WHITE;
 SharedPreferences prefs; LinearLayout root; TextView countdown; Handler handler=new Handler(Looper.getMainLooper()); int day; boolean editing=false;
 static String time(int minute){return String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60);}
 static String key(int d,int i,String type){return "d"+d+"_s"+i+"_"+type;}
 static int today(){return LocalDate.now().getDayOfWeek().getValue()-1;}
 static String value(Context c,int d,int i,String type){return c.getSharedPreferences("plan",MODE_PRIVATE).getString(key(d,i,type),"");}
 static GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
 void add(LinearLayout parent,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(v,p);}
 LinearLayout column(int color,int radius,int padding){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setBackground(bg(color,dp(radius)));pad(c,padding,padding);return c;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);prefs=getSharedPreferences("plan",MODE_PRIVATE);day=Math.max(0,Math.min(4,today()));Notifier.channels(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);Notifier.refresh(this);show();handler.post(new Runnable(){public void run(){if(!editing&&countdown!=null)countdown.setText(remaining());handler.postDelayed(this,1000);}});}
 @Override protected void onResume(){super.onResume();Notifier.refresh(this);}
 @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){super.onRequestPermissionsResult(req,p,g);Notifier.refresh(this);}
 String remaining(){LocalDateTime n=LocalDateTime.now();int m=n.getHour()*60+n.getMinute();if(today()>4)return "Helg";if(m<START)return String.format(Locale.ROOT,"%02d:%02d:%02d", (START*60-(m*60+n.getSecond()))/3600,((START*60-(m*60+n.getSecond()))%3600)/60,(60-n.getSecond())%60);if(m>=END)return "Ferdig for i dag";int sec=(30-(m-START)%30)*60-n.getSecond();return String.format(Locale.ROOT,"%02d:%02d",sec/60,sec%60);}
 void show(){countdown=null;ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Color.rgb(243,246,250));root=new LinearLayout(this);root.setOrientation(1);pad(root,18,20);sc.addView(root);setContentView(sc);
 LinearLayout header=column(NAVY,20,20);add(root,header,0);add(header,label("MIN TIMEPLAN  •  BASE",13,BLUE,true),0);add(header,label(editing?"Rediger ukeplan":"Dagens oversikt",27,WHITE,true),12);
 LinearLayout tabs=new LinearLayout(this);add(header,tabs,18);Button home=button("Oversikt",!editing);Button edit=button("Rediger",editing);tabs.addView(home,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(edit,new LinearLayout.LayoutParams(0,dp(48),1));home.setOnClickListener(v->{editing=false;day=Math.max(0,Math.min(4,today()));show();});edit.setOnClickListener(v->{editing=true;show();});
 if(editing)editor();else dashboard();
 }
 Button button(String s,boolean active){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(active?NAVY:WHITE);b.setBackground(bg(active?Color.rgb(207,227,253):Color.rgb(43,62,84),dp(12)));return b;}
 void dashboard(){int d=today(),m=LocalTime.now().getHour()*60+LocalTime.now().getMinute(),slot=(m-START)/30;boolean active=d<5&&m>=START&&m<END;int current=active?slot:-1;String name=active?value(this,d,current,"name"):"";String next="";int nextMin=START;
 if(active){nextMin=START+(slot+1)*30;next=slot+1<COUNT?value(this,d,slot+1,"name"):"Dagen er ferdig";}else if(d<5&&m<START){next=value(this,d,0,"name");}else{next="Ingen flere økter";}
 LinearLayout now=column(WHITE,20,20);add(root,now,16);add(now,label("NÅVÆRENDE ØKT",12,MUTED,true),0);add(now,label(active?time(START+slot*30)+" – "+time(START+(slot+1)*30):"Utenfor timeplan",15,MUTED,false),8);add(now,label(active?(name.isEmpty()?"Ikke registrert":name):"Ingen aktiv økt",30,NAVY,true),6);
 String note=active?value(this,d,slot,"note"):"";if(!note.isEmpty())add(now,label(note,15,NAVY,false),12);
 LinearLayout timer=column(NAVY,20,20);add(root,timer,14);add(timer,label("TID TIL NESTE BYTTE",12,Color.rgb(176,194,220),true),0);add(timer,label("Neste: "+(next.isEmpty()?"Ikke registrert":next)+"  •  "+(nextMin<END?time(nextMin):"Etter siste økt"),19,WHITE,true),10);countdown=label(remaining(),36,WHITE,true);add(timer,countdown,8);
 LinearLayout nextCard=column(WHITE,20,20);add(root,nextCard,14);add(nextCard,label("NESTE ØKT  •  "+(nextMin<END?time(nextMin):"Etter siste økt"),12,MUTED,true),0);add(nextCard,label(next.isEmpty()?"Ikke registrert":next,25,NAVY,true),10);
 add(root,label("Ukedager: 08:30–16:00  •  Varsler ved øktbytte",13,MUTED,false),20);
 Button privacy=button(prefs.getBoolean("showNames",false)?"Låseskjerm: Vis navn (PÅ)":"Låseskjerm: Skjul navn (AV)",false);add(root,privacy,18);privacy.setOnClickListener(v->{prefs.edit().putBoolean("showNames",!prefs.getBoolean("showNames",false)).apply();Notifier.refresh(this);show();});
 Button test=button("Oppdater låseskjermvarsel",false);add(root,test,8);test.setOnClickListener(v->{Notifier.refresh(this);Toast.makeText(this,"Varsel oppdatert",Toast.LENGTH_SHORT).show();});
 if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);if(!a.canScheduleExactAlarms()){Button allow=button("Tillat presise halvtimevarsler",false);add(root,allow,8);allow.setOnClickListener(v->{Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);i.setData(android.net.Uri.parse("package:"+getPackageName()));startActivity(i);});}}
 }
 void editor(){Spinner spin=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,DAYS);spin.setAdapter(adapter);add(root,spin,15);spin.setSelection(day);LinearLayout list=new LinearLayout(this);list.setOrientation(1);add(root,list,6);spin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){day=pos;populate(list);}});populate(list);}
 void populate(LinearLayout list){list.removeAllViews();final int selected=day;for(int i=0;i<COUNT;i++){final int index=i;LinearLayout card=column(WHITE,16,16);add(list,card,10);add(card,label(time(START+i*30)+" – "+time(START+(i+1)*30),14,MUTED,true),0);EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(19);name.setHint("Elev, pause eller aktivitet");name.setText(value(this,selected,i,"name"));add(card,name,5);EditText note=new EditText(this);note.setTextSize(14);note.setHint("Beskjed for denne halvtimen");note.setMaxLines(3);note.setText(value(this,selected,i,"note"));add(card,note,3);name.addTextChangedListener(watch(s->{prefs.edit().putString(key(selected,index,"name"),s).apply();Notifier.refresh(this);}));note.addTextChangedListener(watch(s->{prefs.edit().putString(key(selected,index,"note"),s).apply();Notifier.refresh(this);}));}}
 interface Save{void apply(String s);} TextWatcher watch(Save s){return new TextWatcher(){public void beforeTextChanged(CharSequence c,int st,int n,int a){}public void onTextChanged(CharSequence c,int st,int before,int count){s.apply(c.toString());}public void afterTextChanged(Editable e){}};}
}
