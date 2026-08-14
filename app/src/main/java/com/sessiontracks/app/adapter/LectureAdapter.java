package com.sessiontracks.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.util.NumberFormatter;

/**
 * Grid of lecture tiles. Tapping a tile plays the video, the checkbox toggles
 * completion and a long press opens the per-lecture action sheet.
 */
public class LectureAdapter extends ListAdapter<Lecture, LectureAdapter.LectureViewHolder> {

    /** Callbacks handled by the chapter screen. */
    public interface LectureListener {
        void onPlay(@NonNull Lecture lecture);

        void onToggleCompleted(@NonNull Lecture lecture, boolean completed);

        void onLongPress(@NonNull Lecture lecture);
    }

    private final LectureListener listener;
    private final NumberFormatter numbers;

    public LectureAdapter(@NonNull LectureListener listener, boolean useBengaliNumerals) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.numbers = new NumberFormatter(useBengaliNumerals);
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<Lecture> DIFF_CALLBACK = new DiffUtil.ItemCallback<Lecture>() {
        @Override
        public boolean areItemsTheSame(@NonNull Lecture oldItem, @NonNull Lecture newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Lecture oldItem, @NonNull Lecture newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    @NonNull
    @Override
    public LectureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lecture, parent, false);
        return new LectureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LectureViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class LectureViewHolder extends RecyclerView.ViewHolder {

        private final View root;
        private final android.widget.TextView numberText;
        private final MaterialCheckBox checkBox;
        private final View linkIcon;
        private final View noteIcon;

        LectureViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.lectureRoot);
            numberText = itemView.findViewById(R.id.lectureNumberText);
            checkBox = itemView.findViewById(R.id.lectureCheckBox);
            linkIcon = itemView.findViewById(R.id.lectureLinkIcon);
            noteIcon = itemView.findViewById(R.id.lectureNoteIcon);
        }

        void bind(@NonNull Lecture lecture) {
            android.content.Context context = itemView.getContext();
            String number = numbers.format(lecture.getNumber());
            numberText.setText(context.getString(R.string.lecture_short, number));

            root.setActivated(lecture.isCompleted());

            linkIcon.setVisibility(lecture.hasLink() ? View.VISIBLE : View.GONE);
            noteIcon.setVisibility(lecture.hasNotes() ? View.VISIBLE : View.GONE);

            // Avoid the listener firing while we sync the checkbox to the data.
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(lecture.isCompleted());
            checkBox.setContentDescription(
                    context.getString(R.string.lecture_checkbox_desc, number));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != lecture.isCompleted()) {
                    listener.onToggleCompleted(lecture, isChecked);
                }
            });

            String status = context.getString(lecture.isCompleted()
                    ? R.string.lecture_status_done : R.string.lecture_status_pending);
            root.setContentDescription(
                    context.getString(R.string.lecture_label, number) + ", " + status);

            root.setOnClickListener(v -> listener.onPlay(lecture));
            root.setOnLongClickListener(v -> {
                listener.onLongPress(lecture);
                return true;
            });
        }
    }
}
