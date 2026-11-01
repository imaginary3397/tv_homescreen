package se.hemma.startskarm;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Spelar bakgrundsklippen ljudlöst, ett i taget, i namnordning (01_…, 02_… osv.) och börjar om
 * när sista klippet spelats.
 *
 * Klippen ligger inbyggda i appen (assets/videos, skapas från mappen klipp/ i repot när appen byggs).
 * Finns det .mp4-filer i mappen
 *   /sdcard/Android/data/se.hemma.startskarm/files/videos/
 * används de i stället, så att klipp kan provas utan att bygga om appen.
 *
 * Säsongsklipp: ett klipp med "halloween" i namnet spelas bara 24 oktober–1 november.
 *
 * Obs: Sonys tv räknar pågående video som aktivitet. Därför stoppar HomeActivity videon när
 * fjärrkontrollen inte rörts på en minut, så att tv:ns skärmsläckare kan starta.
 */
final class BackgroundVideo implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String ASSET_DIR = "videos";

    /** Ett klipp: antingen en fil på tv:n eller en fil inbyggd i appen. */
    private static final class Clip {
        final String name;
        final File file;

        Clip(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }

    private final Context context;
    private final TextureView view;

    private MediaPlayer player;
    private Surface surface;
    private boolean wantPlaying;
    private boolean stopAfterCurrent;
    private String lastClip;

    BackgroundVideo(Context context, TextureView view) {
        this.context = context;
        this.view = view;
        view.setSurfaceTextureListener(this);
        // Skapar mappen för egna klipp om den inte finns
        context.getExternalFilesDir("videos");
    }

    void start() {
        wantPlaying = true;
        stopAfterCurrent = false;
        if (surface == null) return;
        if (player == null) {
            playNext();
        } else if (!player.isPlaying()) {
            try {
                player.start();
            } catch (IllegalStateException e) {
                playNext();
            }
        }
    }

    void pause() {
        wantPlaying = false;
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    /**
     * Låter det klipp som spelas ta slut och startar inget nytt.
     * Används när ingen rört fjärrkontrollen på ett tag, så att tv:ns skärmsläckare kan starta.
     */
    void finishAndStop() {
        stopAfterCurrent = true;
    }

    boolean isStopping() {
        return stopAfterCurrent;
    }

    void release() {
        wantPlaying = false;
        releasePlayer();
    }

    /** Alla klipp som ska spelas just nu, sorterade efter namn. */
    private List<Clip> findClips() {
        List<Clip> clips = new ArrayList<>();

        File dir = context.getExternalFilesDir("videos");
        if (dir != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && isVideo(f.getName())) clips.add(new Clip(f.getName(), f));
                }
            }
        }

        if (clips.isEmpty()) {
            try {
                String[] names = context.getAssets().list(ASSET_DIR);
                if (names != null) {
                    for (String n : names) {
                        if (isVideo(n)) clips.add(new Clip(n, null));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<Clip> active = new ArrayList<>();
        boolean halloween = isHalloweenSeason();
        for (Clip c : clips) {
            boolean seasonal = c.name.toLowerCase(Locale.ROOT).contains("halloween");
            if (!seasonal || halloween) active.add(c);
        }
        Collections.sort(active, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return active;
    }

    private static boolean isVideo(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".webm");
    }

    /** 24 oktober till och med 1 november. */
    private static boolean isHalloweenSeason() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);
        return (month == Calendar.OCTOBER && day >= 24) || (month == Calendar.NOVEMBER && day <= 1);
    }

    /** Nästa klipp i namnordning efter det senast spelade, annars det första. */
    private Clip pickNext(List<Clip> clips) {
        if (lastClip != null) {
            for (Clip c : clips) {
                if (c.name.compareToIgnoreCase(lastClip) > 0) return c;
            }
        }
        return clips.get(0);
    }

    private void playNext() {
        releasePlayer();
        if (surface == null) return;

        List<Clip> clips = findClips();
        if (clips.isEmpty()) return;

        Clip next = pickNext(clips);
        lastClip = next.name;

        MediaPlayer mp = new MediaPlayer();
        player = mp;
        try {
            mp.setSurface(surface);
            mp.setVolume(0f, 0f);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnPreparedListener(p -> {
                if (p == player && wantPlaying) p.start();
            });
            if (next.file != null) {
                mp.setDataSource(next.file.getAbsolutePath());
            } else {
                try (AssetFileDescriptor afd = context.getAssets().openFd(ASSET_DIR + "/" + next.name)) {
                    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }
            }
            mp.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            view.postDelayed(this::retry, 2000);
        }
    }

    private void retry() {
        if (wantPlaying && !stopAfterCurrent && player == null) playNext();
    }

    private void releasePlayer() {
        if (player != null) {
            MediaPlayer old = player;
            player = null;
            try {
                old.release();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp != player) return;
        if (wantPlaying && !stopAfterCurrent) {
            playNext();
        } else {
            releasePlayer();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mp == player) {
            releasePlayer();
            view.postDelayed(this::retry, 2000);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        surface = new Surface(texture);
        if (wantPlaying) start();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        releasePlayer();
        if (surface != null) {
            surface.release();
            surface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
    }
}
