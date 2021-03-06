package net.cyclestreets.maps;

import net.cyclestreets.util.MessageBox;

import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;

import net.cyclestreets.maps.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.support.v4.content.LocalBroadcastManager;

public class MapsDownloader extends Activity
{
  private ProgressBar progressBar_;
  private TextView progressText_;
  private TextView progressBarText_;
  private TextView percentText_;
  private TextView remainingText_;
  private BroadcastReceiver messageReceiver_;
  
  @Override
  public void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.main);
    
    try
    {
      if(isMapAlreadyDownloaded())
        launchCycleStreets();
    } 
    catch (Exception e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } // if ...
    
    progressText_ = (TextView)findViewById(R.id.progress_text);
    progressBar_ = (ProgressBar)findViewById(R.id.progress_bar);
    progressBarText_ = (TextView)findViewById(R.id.progress_bar_text);
    percentText_ = (TextView)findViewById(R.id.percent_text);
    remainingText_ = (TextView)findViewById(R.id.time_remaining);
    
    messageReceiver_ = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String text = intent.getStringExtra("text");
        long current = intent.getLongExtra("currentbytes", 0);
        long total = intent.getLongExtra("totalbytes", 0);
        long time = intent.getLongExtra("time", 0);
        boolean complete = intent.getBooleanExtra("complete", false);

        progressText_.setText(text);        

        if(complete)
        {
          current = total = progressBar_.getMax();
          launchCycleStreets();
        } // if ...
        
        if(total > 0) 
        {
          progressBar_.setIndeterminate(false);
          progressBar_.setProgress((int)(current>>8));
          progressBar_.setMax((int)(total>>8));
          progressBarText_.setText(Helpers.getDownloadProgressString(current, total));
          percentText_.setText(Helpers.getDownloadProgressPercent(current, total));
          remainingText_.setText(getString(R.string.time_remaining_notification,
                                 Helpers.getTimeRemaining(time)));
          progressText_.setText(R.string.state_downloading);
        } // if ...
      } // onReceive
    };

    final IntentFilter download = new IntentFilter("download");
    broadcastManager().registerReceiver(messageReceiver_, download);
  } // onCreate
  
  @Override
  protected void onDestroy() {
    broadcastManager().unregisterReceiver(messageReceiver_);
    super.onDestroy();
  } // onPause

  private LocalBroadcastManager broadcastManager()
  {
    return LocalBroadcastManager.getInstance(this);
  } // broadcastManager
  
  //////////////////////////////////  
  private boolean isMapAlreadyDownloaded() throws Exception
  {
    int result = DownloaderClientMarshaller.startDownloadServiceIfRequired(
          this, pendingIntent(), MapFileDownloaderService.class);
      
    return (DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED == result);
  } // isMapAlreadyDownloaded
  
  private PendingIntent pendingIntent()
  {
    final Intent launcher = getIntent();
    final Intent fromNotification = new Intent(this, getClass());
    fromNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    fromNotification.setAction(launcher.getAction());
    
    if (launcher.getCategories() != null) {
      for (String cat : launcher.getCategories()) {
        fromNotification.addCategory(cat);
      } // for ...
    } // if ...
    
    return PendingIntent.getActivity(this, 0, fromNotification, PendingIntent.FLAG_UPDATE_CURRENT);
  } // createPendingIntent
  
  private void launchCycleStreets()
  {
    if(startCycleStreetsApp())
      return;
    
    MessageBox.YesNo(this, 
                     R.string.download_cyclestreets, 
                     new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface arg0, int arg1) {  
                         openGooglePlay();
                         finish();
                       } // onClick
                     },
                     new DialogInterface.OnClickListener() { 
                       public void onClick(DialogInterface arg0, int arg1) {
                         finish();
                       } // onClick
                     });
  } // launchCycleStreets
  
  private boolean startCycleStreetsApp()
  {
    try {
      final Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.setComponent(ComponentName.unflattenFromString("net.cyclestreets/net.cyclestreets.CycleStreets"));
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      intent.putExtra("mapfile", getPackageName());
      startActivity(intent);
      finish();
      return true;
    } // try 
    catch (final Exception e) {
      // oh, is cyclestreets not installed
      return false;
    } // catch
  } // startCycleStreetsApp
  
  private void openGooglePlay()
  {      
    final Intent play = new Intent(Intent.ACTION_VIEW);
    play.setData(Uri.parse("market://details?id=net.cyclestreets"));
    startActivity(play);
  } // openGooglePlay
} // MapsDownloader