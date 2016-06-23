package org.xposeddownloader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * daktak
 */

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {
    private static final String LOGTAG = LogUtil
            .makeLogTag(MainActivity.class);
    public static String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] perms2 = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_PREFS = 99;
    private static final int RC_EXT_WRITE =1;
    private static final int RC_EXT_READ=2;
    public static MainActivity instance = null;

    private ArrayList<String> urls = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        setApi(this);

        String[] names = new String[] {getString(R.string.loading)};
        ListView mainListView = (ListView) findViewById( R.id.listView );
        ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);
        // Set the ArrayAdapter as the ListView's adapter.
        mainListView.setAdapter( listAdapter );

        if (!(EasyPermissions.hasPermissions(this, perms))) {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, getString(R.string.extWritePerm), RC_EXT_WRITE, perms);
            //otherwise use app
        }

        if (!(EasyPermissions.hasPermissions(this, perms2))) {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, getString(R.string.extReadPerm), RC_EXT_READ, perms2);
            //otherwise use app
        }

        setAlarm(this);
        run(this);

    }

    @TargetApi(21)
    public String get64(){
        String out = "";
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            out += "64";
        }
        return out;
    }


    public void setApi(Context context) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String api = mySharedPreferences.getString("prefProject",getString(R.string.project_val));
        String newApi = "sdk" +Build.VERSION.SDK_INT;
        SharedPreferences.Editor prefEdit = mySharedPreferences.edit();
        //and newApi in list?
        Log.d(LOGTAG, "Detected api: " + newApi);
        if  (!Arrays.asList(R.array.api).contains(newApi)) {
            newApi = getString(R.string.apidefualt);
        }
        if (api.equalsIgnoreCase("unset")) {
            prefEdit.remove("prefProject");
            prefEdit.putString("prefProject", newApi);
            Log.d(LOGTAG, "Setting api: " + newApi);
        }
        String arch = mySharedPreferences.getString("prefDevice",getString(R.string.device_val));
        String arch1 = Build.CPU_ABI;
        String newArch = arch1.substring(0,3).toLowerCase(Locale.ENGLISH);

        newArch += get64();
        Log.d(LOGTAG, "Detected arch: " + newArch);
        //and newArc8h in list?
        if  (!Arrays.asList(R.array.arch).contains(newArch)) {
            newArch = getString(R.string.archdefault);
        }
        if (arch.equalsIgnoreCase("unset")){
            prefEdit.remove("prefDevice");
            prefEdit.putString("prefDevice",newArch);
            Log.d(LOGTAG, "Setting arch: " + newArch);
        }
        prefEdit.apply();
    }

    public void setAlarm(Context context){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean daily = mySharedPreferences.getBoolean("prefDailyDownload",false);
        if (daily) {
            Log.d(LOGTAG, "Setting daily alarm");
            setRecurringAlarm(context);
        } else {
            CancelAlarm(context);
        }
    }
    public void setRecurringAlarm(Context context) {

        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int hour =  Integer.parseInt(mySharedPreferences.getString("prefHour", getString(R.string.hour_val)));
        int minute = Integer.parseInt(mySharedPreferences.getString("prefMinute", getString(R.string.minute_val)));
        Calendar updateTime = Calendar.getInstance();
        //updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        updateTime.set(Calendar.HOUR_OF_DAY, hour);
        updateTime.set(Calendar.MINUTE, minute);

        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
                0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(
                Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                updateTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, recurringDownload);

    }

    public void CancelAlarm(Context context)
    {
        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
                0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(
                Context.ALARM_SERVICE);
        alarms.cancel(recurringDownload);
    }

    public void run(Context context) {
        //new ParseURL().execute(new String[]{buildPath(context)});
        Intent service = new Intent(context, Download.class);
        service.putExtra("url",buildPath(context));
        service.putExtra("action",1);
        context.startService(service);
    }

    public String buildPath(Context context) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String base = mySharedPreferences.getString("prefBase",getString(R.string.base_val)).trim();
        String prefix = mySharedPreferences.getString("prefPrefix",getString(R.string.prefix_val)).trim();
        String project = mySharedPreferences.getString("prefProject",getString(R.string.project_val)).trim();
        String device = mySharedPreferences.getString("prefDevice",getString(R.string.device_val)).trim();
        Uri builtUri = Uri.parse(base+"/"+prefix)
                .buildUpon()
                .appendPath(project)
                .appendPath(device)
                .build();
        return builtUri.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent prefs = new Intent(getBaseContext(), SetPreferenceActivity.class);
            startActivityForResult(prefs, REQUEST_PREFS);
            run(this);
            setAlarm(this);
            return true;
        }
        if (id == R.id.action_reboot) {
            ExecuteAsRootBase e = new ExecuteAsRootBase() {
                    @Override
                    protected ArrayList<String> getCommandsToExecute() {
                        ArrayList<String> a = new ArrayList<String>();
                        a.add("reboot recovery");
                        return a;
                    }
                };
            e.execute();
            return true;
        }
        if (id == R.id.action_apkdl) {

            Intent service = new Intent(this, Download.class);
            service.putExtra("url",getString(R.string.xdathread));
            service.putExtra("action",4);
            this.startService(service);

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied

    }

    public String getBaseUrl() {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String prefix = mySharedPreferences.getString("prefBase",getString(R.string.base_val)).trim()+"/"
                + mySharedPreferences.getString("prefPrefix",getString(R.string.prefix_val)).trim() + "/"
                + mySharedPreferences.getString("prefProject",getString(R.string.project_val)).trim() + "/"
                + mySharedPreferences.getString("prefDevice",getString(R.string.device_val)).trim() + "/";
        return prefix;
    }

    public void setList(List<String> values)  {
        ArrayList<String> names = new ArrayList<String>();
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String directory = mySharedPreferences.getString("prefDirectory",Environment.DIRECTORY_DOWNLOADS).trim();
        boolean external = mySharedPreferences.getBoolean("prefExternal",false);
        File direct;
        if (external){
            directory = Environment.DIRECTORY_DOWNLOADS;
            direct = new File(directory);
        } else {
            if (!(directory.startsWith("/"))) {
                directory = "/" + directory;
            }
            direct = new File(Environment.getExternalStorageDirectory() + directory);

            if (!direct.exists()) {
                direct.mkdirs();
            }
        }
        File file[] = new File[0];
        if (EasyPermissions.hasPermissions(this, perms2)) {
            File f = new File(direct.getAbsolutePath());
            File file1[] = f.listFiles();
            file = file1.clone();
        }

        for (String i : values) {
            i = i.trim();
            int slash = i.lastIndexOf("/")+1;
            try {
                String filename = i.substring(slash);
                /*
                if (EasyPermissions.hasPermissions(this, perms2)) {
                    /*
                    for (int j = 0; j < file.length; j++) {
                        if (filename.equals(file[j].getName())) {
                            Log.d(LOGTAG,filename+" exists");
                            filename += " Have";
                        }
                    }

                }*/

                names.add(filename);
            } catch (Exception e){
                Log.w(LOGTAG, "Cant find slash in "+i);
                names.add(i);
            }


            String prefix = "";
            if (!(i.startsWith("http"))) {
                prefix = getBaseUrl();
            }
            urls.add(prefix+i);


        }
        //newest on top
        Collections.reverse(urls);
        Collections.reverse(names);
        String[] namesS = new String[names.size()];
        namesS = names.toArray(namesS);
        // Find the ListView resource.
        ListView mainListView = (ListView) findViewById( R.id.listView );

        MyCustomAdapter listAdapter = new MyCustomAdapter(this, namesS, file);
        //ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);

        // Set the ArrayAdapter as the ListView's adapter.
        mainListView.setAdapter( listAdapter );

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            if (view.isEnabled()) {
                String url = urls.get(position);
                Context context = getBaseContext();
                Intent service = new Intent(context, Download.class);
                service.putExtra("url", url.toString());
                service.putExtra("action", 2);
                context.startService(service);

                //new ParseURLDownload().execute(new String[]{url.toString()});

            } else {
                Log.d(LOGTAG, "Entry disabled");
            }
          }
        });
    }

    	/**
	 * Executes commands as root user
	 * @author http://muzikant-android.blogspot.com/2011/02/how-to-get-root-access-and-execute.html
	 */
	public abstract class ExecuteAsRootBase {
	  public final boolean execute() {
	    boolean retval = false;
	    try {
	      ArrayList<String> commands = getCommandsToExecute();
	      if (null != commands && commands.size() > 0) {
	        Process suProcess = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

	        // Execute commands that require root access
	        for (String currCommand : commands) {
	          os.writeBytes(currCommand + "\n");
	          os.flush();
	        }

	        os.writeBytes("exit\n");
	        os.flush();

	        try {
	          int suProcessRetval = suProcess.waitFor();
	          if (255 != suProcessRetval) {
	            // Root access granted
	            retval = true;
	          } else {
	            // Root access denied
	            retval = false;
	          }
	        } catch (Exception ex) {
	          Log.e(LOGTAG, "Error executing root action\n"+ ex.toString());
	        }
	      }
	    } catch (IOException ex) {
	      Log.w(LOGTAG, "Can't get root access", ex);
	    } catch (SecurityException ex) {
	      Log.w(LOGTAG, "Can't get root access", ex);
	    } catch (Exception ex) {
	      Log.w(LOGTAG, "Error executing internal operation", ex);
	    }

	    return retval;
	  }

	  protected abstract ArrayList<String> getCommandsToExecute();
	}


    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance = null;
    }

}
