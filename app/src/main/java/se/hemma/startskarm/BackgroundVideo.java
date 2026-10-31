package se.hemma.startskarm;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Spelar bakgrundsklippen ljudlöst, ett i taget i slumpad ordning.
 *
 * Klippen hämtas i första hand från mappen
 *   /sdcard/Android/data/se.hemma.startskarm/files/videos/
 * (dit kan nya .mp4-filer läggas med adb push, utan att appen byggs om).
 * Finns inga klipp där används de som är inbyggda i appen (res/raw).
 *
 * Obs: skärmen hålls INTE vaken medan videon spelas, så tv:ns skärmsläckare
 * startar som vanligt.
 */
final class BackgroundVideo implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final int[] BUILT_IN = {R.raw.bg_isring, R.raw.bg_nebulosa};

    private final Context context;
    private final TextureView view;
    private final Random random = new Random();

    private MediaPlayer player;
    private Surface surface;
    private boolean wantPlaying;
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

    void release() {
        wantPlaying = false;
        releasePlayer();
    }

    private List<Uri> findClips() {
        List<Uri> clips = new ArrayList<>();
        File dir = context.getExternalFilesDir("videos");
        if (dir != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    String n = f.getName().toLowerCase(Locale.ROOT);
                    if (f.isFile() && (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".webm"))) {
                        clips.add(Uri.fromFile(f));
                    }
                }
            }
        }
        if (clips.isEmpty()) {
            for (int id : BUILT_IN) {
                clips.add(Uri.parse("android.resource://" + context.getPackageName() + "/" + id));
            }
        }
        return clips;
    }

    private void playNext() {
        releasePlayer();
        if (surface == null) return;

        List<Uri> clips = findClips();
        if (clips.isEmpty()) return;

        Uri next;
        do {
            next = clips.get(random.nextInt(clips.size()));
        } while (clips.size() > 1 && next.toString().equals(lastClip));
        lastClip = next.toString();

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
            mp.setDataSource(context, next);
            mp.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            view.postDelayed(this::retry, 2000);
        }
    }

    private void retry() {
        if (wantPlaying && player == null) playNext();
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
        if (wantPlaying) {
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
