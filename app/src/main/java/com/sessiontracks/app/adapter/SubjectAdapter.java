package com.sessiontracks.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.model.SubjectWithProgress;
import com.sessiontracks.app.util.ColorUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.TimeUtils;

/** Subject cards nested inside a session card. */
public class SubjectAdapter extends ListAdapter<SubjectWithProgress, SubjectAdapter.SubjectViewHolder> {

    public interface SubjectListener {
        void onOpen(@NonNull SubjectWithProgress subject);

        void onEdit(@NonNull SubjectWithProgress subject);

        void onDelete(@NonNull SubjectWithProgress subject);
    }

    private final SubjectListener listener;
    private final NumberFormatter numbers;

    public SubjectAdapter(@NonNull SubjectListener listener, boolean useBengaliNumerals) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.numbers = new NumberFormatter(useBengaliNumerals);
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<SubjectWithProgress> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SubjectWithProgress>() {
                @Override
                public boolean areItemsTheSame(@NonNull SubjectWithProgress oldItem,
                                               @NonNull SubjectWithProgress newItem) {
                    return oldItem.getSubject().getId().equals(newItem.getSubject().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull SubjectWithProgress oldItem,
                                                  @NonNull SubjectWithProgress newItem) {
                    return oldItem.getSubject().equals(newItem.getSubject())
                            && oldItem.getPercent() == newItem.getPercent()
                            && oldItem.getChapterCount() == newItem.getChapterCount()
                            && oldItem.getTotalLectures() == newItem.getTotalLectures()
                            && oldItem.getCompletedLectures() == newItem.getCompletedLectures();
                }
            };

    @Override
    public long getItemId(int position) {
        return getItem(position).getSubject().getId().hashCode();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class SubjectViewHolder extends RecyclerView.ViewHolder {

        private final View card;
        private final View colorDot;
        private final TextView nameText;
        private final TextView timeText;
        private final TextView percentText;
        private final LinearProgressIndicator progressBar;
        private final ImageButton menuButton;

        SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.subjectCard);
            colorDot = itemView.findViewById(R.id.subjectColorDot);
            nameText = itemView.findViewById(R.id.subjectNameText);
            timeText = itemView.findViewById(R.id.subjectTimeText);
            percentText = itemView.findViewById(R.id.subjectPercentText);
            progressBar = itemView.findViewById(R.id.subjectProgressBar);
            menuButton = itemView.findViewById(R.id.subjectMenuButton);
        }

        void bind(@NonNull SubjectWithProgress item) {
            Context context = itemView.getContext();
            Subject subject = item.getSubject();

            nameText.setText(subject.getName());
            ColorUtils.tintShape(colorDot, subject.getColor());

            String chapters = context.getString(R.string.subject_chapter_count,
                    numbers.format(item.getChapterCount()));
            if (subject.hasCustomTime()) {
                String range = TimeUtils.formatRange(context, subject.getStartMinute(), subject.getEndMinute());
                timeText.setText(range + " • " + chapters);
            } else {
                timeText.setText(chapters);
            }

            int percent = item.getPercent();
            progressBar.setProgress(percent);
            percentText.setText(context.getString(R.string.chapter_progress_format, numbers.format(percent)));
            progressBar.setContentDescription(context.getString(R.string.cd_progress, numbers.format(percent)));

            card.setContentDescription(subject.getName() + ", "
                    + context.getString(R.string.cd_progress, numbers.format(percent)));
            card.setOnClickListener(v -> listener.onOpen(item));
            menuButton.setOnClickListener(v -> showMenu(v, item));
        }

        private void showMenu(@NonNull View anchor, @NonNull SubjectWithProgress item) {
            androidx.appcompat.widget.PopupMenu menu =
                    new androidx.appcompat.widget.PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_subject_item);
            menu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.action_edit_subject) {
                    listener.onEdit(item);
                    return true;
                } else if (id == R.id.action_delete_subject) {
                    listener.onDelete(item);
                    return true;
                }
                return false;
            });
            menu.show();
        }
    }
}
