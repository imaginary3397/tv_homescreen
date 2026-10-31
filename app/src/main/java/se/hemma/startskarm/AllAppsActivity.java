package se.hemma.startskarm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Enkel lista över alla installerade tv-appar. Nås via "Alla appar" på startskärmen. */
public class AllAppsActivity extends Activity {

    private static final int COLUMNS = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(48);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F1114);
        root.setPadding(pad, dp(30), pad, 0);
        root.setClipChildren(false);

        TextView title = new TextView(this);
        title.setText(R.string.all_apps);
        title.setTextColor(0xFFECEAE6);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(16), 0, dp(32));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(COLUMNS);
        grid.setClipChildren(false);
        grid.setUseDefaultMargins(false);

        int w = dp(124);
        int h = dp(70);
        List<Apps.App> apps = Apps.all(this);
        View first = null;
        for (Apps.App app : apps) {
            View tile = HomeActivity.makeTile(this, app, w, h);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = w;
            lp.height = h;
            lp.setMargins(dp(8), dp(8), dp(8), dp(8));
            grid.addView(tile, lp);

            tile.setOnFocusChangeListener((v, has) -> {
                float s = has ? 1.08f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(150).start();
            });
            tile.setOnClickListener(v -> {
                try {
                    startActivity(app.launch);
                } catch (ActivityNotFoundException | SecurityException e) {
                    Toast.makeText(this, "Kunde inte öppna appen", Toast.LENGTH_SHORT).show();
                }
            });
            if (first == null) first = tile;
        }

        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        if (first != null) first.requestFocus();
    }

    private int dp(int value) {
        return HomeActivity.dp(this, value);
    }
}
