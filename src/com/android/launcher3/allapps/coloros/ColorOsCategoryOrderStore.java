package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.android.customize.overlay.model.CategoryInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists user-reordered Categories folder order (Oppo {@code category_order_prefs}).
 */
final class ColorOsCategoryOrderStore {

    private static final String PREFS = "category_order_prefs";
    private static final String KEY_ITEM_ORDER = "item_order";

    private ColorOsCategoryOrderStore() {
    }

    static void applyOrder(@NonNull Context context, @NonNull List<CategoryInfo> categories) {
        List<String> saved = load(context);
        if (saved.isEmpty() || categories.size() <= 1) {
            return;
        }
        Map<String, Integer> rank = new HashMap<>(saved.size());
        for (int i = 0; i < saved.size(); i++) {
            rank.put(saved.get(i), i);
        }
        // Stable fallback keeps assets order for folders never dragged.
        Map<String, Integer> original = new HashMap<>(categories.size());
        for (int i = 0; i < categories.size(); i++) {
            original.put(categories.get(i).getFolderName(), i);
        }
        Collections.sort(categories, (a, b) -> {
            int ra = rank.containsKey(a.getFolderName())
                    ? rank.get(a.getFolderName()) : Integer.MAX_VALUE;
            int rb = rank.containsKey(b.getFolderName())
                    ? rank.get(b.getFolderName()) : Integer.MAX_VALUE;
            if (ra != rb) {
                return Integer.compare(ra, rb);
            }
            return Integer.compare(
                    original.getOrDefault(a.getFolderName(), 0),
                    original.getOrDefault(b.getFolderName(), 0));
        });
    }

    static void saveOrder(@NonNull Context context, @NonNull List<String> folderNames) {
        JSONArray array = new JSONArray();
        for (String name : folderNames) {
            if (name != null && !name.isEmpty()) {
                array.put(name);
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEM_ORDER, array.toString())
                .apply();
    }

    @NonNull
    private static List<String> load(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ITEM_ORDER, null);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // Prefer JSONArray of folder names (Dream format).
            if (raw.trim().startsWith("[")) {
                JSONArray array = new JSONArray(raw);
                List<String> out = new ArrayList<>(array.length());
                for (int i = 0; i < array.length(); i++) {
                    String name = array.optString(i, null);
                    if (name != null && !name.isEmpty()) {
                        out.add(name);
                    }
                }
                return out;
            }
        } catch (JSONException ignored) {
            // Fall through.
        }
        return Collections.emptyList();
    }
}
