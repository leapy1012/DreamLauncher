/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.quickstep.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.R;
import com.coui.appcompat.poplist.PopupListItem;

import java.util.List;

/**
 * One row per shortcut (Oppo {@code OplusTaskMenuAdapter}), not COUI
 * {@link com.coui.appcompat.poplist.DefaultAdapter}'s item/divider interleaved positions.
 * Rows stay non-clickable so {@link android.widget.ListView}'s item click listener fires.
 */
class TaskMenuAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<PopupListItem> mItems;
    private final LayoutInflater mInflater;

    private static class ViewHolder {
        ImageView icon;
        TextView title;
        View divider;
    }

    TaskMenuAdapter(@NonNull Context context, @NonNull List<PopupListItem> items) {
        mContext = context;
        mItems = items;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public PopupListItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        PopupListItem item = mItems.get(position);
        return item == null || item.isEnable();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = mInflater.inflate(R.layout.task_menu_option_item, parent, false);
            // Must stay non-clickable so ListView OnItemClickListener receives the tap.
            convertView.setClickable(false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(com.coui.appcompat.R.id.popup_list_window_item_icon);
            holder.title = convertView.findViewById(com.coui.appcompat.R.id.popup_list_window_item_title);
            holder.divider = convertView.findViewById(R.id.task_menu_item_divider);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PopupListItem item = mItems.get(position);
        boolean enabled = item.isEnable();
        convertView.setEnabled(enabled);

        Drawable icon = item.getIcon();
        if (icon == null && item.getIconId() != 0) {
            icon = mContext.getDrawable(item.getIconId());
        }
        if (icon != null) {
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(icon);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        holder.title.setText(item.getTitle());
        holder.title.setEnabled(enabled);
        ColorStateList titleColor = item.getTitleColorList();
        if (titleColor != null) {
            holder.title.setTextColor(titleColor);
        }

        holder.divider.setVisibility(position == getCount() - 1 ? View.INVISIBLE : View.VISIBLE);
        return convertView;
    }
}
