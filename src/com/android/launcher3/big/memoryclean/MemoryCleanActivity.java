package com.android.launcher3.big.memoryclean;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import com.android.launcher3.R;
import com.android.launcher3.pm.PinRequestHelper;

public class MemoryCleanActivity extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent().getAction().equals("android.intent.action.CREATE_SHORTCUT")) {
			Intent intent = new Intent(this, getClass());
			intent.setAction("android.intent.action.MAIN");
			LauncherApps.PinItemRequest createRequestForShortcut = PinRequestHelper.
					createRequestForShortcut(this, new ShortcutInfo.Builder(this, "clean_shortcut_id")
						.setShortLabel(getResources().getString(R.string.app_name_memory_clean))
						.setIcon(Icon.createWithResource(this, R.mipmap.hxy_clear_bg)).setIntent(intent).build());
			Intent intent2 = new Intent();
			intent2.putExtra("android.content.pm.extra.PIN_ITEM_REQUEST", createRequestForShortcut);
			intent2.setComponent(new ComponentName(this, getClass()));
			setResult(-1, intent2);
			finish();
        }
    }
}
