package com.sessiontracks.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sessiontracks.app.R;
import com.sessiontracks.app.util.NumberFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only preview of the lecture→link mapping in the bulk importer. */
public class LinkPreviewAdapter extends RecyclerView.Adapter<LinkPreviewAdapter.PreviewViewHolder> {

    /** One preview row. */
    public static final class Row {
        public final int lectureNumber;
        @NonNull
        public final String url;

        public Row(int lectureNumber, @NonNull String url) {
            this.lectureNumber = lectureNumber;
            this.url = url;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final NumberFormatter numbers;

    public LinkPreviewAdapter(boolean useBengaliNumerals) {
        this.numbers = new NumberFormatter(useBengaliNumerals);
    }

    public void submit(@NonNull Map<Integer, String> mapping) {
        rows.clear();
        List<Integer> keys = new ArrayList<>(mapping.keySet());
        java.util.Collections.sort(keys);
        for (Integer key : keys) {
            String url = mapping.get(key);
            if (key != null && url != null) {
                rows.add(new Row(key, url));
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        int size = rows.size();
        rows.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_link_preview, parent, false);
        return new PreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    class PreviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView numberText;
        private final TextView urlText;

        PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.previewNumberText);
            urlText = itemView.findViewById(R.id.previewUrlText);
        }

        void bind(@NonNull Row row) {
            String number = numbers.format(row.lectureNumber);
            numberText.setText(itemView.getContext().getString(R.string.lecture_short, number));
            urlText.setText(row.url);
            itemView.setContentDescription(itemView.getContext()
                    .getString(R.string.bulk_row_format, number, row.url));
        }
    }
}
