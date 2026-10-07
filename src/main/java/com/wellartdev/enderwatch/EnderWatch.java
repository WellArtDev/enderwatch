package com.wellartdev.enderwatch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Warns the player the moment their crosshair lands on an enderman.
 *
 * One stray glance at an enderman turns it hostile, and many deaths
 * come from never noticing what the crosshair was resting on.
 * EnderWatch polls the crosshair target every few ticks; when the
 * player is looking at an enderman it shows an amber warning above
 * the hotbar and plays a single soft pling per stare so the player
 * can look away before the enderman gets angry.
 *
 * Client-side only. No mixins, no networking, nothing persisted.
 */
public class EnderWatch implements ClientModInitializer {
    public static final String MOD_ID = "enderwatch";

    /** How often (in client ticks) to re-check the crosshair target. */
    private static final int CHECK_EVERY_TICKS = 3;

    /** Amber text — alarming at a glance, not panicking. */
    private static final int WARNING_COLOR = 0xFFAA00;

    /** The warning line shown above the hotbar while an enderman is targeted. */
    private static final String WARNING_TEXT = "Careful — enderman in your sights!";

    // Read by the HUD element every frame; updated on tick below.
    private boolean targetingEnderman = false;
    private int tick = 0;

    // The enderman that already triggered the pling during this
    // continuous stare. Cleared the moment the player looks away, so
    // the next stare pings again.
    private Entity warnedEntity = null;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "ender_watch"),
                (extractor, deltaTracker) -> {
                    if (!targetingEnderman) {
                        return;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(WARNING_TEXT)) / 2;
                    int y = mc.getWindow().getGuiScaledHeight() - 70;
                    extractor.text(mc.font, WARNING_TEXT, x, y, WARNING_COLOR);
                });
    }

    private void onTick(Minecraft mc) {
        tick++;
        if (mc.player == null || tick % CHECK_EVERY_TICKS != 0) {
            return;
        }

        Entity target = null;
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity hit = entityHit.getEntity();
            if (hit instanceof EnderMan) {
                target = hit;
            }
        }

        targetingEnderman = target != null;

        if (target == null) {
            // Looked away — re-arm the pling for the next stare.
            warnedEntity = null;
            return;
        }

        if (warnedEntity != target) {
            warnedEntity = target;
            // Soft pling once per continuous stare. Still audible in
            // combat without adding to the noise.
            mc.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.0f));
        }
    }
}
