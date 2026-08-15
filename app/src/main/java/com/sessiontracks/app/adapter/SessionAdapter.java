package com.sessiontracks.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.model.SessionWithSubjects;
import com.sessiontracks.app.data.model.SubjectWithProgress;
import com.sessiontracks.app.util.ColorUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.TimeUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Dashboard list of session cards. Each card hosts a nested RecyclerView of
 * subjects and remembers its expanded state across rebinds.
 */
public class SessionAdapter extends ListAdapter<SessionWithSubjects, SessionAdapter.SessionViewHolder> {

    public interface SessionListener {
        void onEditSession(@NonNull StudySession session);

        void onDeleteSession(@NonNull StudySession session);

        void onAddSubject(@NonNull StudySession session);

        void onOpenSubject(@NonNull SubjectWithProgress subject);

        void onEditSubject(@NonNull SubjectWithProgress subject);

        void onDeleteSubject(@NonNull SubjectWithProgress subject);
    }

    private final SessionListener listener;
    private final NumberFormatter numbers;
    /** Session ids the user has collapsed; everything else stays expanded. */
    private final Set<String> collapsed = new HashSet<>();
    private final RecyclerView.RecycledViewPool subjectPool = new RecyclerView.RecycledViewPool();

    public SessionAdapter(@NonNull SessionListener listener, boolean useBengaliNumerals) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.numbers = new NumberFormatter(useBengaliNumerals);
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<SessionWithSubjects> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SessionWithSubjects>() {
                @Override
                public boolean areItemsTheSame(@NonNull SessionWithSubjects oldItem,
                                               @NonNull SessionWithSubjects newItem) {
                    return oldItem.getSession().getId().equals(newItem.getSession().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull SessionWithSubjects oldItem,
                                                  @NonNull SessionWithSubjects newItem) {
                    if (!oldItem.getSession().equals(newItem.getSession())) {
                        return false;
                    }
                    if (oldItem.getSubjectCount() != newItem.getSubjectCount()) {
                        return false;
                    }
                    if (oldItem.getCompletedLectures() != newItem.getCompletedLectures()
                            || oldItem.getTotalLectures() != newItem.getTotalLectures()) {
                        return false;
                    }
                    for (int i = 0; i < oldItem.getSubjectCount(); i++) {
                        SubjectWithProgress a = oldItem.getSubjects().get(i);
                        SubjectWithProgress b = newItem.getSubjects().get(i);
                        if (!a.getSubject().equals(b.getSubject())
                                || a.getPercent() != b.getPercent()
                                || a.getChapterCount() != b.getChapterCount()) {
                            return false;
                        }
                    }
                    return true;
                }
            };

    @Override
    public long getItemId(int position) {
        return getItem(position).getSession().getId().hashCode();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {

        private final View header;
        private final View colorBar;
        private final TextView nameText;
        private final TextView timeText;
        private final TextView subjectCountText;
        private final TextView liveBadge;
        private final ImageButton expandButton;
        private final ImageButton menuButton;
        private final LinearProgressIndicator progressBar;
        private final View body;
        private final TextView emptySubjectsText;
        private final RecyclerView subjectsRecyclerView;
        private final View addSubjectButton;
        private final SubjectAdapter subjectAdapter;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.sessionHeader);
            colorBar = itemView.findViewById(R.id.sessionColorBar);
            nameText = itemView.findViewById(R.id.sessionNameText);
            timeText = itemView.findViewById(R.id.sessionTimeText);
            subjectCountText = itemView.findViewById(R.id.sessionSubjectCountText);
            liveBadge = itemView.findViewById(R.id.liveBadge);
            expandButton = itemView.findViewById(R.id.expandButton);
            menuButton = itemView.findViewById(R.id.sessionMenuButton);
            progressBar = itemView.findViewById(R.id.sessionProgressBar);
            body = itemView.findViewById(R.id.sessionBody);
            emptySubjectsText = itemView.findViewById(R.id.emptySubjectsText);
            subjectsRecyclerView = itemView.findViewById(R.id.subjectsRecyclerView);
            addSubjectButton = itemView.findViewById(R.id.addSubjectButton);

            subjectAdapter = new SubjectAdapter(new SubjectAdapter.SubjectListener() {
                @Override
                public void onOpen(@NonNull SubjectWithProgress subject) {
                    listener.onOpenSubject(subject);
                }

                @Override
                public void onEdit(@NonNull SubjectWithProgress subject) {
                    listener.onEditSubject(subject);
                }

                @Override
                public void onDelete(@NonNull SubjectWithProgress subject) {
                    listener.onDeleteSubject(subject);
                }
            }, numbers.usesBengaliDigits());

            subjectsRecyclerView.setLayoutManager(
                    new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false));
            subjectsRecyclerView.setAdapter(subjectAdapter);
            subjectsRecyclerView.setRecycledViewPool(subjectPool);
            subjectsRecyclerView.setHasFixedSize(false);
            subjectsRecyclerView.setNestedScrollingEnabled(false);
        }

