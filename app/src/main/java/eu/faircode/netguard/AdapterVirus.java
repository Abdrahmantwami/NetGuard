package eu.faircode.netguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import static eu.faircode.netguard.AdapterVirus.Level.Danger;
import static eu.faircode.netguard.AdapterVirus.Level.Safe;
import static eu.faircode.netguard.AdapterVirus.Level.Suspicious;

/**
 * Created by Carlos on 4/10/17.
 */

public class AdapterVirus extends RecyclerView.Adapter<AdapterVirus.ViewHolder> {
    private Context mContext;
    private int textColorSecondary;

    public AdapterVirus(Context context) {
        mContext = context;

        TypedArray typedArray = context.obtainStyledAttributes(R.style
                        .TextAppearance_AppCompat_Notification_Time,
                new int[]{android.R.attr.textColor});
        textColorSecondary = typedArray.getInt(0, Color.BLACK);
        typedArray.recycle();
        // TODO what is styled attr
    }

    @Override public int getItemViewType(final int position) {
        return position == 0 ? 0 : 1;
    }

    @Override public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int type) {
        if (type == 0) {
            return new StatsViewHolder(LayoutInflater.from(mContext).inflate(R.layout.head_virus,
                    viewGroup, false));
        } else {
            return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_virus,
                    viewGroup,
                    false));
        }
    }


    @Override public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (position > 3 && position <= 5) {
            viewHolder.level(Suspicious);
        } else if (position > 5) {
            viewHolder.level(Safe);
        } else { viewHolder.level(Danger); }
    }

    @Override public int getItemCount() {
        return 10;
    }

    enum Level {Danger, Suspicious, Safe}

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTime;

        TextView textTitle;
        TextView textInfo;
        TextView textInfo2;
        LinearLayout linearLayout;


        ViewHolder level(Level level) {
            if (this instanceof StatsViewHolder) {
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
        public StatsViewHolder(final View itemView) {
            super(itemView);
        }
    }
}
