/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.widget.picker;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.launcher3.R;
import com.android.launcher3.OplusBubbleTextView;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher.widget.OplusWidgetsHzRecyclerView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** A {@link ViewHolder} for showing widgets of an app in the full widget picker. */
public final class WidgetsRowViewHolder extends ViewHolder {

    public final WidgetsListTableView tableContainer;
    public final LinearLayout colorOsContainer;
    public final OplusWidgetsHzRecyclerView colorOsRecyclerView;
    public final OplusBubbleTextView colorOsTitle;
    public final Map<WidgetItem, Bitmap> previewCache = new HashMap<>();
    Consumer<Pair<WidgetItem, Bitmap>> mDataCallback;

    public WidgetsRowViewHolder(View v) {
        super(v);

        tableContainer = v.findViewById(R.id.widgets_table);
        colorOsContainer = v.findViewById(R.id.coloros_widget_horizontal_container);
        colorOsRecyclerView = v.findViewById(R.id.widgets_scroll_container);
        colorOsTitle = v.findViewById(R.id.section);
        if (colorOsTitle != null) {
            colorOsTitle.setAccessibilityDelegate(null);
        }
    }

    /**
     * When the preview is loaded we callback to notify that the preview loaded and we rebind the
     * view.
     *
     * @param data is the payload which is needed when binding the view.
     */
    public void onPreviewLoaded(Pair<WidgetItem, Bitmap> data) {
        if (mDataCallback != null) {
            mDataCallback.accept(data);
        }
        if (getBindingAdapter() != null) {
            getBindingAdapter().notifyItemChanged(getBindingAdapterPosition(), data);
        }
    }
}
