package org.bibleplugin.spigot.sound;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bibleplugin.spigot.config.Settings;

/** Plays {@link UiSound}s for players, honouring the {@code sounds.*} configuration. */
public final class SoundPlayer {

    private final boolean enabled;
    private final float volume;

    public SoundPlayer(Settings settings) {
        this.enabled = settings.soundsEnabled();
        this.volume = settings.soundVolume();
    }

    public void play(Player player, UiSound sound) {
        if (!enabled || volume <= 0f || player == null || !player.isOnline()) {
            return;
        }
        player.playSound(player.getLocation(), sound.sound(), SoundCategory.MASTER, volume, sound.pitch());
    }
}
