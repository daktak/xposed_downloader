package org.xposeddownloader;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.File;

public class MyCustomAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final File[] file;
    private static final String LOGTAG = LogUtil
            .makeLogTag(MainActivity.class);

    public MyCustomAdapter(Context context, String[] values, File[] file) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;
        this.file = file;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //LayoutInflater inflater = (LayoutInflater) context
        //       .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder holder;
        String s = values[position];
        int color = ContextCompat.getColor(context, R.color.colorBlack);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.rowlayout, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.label);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        try {
            for (int j = 0; j < file.length; j++) {

                if (s.equals(file[j].getName())) {
                    //Log.w("BasketBuild","have file: "+s+ ":"+file[j] + " : "+ j+"pos:" + position);
                    color = ContextCompat.getColor(context, R.color.disabledText);
                    //convertView.setEnabled(false);
                }
            }
        } catch (Exception e) {
            Log.w(LOGTAG, "Cant "+e.getMessage());
        }

        holder.text.setTextColor(color);
        holder.text.setText(s);

        return convertView;
    }

    static class ViewHolder {
        TextView text;
    }

}
