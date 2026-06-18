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
    private static final int MAX_HISTORY = 5;
    private static final int COOLDOWN_TICKS = 6000; // 5 minutes between replies
    private static final float REPLY_CHANCE = 0.25f; // 25% chance to reply when triggered
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

            // Check if player repeated the same message 3 times
            if (history.size() >= 3) {
                String last = history.get(history.size() - 1);
                String secondLast = history.get(history.size() - 2);
                String thirdLast = history.get(history.size() - 3);

                if (last.equals(secondLast) && secondLast.equals(thirdLast)) {
                    // 25% chance to reply
                    if (RANDOM.nextFloat() < REPLY_CHANCE) {
                        String reply = getCreepyReply(msgText);
                        player.sendMessage(Text.literal("<???> " + reply)
                                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC), false);
                        replyCooldowns.put(playerId, COOLDOWN_TICKS);
                    }
                }
            }
        });
    }

    private static String getCreepyReply(String message) {
        // Check for keyword matches
        for (Map.Entry<String, String[]> entry : KEYWORD_REPLIES.entrySet()) {
            if (message.contains(entry.getKey())) {
                String[] replies = entry.getValue();
                return replies[RANDOM.nextInt(replies.length)];
            }
        }
        // Generic creepy reply
        return GENERIC_REPLIES[RANDOM.nextInt(GENERIC_REPLIES.length)];
    }

    // Decrement cooldowns
    public static void tick() {
        replyCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }
}