package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TextMatch;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.cooldown.CooldownManager;

public class ChatCooldownListener {

    /**
     * Every genuine "you successfully used this" message we've seen from
     * this server includes one of these checkmark characters right after
     * the item name. Denial messages (on cooldown, no target, wrong
     * context, etc.) use an X, hourglass, or warning symbol instead. Rather
     * than trying to blacklist every possible denial phrase - which only
     * covers the ones we happen to know about - requiring a success symbol
     * to be present is a general fix: only genuine "it worked" messages
     * ever start a cooldown, regardless of how a denial happens to be worded.
     */
    private static final String[] SUCCESS_SYMBOLS = {"\u2714", "\u2705", "\u2713"};

    public static void onChatLine(String plainMessage) {
        // Independent of the item-cooldown gates below (each has its own
        // specific pattern), so parse these unconditionally.
        SnakeEyesTracker.onChatLine(plainMessage);
        GrilledPlayerTracker.onChatLine(plainMessage);

        String lower = plainMessage.toLowerCase();

        boolean hasSuccessSymbol = false;
        for (String symbol : SUCCESS_SYMBOLS) {
            if (plainMessage.contains(symbol)) {
                hasSuccessSymbol = true;
                break;
            }
        }
        if (!hasSuccessSymbol) {
            return;
        }

        // "No longer X" describes an effect ENDING (a debuff someone else
        // applied to you wearing off, e.g. "You are no longer covered in
        // mucus"), not an activation - it still uses "You" and a checkmark,
        // so it'd otherwise pass every gate above and wrongly start a
        // cooldown for an ability someone else used on you. Same principle
        // as the "on cooldown" exclusion: skip anything describing an
        // ending, regardless of which item it's about.
        if (lower.contains("no longer")) {
            return;
        }

        // Some servers broadcast every player's ability use publicly, not
        // just yours - a line mentioning another currently-online player by
        // name is USUALLY their activation, not yours. But an ability that
        // targets another player (pulls them, binds them, etc.) can mention
        // that player's name in YOUR OWN success message too, e.g. "Making
        // you and Cow gravitate towards each other!" - which would otherwise
        // be wrongly skipped just because "Cow" happens to be someone's
        // username. The distinguishing signal is "you"/"your": your own
        // activation is second-person, a genuine other-player broadcast is
        // third-person (their name as the actor, no "you" anywhere) - so
        // only apply the exclusion when "you" is absent.
        if (OnlinePlayerCache.mentionsOtherPlayer(lower) && !lower.contains("you")) {
            return;
        }

        for (TrackedItem item : CooldownConfig.getChatTriggerItems()) {
            boolean matched;
            if (item.literalMatch != null) {
                // Fast path: ~500 of a fully-populated config are auto-
                // generated from a plain item name, not a real regex - a
                // hand-written substring scan is far cheaper here than the
                // regex engine, and this runs against every chat line for
                // every tracked item.
                matched = TextMatch.containsWholeWord(lower, item.literalMatch);
            } else if (item.compiledPattern != null) {
                matched = item.compiledPattern.matcher(plainMessage).find();
            } else {
                matched = false;
            }

            if (matched) {
                CooldownManager.start(item.id, item.displayName, item.cooldownSeconds);
            }
        }
    }
}

