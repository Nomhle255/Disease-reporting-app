package com.example.diseasealertlesotho;

import android.content.Context;
import android.database.Cursor;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_VET    = 0;
    private static final int VIEW_TYPE_FARMER = 1;

    private final Context context;
    private final List<ConversationMessage> messages;

    // ─────────────────────────────────────────────
    //  Data model for a single message in the thread
    // ─────────────────────────────────────────────
    public static class ConversationMessage {
        public String sender;        // "vet" or "farmer"
        public String responseType;  // e.g. "Request more information", "Farmer reply"
        public String message;
        public String date;
    }

    // ─────────────────────────────────────────────
    //  Constructor — reads from cursor into a list
    // ─────────────────────────────────────────────
    public ConversationAdapter(Context context, Cursor cursor) {
        this.context  = context;
        this.messages = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                ConversationMessage msg = new ConversationMessage();
                msg.sender       = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                msg.responseType = cursor.getString(cursor.getColumnIndexOrThrow("response_type"));
                msg.message      = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                msg.date         = cursor.getString(cursor.getColumnIndexOrThrow("msg_date"));
                messages.add(msg);
            }
            cursor.close();
        }
    }

    // ─────────────────────────────────────────────
    //  ViewHolder
    // ─────────────────────────────────────────────
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBubbleWrap;
        LinearLayout layoutBubble;
        TextView tvTag;
        TextView tvMessage;
        TextView tvMeta;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBubbleWrap = itemView.findViewById(R.id.layout_bubble_wrap);
            layoutBubble     = itemView.findViewById(R.id.layout_bubble);
            tvTag            = itemView.findViewById(R.id.tv_bubble_tag);
            tvMessage        = itemView.findViewById(R.id.tv_bubble_message);
            tvMeta           = itemView.findViewById(R.id.tv_bubble_meta);
        }
    }

    // ─────────────────────────────────────────────
    //  Inflate correct layout per sender
    // ─────────────────────────────────────────────
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).sender.equals("vet")
                ? VIEW_TYPE_VET
                : VIEW_TYPE_FARMER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_VET)
                ? R.layout.item_bubble_vet
                : R.layout.item_bubble_farmer;

        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    // ─────────────────────────────────────────────
    //  Bind data to each bubble
    // ─────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ConversationMessage msg = messages.get(position);

        holder.tvTag.setText(msg.responseType);
        holder.tvMessage.setText(msg.message);
        holder.tvMeta.setText(formatDate(msg.date));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ─────────────────────────────────────────────
    //  Format date string for display
    // ─────────────────────────────────────────────
    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // raw is stored as "2026-04-10 09:14:00"
        // display as "Apr 10, 09:14"
        try {
            String[] parts = raw.split(" ");
            String datePart = parts[0]; // 2026-04-10
            String timePart = parts.length > 1 ? parts[1].substring(0, 5) : ""; // 09:14

            String[] dateParts = datePart.split("-");
            int month = Integer.parseInt(dateParts[1]);

            String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec"};
            String monthName = (month >= 1 && month <= 12)
                    ? monthNames[month - 1] : "";

            return monthName + " " + dateParts[2] + ", " + timePart;
        } catch (Exception e) {
            return raw;
        }
    }
}