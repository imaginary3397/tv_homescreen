package se.hemma.startskarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Hittar installerade tv-appar och väljer ut favoriterna till appraden. */
final class Apps {

    /**
     * Favoriterna, i den ordning de ska visas.
     * Första ordet är namnet som visas. Resten är sökord som jämförs med appens namn på tv:n
     * (det första sökordet som ger träff vinner). Ändra, ta bort eller lägg till rader här.
     */
    static final String[][] FAVORITES = {
            {"SVT Play", "svt play", "svt"},
            {"UR Play", "ur play"},
            {"YouTube", "youtube"},
            {"TV4 Play", "tv4 play", "tv4"},
            {"Netflix", "netflix"},
            {"Disney+", "disney+", "disney"},
            {"Prime Video", "prime video", "amazon prime", "prime"},
            {"Spotify", "spotify"},
    };

    static final class App {
        String name;
        String label;
        Intent launch;
        Drawable banner;
        Drawable icon;
    }

    private Apps() {
    }

    /** Alla appar som går att starta på en tv, sorterade efter namn. */
    static List<App> all(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        List<App> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || context.getPackageName().equals(ai.packageName)) continue;
            if (!seen.add(ai.packageName)) continue;

            App app = new App();
            app.label = String.valueOf(ri.loadLabel(pm));
            app.name = app.label;
            app.banner = ai.loadBanner(pm);
            if (app.banner == null) app.banner = ai.applicationInfo.loadBanner(pm);
            app.icon = ri.loadIcon(pm);

            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            launch.setClassName(ai.packageName, ai.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            app.launch = launch;

            result.add(app);
        }
        Collections.sort(result, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }

    /** Favoriterna som faktiskt finns installerade, i rätt ordning. */
    static List<App> favorites(Context context) {
        List<App> installed = all(context);
        List<App> result = new ArrayList<>();
        Set<App> used = new HashSet<>();

        for (String[] fav : FAVORITES) {
            App match = null;
            for (int k = 1; k < fav.length && match == null; k++) {
                String key = fav[k].toLowerCase(Locale.ROOT);
                for (App app : installed) {
                    if (used.contains(app)) continue;
                    if (!app.label.toLowerCase(Locale.ROOT).contains(key)) continue;
                    // Kortast namn vinner, så att "YouTube" väljs före "YouTube Kids"
                    if (match == null || app.label.length() < match.label.length()) match = app;
                }
            }
            if (match != null) {
                used.add(match);
                match.name = fav[0];
                result.add(match);
            }
        }
        return result;
    }
}
