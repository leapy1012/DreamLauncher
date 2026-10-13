package com.coui.appcompat.bottomnavigation;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.PopupWindow;

/* JADX INFO: loaded from: classes4.dex */
public class COUINavigationPopupMenu implements PopupWindow.OnDismissListener, AdapterView.OnItemClickListener, View.OnKeyListener, ViewTreeObserver.OnGlobalLayoutListener, View.OnLayoutChangeListener {

    public class MenuAdapter extends BaseAdapter {
        @Override // android.widget.Adapter
        public final int getCount() {
            throw null;
        }

        @Override // android.widget.Adapter
        public final Object getItem(int i3) {
            throw null;
        }

        @Override // android.widget.Adapter
        public final long getItemId(int i3) {
            return i3;
        }

        @Override // android.widget.Adapter
        public final View getView(int i3, View view, ViewGroup viewGroup) {
            if (view == null || i3 != 0) {
                throw null;
            }
            view.setBackgroundResource(com.coui.appcompat.R.drawable.coui_popup_list_top_selector);
            throw null;
        }
    }

    @Override // android.widget.PopupWindow.OnDismissListener
    public final void onDismiss() {
    }

    @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
    public final void onGlobalLayout() {
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public final void onItemClick(AdapterView<?> adapterView, View view, int i3, long j3) {
        throw null;
    }

    @Override // android.view.View.OnKeyListener
    public final boolean onKey(View view, int i3, KeyEvent keyEvent) {
        if (keyEvent.getAction() == 1 && i3 == 82) {
            throw null;
        }
        return false;
    }

    @Override // android.view.View.OnLayoutChangeListener
    public final void onLayoutChange(View view, int i3, int i10, int i11, int i12, int i13, int i14, int i15, int i16) {
    }
}
