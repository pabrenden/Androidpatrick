package no.plotteriet.mintimeplan;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.time.*;
import java.util.*;

public class Notifier extends android.content.BroadcastReceiver {
 static final String CHANNEL="current_plan_v2", ALERT="changes_v2";
 static void channels(Context c){NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);NotificationChannel ongoing=new NotificationChannel(CHANNEL,"Aktiv timeplan",NotificationManager.IMPORTANCE_DEFAULT);ongoing.setDescription("Nåværende og neste elev med nedtelling");ongoing.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);n.createNotificationChannel(ongoing);NotificationChannel alerts=new NotificationChannel(ALERT,"Halvtimevarsler",NotificationManager.IMPORTANCE_HIGH);alerts.setDescription("Varsel når neste økt starter");n.createNotificationChannel(alerts);}
 static PendingIntent intent(Context c,int request){Intent i=new Intent(c,Notifier.class);i.setAction("no.plotteriet.mintimeplan.ALARM");return PendingIntent.getBroadcast(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static String time(int m){return String.format(Locale.ROOT,"%02d:%02d",m/60,m%60);}
 static String display(String s){return s.trim().isEmpty()?"Ikke registrert":s;}
 static void refresh(Context c){channels(c);LocalDateTime now=LocalDateTime.now();int d=now.getDayOfWeek().getValue()-1;int m=now.getHour()*60+now.getMinute();boolean active=d>=0&&d<5&&m>=MainActivity.START&&m<MainActivity.END;int slot=active?(m-MainActivity.START)/30:-1;
 NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){schedule(c);return;}
 if(!active){nm.cancel(101);schedule(c);return;}
 String name=display(MainActivity.value(c,d,slot,"name"));String next=slot+1<MainActivity.COUNT?display(MainActivity.value(c,d,slot+1,"name")):"Dagen er ferdig";int end=MainActivity.START+(slot+1)*30;boolean show=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("showNames",false);
 String title=show?"Nå: "+name:"Pågående økt";String subtitle=show?"Neste "+time(end)+": "+next:"Neste bytte kl. "+time(end);
 Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 long endMillis=System.currentTimeMillis()+((end-m)*60-now.getSecond())*1000L;
 Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(subtitle).setStyle(new Notification.BigTextStyle().bigText(subtitle)).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(true).setWhen(endMillis).setUsesChronometer(true).setChronometerCountDown(true).setVisibility(show?Notification.VISIBILITY_PUBLIC:Notification.VISIBILITY_PRIVATE);
 nm.notify(101,n.build());schedule(c);
 }
 static void schedule(Context c){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);PendingIntent p=intent(c,9);am.cancel(p);LocalDateTime now=LocalDateTime.now();LocalDateTime next=now.withHour(8).withMinute(30).withSecond(0).withNano(0);if(!next.isAfter(now)){int m=now.getHour()*60+now.getMinute();if(m<MainActivity.END){int upcoming=MainActivity.START+(((Math.max(m,MainActivity.START)-MainActivity.START)/30)+1)*30;next=now.withHour(upcoming/60).withMinute(upcoming%60).withSecond(0).withNano(0);}else next=next.plusDays(1);}
 while(next.getDayOfWeek().getValue()>5)next=next.plusDays(1);
 long millis=next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
 if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,p);
 else if(Build.VERSION.SDK_INT<31)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,p);
 else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,p);
 }
 @Override public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())){refresh(c);return;}refresh(c);LocalDateTime now=LocalDateTime.now();int d=now.getDayOfWeek().getValue()-1,m=now.getHour()*60+now.getMinute();if(d<0||d>4||m<MainActivity.START||m>=MainActivity.END)return;int slot=(m-MainActivity.START)/30;String name=display(MainActivity.value(c,d,slot,"name"));String note=MainActivity.value(c,d,slot,"note");boolean show=c.getSharedPreferences("plan",Context.MODE_PRIVATE).getBoolean("showNames",false);NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Notification.Builder n=new Notification.Builder(c,ALERT).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(show?"Ny økt: "+name:"Tid for neste økt").setContentText(show&&!note.isEmpty()?note:"Kl. "+time(m)).setAutoCancel(true).setVisibility(show?Notification.VISIBILITY_PUBLIC:Notification.VISIBILITY_PRIVATE);nm.notify(200+slot,n.build());}
}