        void bind(@NonNull SessionWithSubjects item) {
            Context context = itemView.getContext();
            StudySession session = item.getSession();

            nameText.setText(session.getName());
            timeText.setText(TimeUtils.formatRange(context, session.getStartMinute(), session.getEndMinute()));
            ColorUtils.tintShape(colorBar, session.getColor());

            int percent = item.getPercent();
            progressBar.setProgress(percent);
            progressBar.setContentDescription(context.getString(R.string.cd_progress, numbers.format(percent)));

            String subjectCount = context.getString(R.string.session_subject_count,
                    numbers.format(item.getSubjectCount()));
            subjectCountText.setText(item.getTotalLectures() > 0
                    ? subjectCount + " • " + context.getString(R.string.chapter_progress_format, numbers.format(percent))
                    : subjectCount);

            boolean live = TimeUtils.isNowWithin(session.getStartMinute(), session.getEndMinute());
            liveBadge.setVisibility(live ? View.VISIBLE : View.GONE);

            subjectAdapter.submitList(item.getSubjects());
            boolean hasSubjects = item.getSubjectCount() > 0;
            emptySubjectsText.setVisibility(hasSubjects ? View.GONE : View.VISIBLE);
            subjectsRecyclerView.setVisibility(hasSubjects ? View.VISIBLE : View.GONE);

            boolean expanded = !collapsed.contains(session.getId());
            applyExpanded(expanded);

            View.OnClickListener toggle = v -> {
                boolean nowExpanded = collapsed.contains(session.getId());
                if (nowExpanded) {
                    collapsed.remove(session.getId());
                } else {
                    collapsed.add(session.getId());
                }
                applyExpanded(nowExpanded);
            };
            header.setOnClickListener(toggle);
            expandButton.setOnClickListener(toggle);

            addSubjectButton.setOnClickListener(v -> listener.onAddSubject(session));
            menuButton.setOnClickListener(v -> showMenu(v, session));
        }

        private void applyExpanded(boolean expanded) {
            body.setVisibility(expanded ? View.VISIBLE : View.GONE);
            expandButton.setRotation(expanded ? 180f : 0f);
            expandButton.setContentDescription(itemView.getContext()
                    .getString(expanded ? R.string.cd_collapse : R.string.cd_expand));
        }

        private void showMenu(@NonNull View anchor, @NonNull StudySession session) {
            androidx.appcompat.widget.PopupMenu menu =
                    new androidx.appcompat.widget.PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_session_item);
            menu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.action_edit_session) {
                    listener.onEditSession(session);
                    return true;
                } else if (id == R.id.action_add_subject) {
                    listener.onAddSubject(session);
                    return true;
                } else if (id == R.id.action_delete_session) {
                    listener.onDeleteSession(session);
                    return true;
                }
                return false;
            });
            menu.show();
        }
    }
}
