package eu.faircode.netguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.faircode.netguard.monitor.Scan;

import static eu.faircode.netguard.AdapterVirusList.Level.Danger;
import static eu.faircode.netguard.AdapterVirusList.Level.Disable;
import static eu.faircode.netguard.AdapterVirusList.Level.Safe;
import static eu.faircode.netguard.AdapterVirusList.Level.Suspicious;

/**
 * Created by Carlos on 4/10/17.
 */

public class AdapterVirusList extends BaseAdapter implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    private static final String TAG = "AdapterVirusList";
    private Context mContext;
    private int textColorSecondary;
    private List<Scan> mData = new ArrayList<>();
    private final String virus_msg_time_danger;
    private final String virus_msg_time_suspicious;
    private final String virus_msg_time_safe;
    private final String virus_msg_title_danger;
    private final String virus_msg_title_suspicious;
    private final String virus_msg_title_safe;
    private final String virus_msg_info2_danger;
    private final String virus_msg_info2_suspicious;
    private final String virus_msg_info2_safe;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(" MM-dd HH:MM", Locale
            .getDefault());

    public void add(List<Scan> scans) {
        mData.addAll(scans);
        notifyDataSetChanged();
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    private int day = 0;
    private int sum = 0;
    private int progress = 0;
    private int not_handled = 0;
    private int handled = 0;
    private SharedPreferences mPreferences;


    public AdapterVirusList(Context context) {
        mContext = context;

        TypedArray typedArray = context.obtainStyledAttributes(R.style
                        .TextAppearance_AppCompat_Notification_Time,
                new int[]{android.R.attr.textColor});
        textColorSecondary = typedArray.getInt(0, Color.BLACK);
        typedArray.recycle();

        this.virus_msg_time_danger = context.getResources().getString(R.string
                .virus_msg_time_danger);
        this.virus_msg_time_suspicious = context.getResources().getString(R.string
                .virus_msg_time_suspicious);
        this.virus_msg_time_safe = context.getResources().getString(R.string.virus_msg_time_safe);
        this.virus_msg_title_danger = context.getResources().getString(R.string
                .virus_msg_title_danger);
        this.virus_msg_title_suspicious = context.getResources().getString(R.string
                .virus_msg_title_suspicious);
        this.virus_msg_title_safe = context.getResources().getString(R.string
                .virus_msg_title_safe);
        this.virus_msg_info2_danger = context.getResources().getString(R.string
                .virus_msg_info2_danger);
        this.virus_msg_info2_suspicious = context.getResources().getString(R.string
                .virus_msg_info2_suspicious);
        this.virus_msg_info2_safe = context.getResources().getString(R.string
                .virus_msg_info2_safe);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);

        long first = mPreferences.getLong("first_enable_virus", 0);
        if (first == 0) {
            day = 0;
        } else {
            day = (int) ((System.currentTimeMillis() - first) / 1000 / 3600 / 24);
        }
        // TODO what is styled attr
    }

    @Override public int getCount() {
        return mData.size() + 1;
    }

    @Override public Object getItem(final int position) {
        return mData.get((int) getItemId(position));
    }

    public void onDestory() {
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override public long getItemId(final int position) {
        return position - 1;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View itemView = convertView;
        int type = getItemViewType(position);

        if (itemView == null) {
            ViewHolder vh;
            if (type == 0) {
                vh = new StatsViewHolder(LayoutInflater.from(mContext).inflate(R.layout
                        .head_virus, parent, false));
            } else {
                vh = new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout
                        .item_virus, parent, false));
            }
            itemView = vh.itemView;
            itemView.setTag(vh);
        }


        ViewHolder viewHolder = (ViewHolder) itemView.getTag();

        if (type == 1) {
            Scan scan = (Scan) getItem(position);
            viewHolder.textInfo.setText(scan.path);
            String time = mSimpleDateFormat.format(new Date(scan.time));

            switch (scan.which()) {
                case SkipLarge:
                case SkipSafe:
                case Safe:
                    viewHolder.level(Safe);
                    viewHolder.textTime.setText(String.format(virus_msg_time_safe, time));
                    viewHolder.textTitle.setText(virus_msg_title_safe);
                    viewHolder.textInfo2.setText(String.format(virus_msg_info2_safe, scan
                            .totalAvs));
                    break;
                case Danger:
                    double ratio = scan.totalDetectedAvs / ((double) scan.totalAvs);


                    if (ratio > 0.2f) {
                        viewHolder.level(Danger);
                        viewHolder.textTime.setText(String.format(virus_msg_time_danger, time));
                        viewHolder.textTitle.setText(virus_msg_title_danger);
                        viewHolder.textInfo2.setText(String.format(virus_msg_info2_danger, scan
                                .totalDetectedAvs, scan.totalAvs));
                    } else {
                        viewHolder.level(Suspicious);
                        viewHolder.textTime.setText(String.format(virus_msg_time_suspicious, time));
                        viewHolder.textTitle.setText(virus_msg_title_suspicious);
                        viewHolder.textInfo2.setText(String.format(virus_msg_info2_suspicious, scan
                                .totalDetectedAvs, scan.totalAvs));
                    }

                    break;
                case Queue:
                case Stub:
                    Log.e(TAG, String.format("not show scan %s", scan));
                    break;
            }
        } else if (type == 0) {
            if (mPreferences.getBoolean("enabled_virus", false)) {
                if (not_handled == 0) {
                    viewHolder.level(Safe);
                } else {
                    viewHolder.level(Suspicious);
                }
                viewHolder.textTime.setText(mContext.getString(R.string.virus_head_enable));
            } else {
                viewHolder.level(Disable);
                viewHolder.textTime.setText(mContext.getString(R.string.virus_head_disable));
            }

            viewHolder.textTitle.setText(mContext.getString(R.string.virus_head_title, day));
            viewHolder.textInfo.setText(mContext.getString(R.string.virus_head_not_handled,
                    not_handled));
            viewHolder.textInfo2.setText(mContext.getString(R.string.virus_head_count, sum,
                    progress, handled));


        }
        return itemView;
    }

    @Override public int getItemViewType(final int position) {
        return position == 0 ? 0 : 1;
    }

    private void updateCount() {
        sum = mPreferences.getInt("scan_sum", 0);
        progress = mPreferences.getInt("scan_progress", 0);
        not_handled = mPreferences.getInt("scan_not_handled", 0);
        handled = mPreferences.getInt("scan_handled", 0);

        notifyDataSetChanged();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String
            key) {
        if (key.startsWith("scan_") || key.equals("enabled_virus")) { updateCount(); }
    }


    enum Level {Danger, Suspicious, Safe, Disable}

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTime;

        TextView textTitle;
        TextView textInfo;
        TextView textInfo2;
        LinearLayout linearLayout;


        ViewHolder level(Level level) {
            if (this instanceof StatsViewHolder) {
                switch (level) {
                    case Danger:
                        ((StatsViewHolder) this).statusImage.setImageResource(R.drawable
                                .ic_security_error);
                        break;
                    case Suspicious:
                        ((StatsViewHolder) this).statusImage.setImageResource(R.drawable
                                .ic_security_warning);
                        break;
                    case Safe:
                        ((StatsViewHolder) this).statusImage.setImageResource(R.drawable
                                .ic_security_safe);
                        break;
                    case Disable:
                        ((StatsViewHolder) this).statusImage.setImageResource(R.drawable
                                .ic_security_block);
                        break;
                }
                return this;
            }
            switch (level) {
                case Danger:
                    linearLayout.setBackgroundResource(R.drawable.bg_btn_white_red_square);
                    textTime.setBackgroundResource(R.drawable.bg_title_red);
                    textTime.setTextColor(mContext.getResources().getColor(R.color.colorWhite));
                    break;
                case Suspicious:
                    linearLayout.setBackgroundResource(R.drawable.bg_btn_white);
                    textTime.setBackgroundResource(R.drawable.bg_title_yellow);
                    textTime.setTextColor(textColorSecondary);
                    break;
                case Safe:
                    linearLayout.setBackgroundResource(R.drawable.bg_btn_white);
                    textTime.setBackground(null);
                    textTime.setTextColor(textColorSecondary);
                    break;
            }

            return this;
        }

        public ViewHolder(final View itemView) {
            super(itemView);

            linearLayout = (LinearLayout) itemView;
            textTime = (TextView) itemView.findViewById(R.id.textTime);
            textTitle = (TextView) itemView.findViewById(R.id.textTitle);
            textInfo = (TextView) itemView.findViewById(R.id.textInfo);
            textInfo2 = (TextView) itemView.findViewById(R.id.textInfo2);
        }
    }

    public class StatsViewHolder extends ViewHolder {
        ImageView statusImage;

        StatsViewHolder(final View itemView) {
            super(itemView);
            statusImage = (ImageView) itemView.findViewById(R.id.statusImage);
        }
    }
}
