package com.android.launcher3.views;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_WIDGETS_PREDICTION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.widget.WidgetCell;
import com.android.launcher3.widget.OplusWidgetCell;
import com.android.launcher3.widget.model.WidgetsListBaseEntry;
import com.android.launcher3.widget.model.WidgetsListContentEntry;
import com.coui.appcompat.cardlist.COUICardListHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Local counterpart of Assistant Screen's CardStoreMainPageAdapter.
 *
 * <p>The decoded adapter uses view type 4 for a 48dp category title, the default type for
 * operating cards (rendered at 0.92 scale), and type 100 for the launcher-widget entry. The
 * proprietary operating-card provider is unavailable on MTK, so those slots bind Launcher's
 * installed {@link WidgetItem} data without changing the decoded list hierarchy.</p>
 */
final class ColorOsWidgetHubAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CARD = 0;
    private static final int TYPE_CATEGORY_TITLE = 4;
    private static final int TYPE_ALL_WIDGETS = 100;
    private static final float OPPO_CARD_SCALE = 0.92f;
    private static final int MAX_RECOMMENDATIONS = 4;

    private static final Object CATEGORY_TITLE = new Object();
    private static final Object ALL_WIDGETS = new Object();

    private final Launcher mLauncher;
    private final Runnable mOpenAllWidgets;
    private final View.OnClickListener mWidgetClickListener;
    private final View.OnLongClickListener mWidgetLongClickListener;
    private final List<Object> mItems = new ArrayList<>();

    ColorOsWidgetHubAdapter(Launcher launcher, Runnable openAllWidgets,
            View.OnClickListener widgetClickListener,
            View.OnLongClickListener widgetLongClickListener) {
        mLauncher = launcher;
        mOpenAllWidgets = openAllWidgets;
        mWidgetClickListener = widgetClickListener;
        mWidgetLongClickListener = widgetLongClickListener;
        rebuildItems();
    }

    private void rebuildItems() {
        mItems.clear();
        List<WidgetItem> widgets = new ArrayList<>(
                mLauncher.getPopupDataProvider().getRecommendedWidgets());
        if (widgets.isEmpty()) {
            // The AOSP recommendation service is optional. Preserve model ordering and select
            // one real widget per package, matching the card store's grouped feed semantics.
            for (WidgetsListBaseEntry entry
                    : mLauncher.getPopupDataProvider().getAllWidgets()) {
                if (!(entry instanceof WidgetsListContentEntry)) {
                    continue;
                }
                List<WidgetItem> packageWidgets = ((WidgetsListContentEntry) entry).mWidgets;
                for (WidgetItem item : packageWidgets) {
                    // Assistant Screen operating cards are widgets, not shortcut-config
                    // activities. Keep the Launcher's provider order but skip shortcut rows.
                    if (item.widgetInfo != null) {
                        widgets.add(item);
                        break;
                    }
                }
                if (widgets.size() == MAX_RECOMMENDATIONS) {
                    break;
                }
            }
        }

        if (!widgets.isEmpty()) {
            mItems.add(CATEGORY_TITLE);
            mItems.addAll(widgets.subList(0, Math.min(MAX_RECOMMENDATIONS, widgets.size())));
        }
        mItems.add(ALL_WIDGETS);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mItems.get(position);
        if (item == CATEGORY_TITLE) {
            return TYPE_CATEGORY_TITLE;
        }
        if (item == ALL_WIDGETS) {
            return TYPE_ALL_WIDGETS;
        }
        return TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CATEGORY_TITLE) {
            return new CategoryHolder(inflater.inflate(
                    R.layout.coloros_widget_hub_category_title, parent, false));
        }
        if (viewType == TYPE_ALL_WIDGETS) {
            View view = inflater.inflate(
                    R.layout.coloros_widget_hub_all_widgets_item, parent, false);
            COUICardListHelper.setItemCardBackground(view, COUICardListHelper.FULL);
            view.setOnClickListener(ignored -> mOpenAllWidgets.run());
            return new EntryHolder(view);
        }
        return new CardHolder(inflater.inflate(
                R.layout.coloros_widget_hub_card_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CategoryHolder) {
            ((CategoryHolder) holder).title.setText(R.string.coloros_smart_suggestions);
            return;
        }
        if (holder instanceof CardHolder) {
            WidgetItem item = (WidgetItem) mItems.get(position);
            WidgetCell cell = ((CardHolder) holder).cell;
            cell.clear();
            cell.setSourceContainer(CONTAINER_WIDGETS_PREDICTION);
            cell.applyFromCellItem(item, OPPO_CARD_SCALE);
            // OplusWidgetsHzAdapter installs interaction handlers on OplusWidgetCell itself.
            // This preserves its decoded preview-only 0.9 press animation and native long press.
            cell.setOnClickListener(mWidgetClickListener);
            cell.setOnLongClickListener(mWidgetLongClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof CardHolder) {
            ((CardHolder) holder).cell.clear();
        }
        super.onViewRecycled(holder);
    }

    private static final class CategoryHolder extends RecyclerView.ViewHolder {
        final TextView title;

        CategoryHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.coloros_widget_hub_group_title);
        }
    }

    private static final class CardHolder extends RecyclerView.ViewHolder {
        final WidgetCell cell;

        CardHolder(View itemView) {
            super(itemView);
            cell = itemView.findViewById(R.id.coloros_widget_hub_card_cell);
            // The first-level operating-card slot is aspect-ratio driven. OplusWidgetCell's
            // square size is only for the launcher's compact toggle-bar row.
            if (cell instanceof OplusWidgetCell) {
                cell.setColorOsPreviewSize(0);
            }
        }
    }

    private static final class EntryHolder extends RecyclerView.ViewHolder {
        EntryHolder(View itemView) {
            super(itemView);
        }
    }
}
