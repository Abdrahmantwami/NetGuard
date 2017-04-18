package eu.faircode.netguard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

/**
 * Created by Carlos on 4/18/17.
 */

public class ActivityWeb extends ActivityBase {

    @Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        AdapterWeb adapterWeb = new AdapterWeb();
        recyclerView.setAdapter(adapterWeb);
    }


    @Override public String getTag() {
        return "ActivityWeb";
    }

    private class AdapterWeb extends RecyclerView.Adapter<AdapterWeb.ViewHolder> implements
            SwitchCompat.OnCheckedChangeListener {
        private String titles[];
        private String descs[];
        private final SharedPreferences mPreferences;

        public AdapterWeb() {
            super();

            titles = getResources().getStringArray(R.array.itemWebTitles);
            descs = getResources().getStringArray(R.array.itemWebDescs);

            mPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityWeb.this);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_web, parent, false);
            return new ViewHolder(view, this);
        }

        @Override public void onBindViewHolder(final ViewHolder holder, final int position) {

            holder.itemTitle.setText(titles[position]);
            holder.itemTitle.setText(descs[position]);
            holder.itemSwitch.setTag(position);
            holder.itemSwitch.setChecked(mPreferences.getBoolean("item_web_switch" + position,
                    false));
        }

        @Override public int getItemCount() {
            return titles.length;
        }

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            Object tag = buttonView.getTag();
            if (tag == null || tag instanceof Integer) {
                Log.e(getTag(), String.format("switch's tag is wrong, view: %s, tag: %s",
                        buttonView, tag));
                return;
            }

            mPreferences.edit().putBoolean("item_web_switch" + ((int) tag), isChecked).apply();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView itemTitle;
            TextView itemDesc;
            SwitchCompat itemSwitch;

            ViewHolder(final View itemView, CompoundButton.OnCheckedChangeListener listener) {
                super(itemView);

                itemTitle = ((TextView) itemView.findViewById(R.id.itemTitle));
                itemDesc = (TextView) itemView.findViewById(R.id.itemDesc);
                itemSwitch = (SwitchCompat) itemView.findViewById(R.id.itemSwitch);
                itemView.setOnClickListener(this);
                itemSwitch.setOnCheckedChangeListener(listener);
            }

            @Override public void onClick(final View v) {
                itemSwitch.setChecked(!itemSwitch.isChecked());
            }
        }
    }

}
