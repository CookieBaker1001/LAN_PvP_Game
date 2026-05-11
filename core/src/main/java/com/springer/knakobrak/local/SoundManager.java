package com.springer.knakobrak.local;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

public class SoundManager {

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final ObjectMap<String, Music> musicTracks = new ObjectMap<>();

    private Music currentMusic;

    private float musicVolume = 0.4f;
    private float soundVolume = 1f;

    private boolean muted = false;

    public void loadSound(String id, String file) {
        if (!sounds.containsKey(id)) {
            sounds.put(id, Gdx.audio.newSound(Gdx.files.internal(file)));
        }
    }

    public void loadMusic(String id, String file) {
        if (!musicTracks.containsKey(id)) {
            musicTracks.put(id, Gdx.audio.newMusic(Gdx.files.internal(file)));
        }
    }

    public void playSound(String id) {
        if (muted) return;

        Sound sound = sounds.get(id);
        if (sound != null) {
            sound.play(soundVolume);
        }
    }

    public void playMusic(String id, boolean loop) {
        if (currentMusic != null) {
            currentMusic.stop();
        }

        currentMusic = musicTracks.get(id);

        if (currentMusic != null) {
            currentMusic.setLooping(loop);
            currentMusic.setVolume(muted ? 0f : musicVolume);
            currentMusic.play();
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }


    public void pauseAll() {
        if (currentMusic != null) currentMusic.pause();
    }

    public void resumeAll() {
        if (currentMusic != null && !muted) currentMusic.play();
    }

    public void setSoundVolume(float volume) {
        soundVolume = MathUtils.clamp(volume, 0f, 1f);
    }

    public void setMusicVolume(float volume) {
        musicVolume = MathUtils.clamp(volume, 0f, 1f);
        if (currentMusic != null) {
            currentMusic.setVolume(muted ? 0f : musicVolume);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (currentMusic != null) {
            currentMusic.setVolume(muted ? 0f : musicVolume);
        }
    }

    public void dispose() {
        for (Sound s : sounds.values()) s.dispose();
        for (Music m : musicTracks.values()) m.dispose();

        sounds.clear();
        musicTracks.clear();
    }
}
