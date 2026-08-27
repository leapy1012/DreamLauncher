package com.coui.appcompat.searchview;


import com.coui.appcompat.R;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.WindowInsets;
import androidx.appcompat.widget.SearchView;
import java.lang.reflect.Field;


public class COUISearchView extends SearchView {
    private COUIHintAnimationLayout mCOUIHintAnimationLayout;
    private boolean mIsHintTextSize;
    private SearchView.SearchAutoComplete mSearchSrcTextView;

    public static class COUISearchAutoComplete extends SearchView.SearchAutoComplete {
        private static final int VERSION_CODE_U = 34;
        private boolean mEnableNativeKeyPreIme;

        public COUISearchAutoComplete(Context context) {
            super(context);
            this.mEnableNativeKeyPreIme = false;
        }

        @Override
        public boolean onKeyPreIme(int i2, KeyEvent keyEvent) {
            WindowInsets rootWindowInsets;
            boolean zOnKeyPreIme = super.onKeyPreIme(i2, keyEvent);
            if (Build.VERSION.SDK_INT < 34 || this.mEnableNativeKeyPreIme || (rootWindowInsets = getRootView().getRootWindowInsets()) == null || rootWindowInsets.isVisible(WindowInsets.Type.ime()) || i2 != 4) {
                return zOnKeyPreIme;
            }
            return false;
        }

        public void setEnableNativeKeyPreIme(boolean z6) {
            this.mEnableNativeKeyPreIme = z6;
        }

        public COUISearchAutoComplete(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mEnableNativeKeyPreIme = false;
        }

        public COUISearchAutoComplete(Context context, AttributeSet attributeSet, int i2) {
            super(context, attributeSet, i2);
            this.mEnableNativeKeyPreIme = false;
        }
    }

    public COUISearchView(Context context) {
        super(context);
        this.mIsHintTextSize = true;
    }

    private void changeTextSize(String str) {
        if (this.mSearchSrcTextView == null) {
            this.mSearchSrcTextView = getSearchAutoComplete();
        }
        if (str.isEmpty()) {
            this.mSearchSrcTextView.setTextSize(0, getContext().getResources().getDimensionPixelSize(R.dimen.coui_searchview_text_hint_size));
            this.mIsHintTextSize = true;
        } else if (this.mIsHintTextSize) {
            this.mSearchSrcTextView.setTextSize(0, getContext().getResources().getDimensionPixelSize(R.dimen.coui_searchview_text_size));
            this.mIsHintTextSize = false;
        }
    }

    public COUIHintAnimationLayout getHintAnimationLayout() {
        return this.mCOUIHintAnimationLayout;
    }

    public SearchView.SearchAutoComplete getSearchAutoComplete() {
        SearchView.SearchAutoComplete searchAutoComplete = this.mSearchSrcTextView;
        if (searchAutoComplete != null) {
            return searchAutoComplete;
        }
        try {
            Field declaredField = SearchView.class.getDeclaredField("mSearchSrcTextView");
            declaredField.setAccessible(true);
            SearchView.SearchAutoComplete searchAutoComplete2 = (SearchView.SearchAutoComplete) declaredField.get(this);
            this.mSearchSrcTextView = searchAutoComplete2;
            return searchAutoComplete2;
        } catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public COUISearchView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsHintTextSize = true;
    }

    public COUISearchView(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        this.mIsHintTextSize = true;
        this.mCOUIHintAnimationLayout = (COUIHintAnimationLayout) findViewById(R.id.search_animation_layout);
    }
}
