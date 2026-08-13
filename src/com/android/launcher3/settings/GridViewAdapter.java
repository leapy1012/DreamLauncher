
package com.android.launcher3.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.launcher3.R;

import java.util.ArrayList;
import com.android.launcher3.settings.ThemeActivity.ThemeData;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsListView;
import android.widget.GridView;

public class GridViewAdapter extends BaseAdapter {
    private Context mContext;

    ArrayList<ThemeData> mItemList;

    private LayoutInflater mInflater;

    public GridViewAdapter(Context context, ArrayList<ThemeData> items) {
        mContext = context;
        mItemList = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        if (mItemList != null) {
            return mItemList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        ThemeData item = mItemList.get(position);
        if (convertView == null) {
            view = mInflater.inflate(R.layout.theme_item_list, parent, false);
        } else {
            view = convertView;
        }
        if (parent instanceof GridView) {
            int columnWidth = ((GridView) parent).getColumnWidth();
            if (columnWidth > 0) {
                view.setLayoutParams(new AbsListView.LayoutParams(
                        columnWidth, AbsListView.LayoutParams.WRAP_CONTENT));
            }
        }
        bindView(item, view);
        return view;
    }

    private void bindView(ThemeData item, View view) {
         ImageView imageView =
         (ImageView)view.findViewById(R.id.theme_preview);
         TextView textView = (TextView) view.findViewById(R.id.theme_title);
         view.setSelected(item.selected);
         imageView.setImageDrawable(item.preview);
         textView.setText(item.themeTitle);
    }
}
