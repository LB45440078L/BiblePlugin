package org.bibleplugin.spigot.sound;

import org.bukkit.Sound;

/**
 * Sounds the plugin uses for feedback. Each constant wraps a vanilla {@link Sound} and a pitch.
 * The constants are static fields of the Bukkit API, so they link on every server version
 * from 1.21 to 26.x.
 */
public enum UiSound {
    CLICK(Sound.UI_BUTTON_CLICK, 1.0f),
    PAGE(Sound.ITEM_BOOK_PAGE_TURN, 1.0f),
    OPEN(Sound.BLOCK_CHEST_OPEN, 1.4f),
    SUCCESS(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f),
    DOWNLOADED(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f),
    ERROR(Sound.ENTITY_VILLAGER_NO, 1.0f),
    TURN_ON(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);

    private final Sound sound;
    private final float pitch;

    UiSound(Sound sound, float pitch) {
        this.sound = sound;
        this.pitch = pitch;
    }

    public Sound sound() {
        return sound;
    }

    public float pitch() {
        return pitch;
    }
}
