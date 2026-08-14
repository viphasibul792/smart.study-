package com.sessiontracks.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.model.ChapterWithProgress;
import com.sessiontracks.app.util.NumberFormatter;

/** Chapter rows on the subject detail screen. */
public class ChapterAdapter extends ListAdapter<ChapterWithProgress, ChapterAdapter.ChapterViewHolder> {

    public interface ChapterListener {
        void onOpen(@NonNull Chapter chapter);

        void onEdit(@NonNull Chapter chapter);

        void onNotes(@NonNull Chapter chapter);

        void onDelete(@NonNull Chapter chapter);
    }

    private final ChapterListener listener;
    private final NumberFormatter numbers;

    public ChapterAdapter(@NonNull ChapterListener listener, boolean useBengaliNumerals) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.numbers = new NumberFormatter(useBengaliNumerals);
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<ChapterWithProgress> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChapterWithProgress>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChapterWithProgress oldItem,
                                               @NonNull ChapterWithProgress newItem) {
                    return oldItem.getChapter().getId().equals(newItem.getChapter().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChapterWithProgress oldItem,
                                                  @NonNull ChapterWithProgress newItem) {
                    return oldItem.getChapter().equals(newItem.getChapter())
                            && oldItem.getPercent() == newItem.getPercent()
                            && oldItem.getTotalLectures() == newItem.getTotalLectures()
                            && oldItem.getCompletedLectures() == newItem.getCompletedLectures();
                }
            };

    @Override
    public long getItemId(int position) {
        return getItem(position).getChapter().getId().hashCode();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder {

        private final View card;
        private final TextView numberText;
        private final TextView nameText;
        private final TextView percentText;
        private final TextView detailText;
        private final LinearProgressIndicator progressBar;
        private final ImageView noteIndicator;
        private final ImageButton menuButton;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.chapterCard);
            numberText = itemView.findViewById(R.id.chapterNumberText);
            nameText = itemView.findViewById(R.id.chapterNameText);
            percentText = itemView.findViewById(R.id.chapterPercentText);
            detailText = itemView.findViewById(R.id.chapterDetailText);
            progressBar = itemView.findViewById(R.id.chapterProgressBar);
            noteIndicator = itemView.findViewById(R.id.chapterNoteIndicator);
            menuButton = itemView.findViewById(R.id.chapterMenuButton);
        }

        void bind(@NonNull ChapterWithProgress item) {
            Context context = itemView.getContext();
            Chapter chapter = item.getChapter();

            boolean hasNumber = !chapter.getNumber().trim().isEmpty();
            numberText.setVisibility(hasNumber ? View.VISIBLE : View.GONE);
            if (hasNumber) {
                numberText.setText(context.getString(R.string.chapter_label,
                        numbers.formatText(chapter.getNumber())));
            }

            nameText.setText(chapter.getName().trim().isEmpty()
                    ? context.getString(R.string.chapter_new) : chapter.getName());

            int percent = item.getPercent();
            progressBar.setProgress(percent);
            percentText.setText(context.getString(R.string.chapter_progress_format, numbers.format(percent)));
            detailText.setText(context.getString(R.string.chapter_progress_detail,
                    numbers.format(item.getCompletedLectures()), numbers.format(item.getTotalLectures())));
            progressBar.setContentDescription(context.getString(R.string.cd_progress, numbers.format(percent)));

            noteIndicator.setVisibility(chapter.hasNotes() ? View.VISIBLE : View.GONE);

            card.setOnClickListener(v -> listener.onOpen(chapter));
            menuButton.setOnClickListener(v -> showMenu(v, chapter));
        }

        private void showMenu(@NonNull View anchor, @NonNull Chapter chapter) {
            androidx.appcompat.widget.PopupMenu menu =
                    new androidx.appcompat.widget.PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_chapter_item);
            menu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.action_edit_chapter) {
                    listener.onEdit(chapter);
                    return true;
                } else if (id == R.id.action_chapter_notes) {
                    listener.onNotes(chapter);
                    return true;
                } else if (id == R.id.action_delete_chapter) {
                    listener.onDelete(chapter);
                    return true;
                }
                return false;
            });
            menu.show();
        }
    }
}
