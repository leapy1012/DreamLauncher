package com.android.launcher3.screenedit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.Workspace;
import com.android.launcher3.effect.ScrollEffect;
import com.android.launcher3.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScrollEffectAdapter extends GridGalleryAdapter {
    // 上下文对象
    private Context context;
    // Launcher 实例
    private Launcher launcher;
    // 布局加载器
    private LayoutInflater layoutInflater;
    // 滚动效果列表
    private List<ScrollEffectItem> scrollEffectItems;
    // 当前选中的滚动效果名称
    private String currentEffectName;
    // 图标文本视图的弱引用列表
    private List<WeakReference<IconTextView>> iconTextViewReferences;

    public class ScrollEffectItem {
        // 滚动效果名称
        public String effectName;
        // 滚动效果的字符串资源 ID
        public int effectStringResId;
        // 滚动效果的图标资源 ID
        public int effectIconResId;

        public ScrollEffectItem(String effectName, int effectStringResId, int effectIconResId) {
            this.effectName = effectName;
            this.effectStringResId = effectStringResId;
            this.effectIconResId = effectIconResId;
        }
    }

    // 视图持有者类，用于缓存视图组件
    private static class ViewHolder {
        // 图标文本视图
        public IconTextView effectButton;
        // 内容视图
        public View contentView;

        public ViewHolder() {
        }
    }

    public ScrollEffectAdapter(Context context) {
        this.context = context;
        this.launcher = (Launcher) context;
        this.layoutInflater = LayoutInflater.from(context);
        // 获取当前的滚动效果名称
        String workspaceScrollEffect = LauncherPrefs.get(context).get(LauncherPrefs.WORKSPACE_SCROLL_EFFECT);
        this.currentEffectName = TextUtils.isEmpty(workspaceScrollEffect) ? "none" : workspaceScrollEffect;
        Log.d("zr_effect", "ScrollEffectAdapter mEffectName=" + this.currentEffectName);

        this.iconTextViewReferences = new ArrayList<>();
        this.scrollEffectItems = new ArrayList<>();
        // 添加各种滚动效果项
        this.scrollEffectItems.add(new ScrollEffectItem("none", R.string.transition_effect_none, R.drawable.screen_edit_effect_normal));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OPPO_ROLL, R.string.transition_effect_oppo_roll, R.drawable.screen_edit_effect_carousel_left));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OPPO_CUBE, R.string.transition_effect_oppo_cube, R.drawable.screen_edit_effect_cube_in));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OPPO_FLIP, R.string.transition_effect_oppo_flip, R.drawable.screen_edit_effect_flip));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OPPO_CARD, R.string.transition_effect_oppo_card, R.drawable.screen_edit_effect_stack));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OPPO_TILT, R.string.transition_effect_oppo_tilt, R.drawable.screen_edit_effect_carousel_right));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_STACK, R.string.transition_effect_stack, R.drawable.screen_edit_effect_stack));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_ACCORDION, R.string.transition_effect_accordion, R.drawable.screen_edit_effect_accordian));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_CUBE_IN, R.string.transition_effect_cubein, R.drawable.screen_edit_effect_cube_in));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_CUBE_OUT, R.string.transition_effect_cubeout, R.drawable.screen_edit_effect_cube_out));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_OVERVIEW, R.string.transition_effect_overview, R.drawable.screen_edit_effect_overview));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_CROSS, R.string.transition_effect_cross, R.drawable.screen_edit_effect_cross));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_FLIP, R.string.transition_effect_flip, R.drawable.screen_edit_effect_flip));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_WINDMILL, R.string.transition_effect_windmill, R.drawable.screen_edit_effect_windmill));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_WHEEL, R.string.transition_effect_wheel, R.drawable.screen_edit_effect_wheel));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_CAROUSEL_LEFT, R.string.transition_effect_carousel_left, R.drawable.screen_edit_effect_carousel_left));
        this.scrollEffectItems.add(new ScrollEffectItem(ScrollEffect.SCROLL_EFFECT_CAROUSEL_RIGHT, R.string.transition_effect_carousel_right, R.drawable.screen_edit_effect_carousel_right));
    }

    // 更新图标文本视图的选中状态
    public final void updateIconTextViewSelection(int selectedPosition) {
        for (int i = 0; i < iconTextViewReferences.size(); i++) {
            IconTextView iconTextView = iconTextViewReferences.get(i).get();
            if (iconTextView == null) {
                iconTextViewReferences.remove(i);
            } else {
                iconTextView.setSelect((Integer) iconTextView.getTag() == selectedPosition);
            }
        }
    }

    public int getCount() {
        return scrollEffectItems.size();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.grid_gallery_item, parent, false);
            viewHolder.effectButton = convertView.findViewById(R.id.effect_button);
            viewHolder.contentView = convertView.findViewById(R.id.content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ScrollEffectItem effectItem = scrollEffectItems.get(position);
        viewHolder.effectButton.setCompoundDrawables(null, context.getDrawable(effectItem.effectIconResId), null, null);
        viewHolder.effectButton.setText(effectItem.effectStringResId);

        viewHolder.contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Workspace<?> workspace = launcher.getWorkspace();
                workspace.setScrollEffectFromString(effectItem.effectName);
                workspace.showScrollEffectAnimation();
                LauncherPrefs.get(context).put(LauncherPrefs.WORKSPACE_SCROLL_EFFECT, effectItem.effectName);
                currentEffectName = effectItem.effectName;
                updateIconTextViewSelection(position);
            }
        });

        Log.d("zr_effect", "ScrollEffectAdapter getView mEffectName=" + currentEffectName + ", bean.effectName=" + effectItem.effectName);

        viewHolder.effectButton.setSelect(currentEffectName.equals(effectItem.effectName));
        viewHolder.effectButton.setTag(position);
        iconTextViewReferences.add(new WeakReference<>(viewHolder.effectButton));

        return convertView;
    }
}
