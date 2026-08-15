package com.sessiontracks.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sessiontracks.app.R;
import com.sessiontracks.app.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/** Horizontal colour picker used by the session and subject editors. */
public class ColorSwatchAdapter extends RecyclerView.Adapter<ColorSwatchAdapter.SwatchViewHolder> {

    public interface OnColorSelected {
        void onColorSelected(@ColorInt int color);
    }

    private final List<Integer> colors = new ArrayList<>();
    private final OnColorSelected listener;
    private int selectedColor;

    public ColorSwatchAdapter(@NonNull List<Integer> palette, @ColorInt int selectedColor,
                              @NonNull OnColorSelected listener) {
        this.colors.addAll(palette);
        this.selectedColor = selectedColor;
        this.listener = listener;
    }

    @ColorInt
    public int getSelectedColor() {
        return selectedColor;
    }

    @NonNull
    @Override
    public SwatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_swatch, parent, false);
        return new SwatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SwatchViewHolder holder, int position) {
        holder.bind(colors.get(position));
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    class SwatchViewHolder extends RecyclerView.ViewHolder {

        private final View swatch;
        private final ImageView selectedIcon;

        SwatchViewHolder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.colorSwatch);
            selectedIcon = itemView.findViewById(R.id.colorSelectedIcon);
        }

        void bind(@ColorInt int color) {
            ColorUtils.tintShape(swatch, color);
            boolean selected = color == selectedColor;
            selectedIcon.setVisibility(selected ? View.VISIBLE : View.GONE);
            selectedIcon.setColorFilter(ColorUtils.contrastingTextColor(color));
            swatch.setSelected(selected);
            swatch.setContentDescription(itemView.getContext().getString(R.string.session_color));

            swatch.setOnClickListener(v -> {
                int previous = indexOf(selectedColor);
                selectedColor = color;
                listener.onColorSelected(color);
                if (previous >= 0) {
                    notifyItemChanged(previous);
                }
                notifyItemChanged(getBindingAdapterPosition());
            });
        }

        private int indexOf(@ColorInt int color) {
            for (int i = 0; i < colors.size(); i++) {
                if (colors.get(i) == color) {
                    return i;
                }
            }
            return -1;
        }
    }
}
