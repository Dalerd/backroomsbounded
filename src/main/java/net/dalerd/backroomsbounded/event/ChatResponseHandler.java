package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class ChatResponseHandler {

    private static final Map<UUID, List<String>> playerMessageHistory = new HashMap<>();
    private static final Map<UUID, Integer> replyCooldowns = new HashMap<>();
    private static final Map<UUID, Set<String>> usedReplies = new HashMap<>();
    private static final int MAX_HISTORY = 8;
    private static final int COOLDOWN_TICKS = 12000; // 10 minutes between replies
    private static final float REPEAT_REPLY_CHANCE = 0.40f; // 40% chance when repeating
    private static final float KEYWORD_REPLY_CHANCE = 0.15f; // 15% chance on keyword match
    private static final Random RANDOM = new Random();

    // Creepy replies based on message content
    private static final Map<String, String[]> KEYWORD_REPLIES = new LinkedHashMap<>() {{
        put("hello", new String[]{"...hi?", "who's there?", "you're not alone", "hello?"});
        put("help", new String[]{"there is no help", "no one can help you", "help is not coming", "save yourself"});
        put("exit", new String[]{"there is no exit", "you can't leave", "the exit is a lie", "stop looking"});
        put("where", new String[]{"you are nowhere", "level 0", "the backrooms", "you don't want to know"});
        put("run", new String[]{"running won't help", "it's faster than you", "you can't escape", "keep running"});
        put("who", new String[]{"the bacterium", "it has no name", "something ancient", "don't ask"});
        put("why", new String[]{"why not?", "because you're here now", "no reason", "it just is"});
        put("scared", new String[]{"you should be", "good", "fear is smart", "it can smell fear"});
        put("lost", new String[]{"we all are", "there is no map", "welcome to the club", "forever lost"});
        put("die", new String[]{"you will", "eventually", "it's only a matter of time", "death is mercy here"});
        put("light", new String[]{"don't trust the lights", "they flicker for a reason", "darkness is safer", "the lights lie"});
        put("dark", new String[]{"you get used to it", "the dark is alive", "something lives in the dark", "embrace it"});
        put("noise", new String[]{"you heard it too?", "it's getting closer", "don't make a sound", "pretend you didn't hear"});
        put("door", new String[]{"don't open it", "it might be behind that door", "doors are traps", "some doors should stay closed"});
    }};

    // Generic replies when no keywords match
    private static final String[] GENERIC_REPLIES = {
            "...", "shh", "did you hear that?", "it's watching",
            "keep moving", "don't stop", "behind you", "i see you",
            "they know", "it's close", "listen", "quiet"
    };

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (!(sender instanceof ServerPlayerEntity player)) return;
            if (player.getWorld().getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return;

            String msgText = message.getSignedContent().toLowerCase().trim();
            UUID playerId = player.getUuid();

            // Check cooldown
            int cooldown = replyCooldowns.getOrDefault(playerId, 0);
            if (cooldown > 0) return;

            // Track message history
            List<String> history = playerMessageHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
            history.add(msgText);
            if (history.size() > MAX_HISTORY) history.remove(0);

            // Track used replies per player to avoid repeats
            Set<String> used = usedReplies.computeIfAbsent(playerId, k -> new HashSet<>());

            boolean shouldReply = false;

            // Check if player repeated the same message twice (not three times)
            if (history.size() >= 2) {
                String last = history.get(history.size() - 1);
                String secondLast = history.get(history.size() - 2);
                if (last.equals(secondLast) && RANDOM.nextFloat() < REPEAT_REPLY_CHANCE) {
                    shouldReply = true;
                }
            }

            // Check for keyword matches (single message can trigger)
            if (!shouldReply) {
                for (String keyword : KEYWORD_REPLIES.keySet()) {
                    if (msgText.contains(keyword) && RANDOM.nextFloat() < KEYWORD_REPLY_CHANCE) {
                        shouldReply = true;
                        break;
                    }
                }
            }

            if (shouldReply) {
                String reply = getCreepyReply(msgText, used);
                player.sendMessage(Text.literal("<???> " + reply)
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC), false);
                replyCooldowns.put(playerId, COOLDOWN_TICKS);
                used.add(reply);

                // Reset used replies if we've gone through most of them
                if (used.size() >= 8) used.clear();
            }
        });
    }

    private static String getCreepyReply(String message, Set<String> usedReplies) {
        // Check for keyword matches
        for (Map.Entry<String, String[]> entry : KEYWORD_REPLIES.entrySet()) {
            if (message.contains(entry.getKey())) {
                String[] replies = entry.getValue();
                // Find a reply not used yet
                List<String> unused = new ArrayList<>();
                for (String r : replies) {
                    if (!usedReplies.contains(r)) unused.add(r);
                }
                if (!unused.isEmpty()) {
                    return unused.get(RANDOM.nextInt(unused.size()));
                }
                // All used, pick random anyway
                return replies[RANDOM.nextInt(replies.length)];
            }
        }
        // Generic creepy reply - avoid repeats
        List<String> unusedGeneric = new ArrayList<>();
        for (String r : GENERIC_REPLIES) {
            if (!usedReplies.contains(r)) unusedGeneric.add(r);
        }
        if (!unusedGeneric.isEmpty()) {
            return unusedGeneric.get(RANDOM.nextInt(unusedGeneric.size()));
        }
        return GENERIC_REPLIES[RANDOM.nextInt(GENERIC_REPLIES.length)];
    }

    public static void tick() {
        replyCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }
}