package no.plotteriet.mintimeplan;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.RemoteViews;
import java.time.*;
import java.util.*;

public class Notifier extends BroadcastReceiver {
 static final String CHANNEL="current_plan_v3", SOUND="changes_sound_v3", SILENT="changes_silent_v3";
 static final String START_ACTION="no.plotteriet.mintimeplan.START",WARN_ACTION="no.plotteriet.mintimeplan.WARN";
 static void channels(Context c){
 NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
 NotificationChannel ongoing=new NotificationChannel(CHANNEL,"Aktiv timeplan og tidslinje",NotificationManager.IMPORTANCE_DEFAULT);
 ongoing.setSound(null,null);ongoing.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);nm.createNotificationChannel(ongoing);
 NotificationChannel sound=new NotificationChannel(SOUND,"Byttevarsler med lyd",NotificationManager.IMPORTANCE_HIGH);nm.createNotificationChannel(sound);
 NotificationChannel silent=new NotificationChannel(SILENT,"Stille byttevarsler",NotificationManager.IMPORTANCE_LOW);silent.setSound(null,null);nm.createNotificationChannel(silent);
 }
 static PendingIntent pending(Context c,String action,int id){Intent i=new Intent(c,Notifier.class);i.setAction(action);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static boolean allowed(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
 static String display(String s){return s.trim().isEmpty()?"Ikke registrert":s;}
 static String time(int m){return MainActivity.time(m);}
 static void refresh(Context c){
 ScheduleWidget.update(c);channels(c);
 NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
 AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 boolean enabled=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("notificationsEnabled",false);
 if(!enabled){nm.cancel(101);am.cancel(pending(c,START_ACTION,9));am.cancel(pending(c,WARN_ACTION,10));return;}
 schedule(c);if(!allowed(c))return;
 LocalDateTime now=LocalDateTime.now();int d=now.getDayOfWeek().getValue()-1,m=now.getHour()*60+now.getMinute();
 boolean active=d<5&&m>=MainActivity.START&&m<MainActivity.END;
 boolean show=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("showNames",false);
 Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setVisibility(show?Notification.VISIBILITY_PUBLIC:Notification.VISIBILITY_PRIVATE);
 RemoteViews expanded=new RemoteViews(c.getPackageName(),R.layout.notification_expanded);
 if(active){
 int slot=(m-MainActivity.START)/30,end=MainActivity.START+(slot+1)*30;
 String name=display(MainActivity.value(c,d,slot,"name"));String next=slot+1<MainActivity.COUNT?display(MainActivity.value(c,d,slot+1,"name")):"Dagen ferdig";String later=slot+2<MainActivity.COUNT?display(MainActivity.value(c,d,slot+2,"name")):"Dagen ferdig";
 int remaining=(end-m)*60-now.getSecond();long endMs=System.currentTimeMillis()+remaining*1000L;
 n.setContentTitle(show?"Nå: "+name:"Pågående økt").setContentText(show?"Neste "+time(end)+": "+next:"Neste bytte "+time(end)).setWhen(endMs).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true);
 expanded.setTextViewText(R.id.notif_heading,"MIN TIMEPLAN · "+time(MainActivity.START+slot*30)+"–"+time(end));
 expanded.setTextViewText(R.id.notif_current,show?"NÅ · "+name:"NÅ · Pågående økt");
 expanded.setTextViewText(R.id.notif_next,show?"NESTE "+time(end)+" · "+next:"NESTE "+time(end));
 expanded.setTextViewText(R.id.notif_later,show&&slot+2<MainActivity.COUNT?"DERETTER "+time(end+30)+" · "+later:slot+2<MainActivity.COUNT?"DERETTER "+time(end+30):"Siste økt i dag");
 expanded.setProgressBar(R.id.notif_progress,1800,1800-remaining,false);
 }else{
 int nextDay=d;String nextTime="08:30";
 if(d<5&&m<MainActivity.START)nextDay=d;
 else{do{nextDay=(nextDay+1)%7;}while(nextDay>4);nextTime+=" · "+MainActivity.DAYS[nextDay];}
 String name=display(MainActivity.value(c,nextDay,0,"name"));
 n.setContentTitle("Min timeplan er aktiv").setContentText(show?"Neste: "+name+" · "+nextTime:"Neste økt: "+nextTime);
 expanded.setTextViewText(R.id.notif_heading,"MIN TIMEPLAN · KLAR");
 expanded.setTextViewText(R.id.notif_current,"Ingen aktiv økt");
 expanded.setTextViewText(R.id.notif_next,show?"NESTE "+nextTime+" · "+name:"NESTE "+nextTime);
 expanded.setTextViewText(R.id.notif_later,"Varsler er aktivert");
 expanded.setProgressBar(R.id.notif_progress,1800,0,false);
 }
 n.setCustomBigContentView(expanded);
 nm.notify(101,n.build());
 }
 static void setAlarm(Context c,PendingIntent pi,LocalDateTime when){
 AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 long at=when.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
 if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
 else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
 }
 static LocalDateTime nextStart(LocalDateTime now){
 LocalDateTime next=now.withHour(8).withMinute(30).withSecond(0).withNano(0);
 if(!next.isAfter(now)){
 int m=now.getHour()*60+now.getMinute();
 if(m<MainActivity.END){int upcoming=MainActivity.START+(((Math.max(m,MainActivity.START)-MainActivity.START)/30)+1)*30;next=now.withHour(upcoming/60).withMinute(upcoming%60).withSecond(0).withNano(0);}
 else next=next.plusDays(1);
 }
 while(next.getDayOfWeek().getValue()>5)next=next.plusDays(1);
 return next;
 }
 static void schedule(Context c){
 AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 PendingIntent start=pending(c,START_ACTION,9),warning=pending(c,WARN_ACTION,10);
 am.cancel(start);am.cancel(warning);
 LocalDateTime now=LocalDateTime.now();
 LocalDateTime next=nextStart(now);setAlarm(c,start,next);
 int lead=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getInt("warningMinutes",0);
 if(lead>0){
 LocalDateTime warn=next.minusMinutes(lead);
 if(!warn.isAfter(now)){LocalDateTime after=next.plusSeconds(1);warn=nextStart(after).minusMinutes(lead);}
 if(warn.isAfter(now))setAlarm(c,warning,warn);
 }
 }
 static void alert(Context c,boolean warning){
 if(!allowed(c))return;
 LocalDateTime now=LocalDateTime.now();int d=now.getDayOfWeek().getValue()-1,m=now.getHour()*60+now.getMinute();
 int target=warning?m+c.getSharedPreferences("plan",Context.MODE_PRIVATE).getInt("warningMinutes",0):m;
 if(d>4||target<MainActivity.START||target>=MainActivity.END)return;
 int slot=(target-MainActivity.START)/30;
 String name=display(MainActivity.value(c,d,slot,"name"));
 boolean show=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("showNames",false);
 boolean sound=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("alertSound",true);
 String channel=sound?SOUND:SILENT;
 String title=warning?"Snart elevbytte":"Tid for neste økt";
 String subtitle=warning?"Om "+c.getSharedPreferences("plan",Context.MODE_PRIVATE).getInt("warningMinutes",0)+" min · "+time(target):"Kl. "+time(target);
 if(show)subtitle+=" · "+name;
 Notification.Builder n=new Notification.Builder(c,channel).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(subtitle).setAutoCancel(true).setVisibility(show?Notification.VISIBILITY_PUBLIC:Notification.VISIBILITY_PRIVATE);
 ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(warning?301:302,n.build());
 }
 @Override public void onReceive(Context c,Intent i){
 if(!c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("notificationsEnabled",false))return;
 String action=i.getAction();
 if(START_ACTION.equals(action)){refresh(c);alert(c,false);}
 else if(WARN_ACTION.equals(action)){alert(c,true);schedule(c);}
 else refresh(c);
 }
}
