package se.hemma.startskarm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Själva startskärmen. */
public class HomeActivity extends Activity {

    private BackgroundVideo video;
    private LinearLayout row;
    private TextView focusedName;
    private final List<View> tiles = new ArrayList<>();
    private int lastFocused = 0;

    /** Så här länge utan knapptryck spelas videon, sedan får skärmsläckaren ta över. */
    private static final long IDLE_MS = 60_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastInteraction = SystemClock.uptimeMillis();
    private final Runnable idleCheck = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.uptimeMillis() - lastInteraction >= IDLE_MS) {
                video.finishAndStop();
            } else {
                handler.postDelayed(this, 5_000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        video = new BackgroundVideo(this, (TextureView) findViewById(R.id.video));
        row = findViewById(R.id.app_row);
        focusedName = findViewById(R.id.focused_name);

        View allApps = findViewById(R.id.btn_all_apps);
        allApps.setOnClickListener(v -> open(new Intent(this, AllAppsActivity.class)));
        allApps.setOnFocusChangeListener((v, has) -> {
            if (has) focusedName.setText("");
        });

        View settings = findViewById(R.id.btn_settings);
        settings.setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        settings.setOnFocusChangeListener((v, has) -> {
            if (has) focusedName.setText("");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Byggs om varje gång, så att nyinstallerade appar kommer med
        buildRow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userActive();
        focusTile(lastFocused);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(idleCheck);
        video.pause();
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) userActive();
        return super.dispatchKeyEvent(event);
    }

    /** Någon använder fjärrkontrollen: spela video och starta om nedräkningen. */
    private void userActive() {
        lastInteraction = SystemClock.uptimeMillis();
        video.start();
        handler.removeCallbacks(idleCheck);
        handler.postDelayed(idleCheck, 5_000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(idleCheck);
        video.release();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Pausa när något annat ligger över, t.ex. skärmsläckaren
        if (hasFocus) userActive();
        else video.pause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Hemknappen trycktes medan startskärmen redan visades
        lastFocused = 0;
        focusTile(0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Tillbaka gör ingenting på startskärmen
    }

    private void buildRow() {
        row.removeAllViews();
        tiles.clear();

        List<Apps.App> apps = Apps.favorites(this);
        for (int i = 0; i < apps.size(); i++) {
            Apps.App app = apps.get(i);
            final int index = i;

            View tile = makeTile(this, app, dp(92), dp(52));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(92), dp(52));
            if (i > 0) lp.leftMargin = dp(12);
            row.addView(tile, lp);
            tiles.add(tile);

            tile.setOnFocusChangeListener((v, has) -> {
                float s = has ? 1.12f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(150).start();
                if (has) {
                    focusedName.setText(app.name);
                    lastFocused = index;
                }
            });
            tile.setOnClickListener(v -> open(app.launch));
        }

        if (apps.isEmpty()) {
            focusedName.setText("Hittade inga av favoritapparna. Tryck upp för Alla appar.");
        }
    }

    private void focusTile(int index) {
        if (tiles.isEmpty()) {
            findViewById(R.id.btn_all_apps).requestFocus();
            return;
        }
        int i = Math.max(0, Math.min(index, tiles.size() - 1));
        tiles.get(i).requestFocus();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Kunde inte öppna appen", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return dp(this, value);
    }

    static int dp(Context c, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, c.getResources().getDisplayMetrics()));
    }

    /** En appruta: appens banderoll om den finns, annars ikon och namn. */
    static View makeTile(Context c, Apps.App app, int width, int height) {
        FrameLayout tile = new FrameLayout(c);
        tile.setBackgroundResource(R.drawable.tile_bg);
        tile.setForeground(c.getDrawable(R.drawable.tile_focus));
        tile.setClipToOutline(true);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setContentDescription(app.name);

        if (app.banner != null) {
            ImageView banner = new ImageView(c);
            banner.setImageDrawable(app.banner);
            banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(banner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);

            ImageView icon = new ImageView(c);
            icon.setImageDrawable(app.icon);
            int size = Math.round(height * 0.45f);
            col.addView(icon, new LinearLayout.LayoutParams(size, size));

            TextView label = new TextView(c);
            label.setText(app.name);
            label.setTextColor(0xFFCFCCC5);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            label.setSingleLine(true);
            label.setPadding(dp(c, 4), dp(c, 2), dp(c, 4), 0);
            col.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            tile.addView(col, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        return tile;
    }
}
