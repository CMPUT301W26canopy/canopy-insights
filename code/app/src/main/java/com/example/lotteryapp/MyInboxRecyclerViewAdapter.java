package com.example.lotteryapp;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link NotificationModel}.
 */
public class MyInboxRecyclerViewAdapter extends RecyclerView.Adapter<MyInboxRecyclerViewAdapter.ViewHolder> {

    private final List<NotificationModel> mValues;

    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_inbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        NotificationModel item = mValues.get(position);
        holder.mItem = item;
        holder.mIdView.setText(item.getSenderAccountID());
        holder.mContentView.setText(item.getMessage());

        if (item.getEventId() != null && !item.getEventId().isEmpty()) {
            holder.mViewEventBtn.setVisibility(View.VISIBLE);
            holder.mViewEventBtn.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventActivity.class);
                intent.putExtra("EVENT_ID", item.getEventId());
                context.startActivity(intent);
            });
        } else {
            holder.mViewEventBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public final Button mViewEventBtn;
        public NotificationModel mItem;

        public ViewHolder(View view) {
            super(view);
            mIdView = view.findViewById(R.id.item_number);
            mContentView = view.findViewById(R.id.content);
            mViewEventBtn = view.findViewById(R.id.btn_view_event);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
