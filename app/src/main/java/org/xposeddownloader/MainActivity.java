package org.xposeddownloader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
    public ArrayList<String> md5check = new ArrayList<String>();
    public ArrayList<String> names = new ArrayList<String>();
    public ArrayList<String> urls = new ArrayList<String>();
    public String directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        setApi(this);

        String[] namesA = new String[] {getString(R.string.loading)};
        ListView mainListView = (ListView) findViewById( R.id.listView );
        ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namesA);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setApi(this);
        setAlarm(this);
        run(this);
    }

    @TargetApi(21)
    public String get64(){
        String out = "";
        if (Build.VERSION.SDK_INT >= 21) {
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                out += "64";
            }
        }
        return out;
    }


    public void setApi(Context context) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String api = mySharedPreferences.getString("prefProject",getString(R.string.project_val));
        SharedPreferences.Editor prefEdit = mySharedPreferences.edit();

        if (api.equalsIgnoreCase(getString(R.string.auto_detect))) {
            String newApi = getString(R.string.apidefualt);
            try {
                newApi = "sdk" +Build.VERSION.SDK_INT;
            } catch (Exception e){
		newApi = getString(R.string.apidefualt);
                Log.w(LOGTAG, "Failed to get version");
            }
            //and newApi in list?
	    /*
            Log.d(LOGTAG, "Detected api: " + newApi);
            if  (!Arrays.asList(getResources().getStringArray(R.array.api)).contains(newApi)) {
                newApi = getString(R.string.apidefualt);
            }
	    */

            prefEdit.remove("prefProject");
            prefEdit.putString("prefProject", newApi);
            Log.d(LOGTAG, "Setting api: " + newApi);
        }
        String arch = mySharedPreferences.getString("prefDevice",getString(R.string.device_val));

        if (arch.equalsIgnoreCase(getString(R.string.auto_detect))) {
            String newArch = getString(R.string.archdefault);
            try {
                String arch1 = Build.CPU_ABI;
                newArch = arch1.substring(0,3).toLowerCase(Locale.ENGLISH);
                newArch += get64();
            } catch (Exception e) {
                Log.w(LOGTAG, "Failed to get arch");
            }
            Log.d(LOGTAG, "Detected arch: " + newArch);
            //and newArc8h in list?
            if  (!Arrays.asList(getResources().getStringArray(R.array.arch)).contains(newArch)) {
                newArch = getString(R.string.archdefault);
            }

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
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        directory = mySharedPreferences.getString("prefDirectory",Environment.DIRECTORY_DOWNLOADS).trim();
        boolean external = mySharedPreferences.getBoolean("prefExternal",false);
        md5check.clear();
        names.clear();
        urls.clear();

        if (external){
            directory = Environment.DIRECTORY_DOWNLOADS;
        }

        File direct = new File(Environment.getExternalStorageDirectory() + "/" + directory);
        if (!direct.exists()) {
            direct.mkdirs();
        }
        Log.w(LOGTAG, directory);
        File file[] = new File[0];
        if (EasyPermissions.hasPermissions(this, perms2)) {
            try {
                    file = direct.listFiles();
                } catch (Exception e) {
                    Log.w(LOGTAG, "Cant "+e.getMessage());
            }
        }

        for (String i : values) {
            String md5val = "";
            String name = i;
            i = i.trim();
            int slash = i.lastIndexOf("/")+1;
            try {
                name = i.substring(slash);
            } catch (Exception e){
                Log.w(LOGTAG, "Cant find slash in "+i);
            }
            names.add(name);

            String prefix = "";
            if (!(i.startsWith("http"))) {
                prefix = getBaseUrl();
            }
            urls.add(prefix+i);

            for (int k = 0; k < file.length; k++) {

                if (name.equals(file[k].getName())) {
                    md5val = "U";
                }
            }
            md5check.add(md5val);

        }
        //newest on top
        Collections.reverse(urls);
        Collections.reverse(names);
        Collections.reverse(md5check);
        String[] namesS = new String[names.size()];
        namesS = names.toArray(namesS);
        // Find the ListView resource.
        ListView mainListView = (ListView) findViewById( R.id.listView );

        MyCustomAdapter listAdapter = new MyCustomAdapter(this, namesS, file);
        //ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);

        // Set the ArrayAdapter as the ListView's adapter.
        mainListView.setAdapter( listAdapter );
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mainListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                boolean dis = true;
                                if (md5check.get(position).isEmpty()) { dis = false; };
                                return dis;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    final int pos = position;
                                    DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            File direct = new File(Environment.getExternalStorageDirectory() + "/" + directory+"/"+names.get(pos));
                                            Log.d(LOGTAG, "Delete " + direct.getName());
                                            if (direct.exists()&&direct.isFile()) { direct.delete(); }

                                            if (MainActivity.instance != null) {
                                                run(MainActivity.instance);
                                            }
                                        }
                                    };
                                    message_dialog_yes_no(getString(R.string.delete) + " " + names.get(pos)+"?" , yesListener);
                                }
                            }
                        });
        mainListView.setOnTouchListener(touchListener);

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

    public void message_dialog_yes_no (String msg, DialogInterface.OnClickListener yesListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), yesListener)
                .setNegativeButton(getString(R.string.no),  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }})
                .show();
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
