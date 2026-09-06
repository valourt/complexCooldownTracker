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
        // Independent of the item-cooldown gates below (it has its own
        // specific pattern), so parse it unconditionally.
        SnakeEyesTracker.onChatLine(plainMessage);

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

        // Some servers broadcast every player's ability use publicly, not
        // just yours - a line mentioning another currently-online player by
        // name is their activation, not yours, so skip it before matching.
        if (OnlinePlayerCache.mentionsOtherPlayer(lower)) {
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

