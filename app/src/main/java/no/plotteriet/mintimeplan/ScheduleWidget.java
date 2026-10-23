package no.plotteriet.mintimeplan;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.time.*;
import java.util.*;

public class ScheduleWidget extends AppWidgetProvider {
 static void update(Context c){
 AppWidgetManager manager=AppWidgetManager.getInstance(c);
 int[] ids=manager.getAppWidgetIds(new ComponentName(c,ScheduleWidget.class));
 if(ids.length==0)return;
 LocalDateTime now=LocalDateTime.now();int day=now.getDayOfWeek().getValue()-1;int min=now.getHour()*60+now.getMinute();
 boolean active=day>=0&&day<5&&min>=MainActivity.START&&min<MainActivity.END;
 String current="Ingen aktiv økt",next="Ingen flere økter",later="Deretter: —",timer="Utenfor skoletid";int progress=0;
 if(active){int slot=(min-MainActivity.START)/30;String a=MainActivity.value(c,day,slot,"name");String b=slot+1<MainActivity.COUNT?MainActivity.value(c,day,slot+1,"name"):"Dagen ferdig";
 current="Nå: "+(a.isEmpty()?"Ikke registrert":a)+" ("+MainActivity.time(MainActivity.START+slot*30)+"–"+MainActivity.time(MainActivity.START+(slot+1)*30)+")";
 next="Neste "+MainActivity.time(MainActivity.START+(slot+1)*30)+": "+(b.isEmpty()?"Ikke registrert":b);
 int secs=(MainActivity.START+(slot+1)*30-min)*60-now.getSecond();timer=String.format(Locale.ROOT,"Bytte om %02d:%02d",secs/60,secs%60);progress=1800-secs;if(slot+2<MainActivity.COUNT){String third=MainActivity.value(c,day,slot+2,"name");later="Deretter "+MainActivity.time(MainActivity.START+(slot+2)*30)+": "+(third.isEmpty()?"Ikke registrert":third);}else later="Siste økt i dag";
 }else if(day>=0&&day<5&&min<MainActivity.START){String b=MainActivity.value(c,day,0,"name");next="Neste 08:30: "+(b.isEmpty()?"Ikke registrert":b);timer="Starter kl. 08:30";}
 for(int id:ids){RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.widget_schedule);views.setTextViewText(R.id.widget_current,current);views.setTextViewText(R.id.widget_next,next);views.setTextViewText(R.id.widget_countdown,timer);views.setTextViewText(R.id.widget_later,later);views.setProgressBar(R.id.widget_progress,1800,progress,false);
 Intent open=new Intent(c,MainActivity.class);views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
 manager.updateAppWidget(id,views);}
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){update(c);}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction()))update(c);}
}
