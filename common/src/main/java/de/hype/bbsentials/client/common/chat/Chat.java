package de.hype.bbsentials.client.common.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.hype.bbsentials.client.common.SystemUtils;
import de.hype.bbsentials.client.common.client.BBsentials;
import de.hype.bbsentials.client.common.client.objects.TrustedPartyMember;
import de.hype.bbsentials.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bbsentials.client.common.hpmodapi.HPModAPIPacket;
import de.hype.bbsentials.client.common.mclibraries.EnvironmentCore;
import de.hype.bbsentials.client.common.objects.ChatPrompt;
import de.hype.bbsentials.shared.constants.Formatting;
import de.hype.bbsentials.shared.constants.Islands;
import de.hype.bbsentials.shared.constants.StatusConstants;
import de.hype.bbsentials.shared.objects.Message;
import de.hype.bbsentials.shared.packets.network.CompletedGoalPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat {

    public static Date lastNPCMessage = new Date();
    //{"strikethrough":false,"extra":[{"strikethrough":false,"clickEvent":{"action":"run_command","value":"/viewprofile 4fa1228c-8dd6-47c4-8fe3-b04b580311b8"},"hoverEvent":{"action":"show_text","contents":{"strikethrough":false,"text":"§eClick here to view §bHype_the_Time§e's profile"}},"text":"§9Party §8> §b[MVP§2+§b] Hype_the_Time§f: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"text":"h:test"}],"text":""}// {"strikethrough":false,"extra":[{"strikethrough":false,"clickEvent":{"action":"run_command","value":"/viewprofile f772b2c7-bd2a-46e1-b1a2-41fa561157d6"},"hoverEvent":{"action":"show_text","contents":{"strikethrough":false,"text":"§eClick here to view §bShourtu§e's profile"}},"text":"§9Party §8> §b[MVP§c+§b] Shourtu§f: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"text":"Hype_the_Time TEST"}],"text":""}
    //{"strikethrough":false,"extra":[{"strikethrough":false,"clickEvent":{"action":"run_command","value":"/viewprofile 4fa1228c-8dd6-47c4-8fe3-b04b580311b8"},"hoverEvent":{"action":"show_text","contents":{"strikethrough":false,"text":"§eClick here to view §bHype_the_Time§e's profile"}},"text":"§9Party §8> §b[MVP§2+§b] Hype_the_Time§f: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"text":"h:test"}],"text":""}
    private final Map<String, Instant> partyDisbandedMap = new HashMap<>();
    private String lastPartyDisbandedUsername = null;

    public static String[] getVariableNames(String packageName, String className) {
        List<String> variableInfoList = new ArrayList<>();

        // Combine the class name with the package name
        String fullClassName = packageName + "." + className;

        // Load the class
        Class<?> clazz = null;
        try {
            clazz = Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Extract fields of the class
        Field[] fields = clazz.getDeclaredFields();

        // Collect information for each field
        for (Field field : fields) {
            // Exclude transient fields
            if (java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            String variableName = field.getName();
            String variablePackageName = clazz.getPackage().getName();
            String variableClassName = clazz.getSimpleName();

            variableInfoList.add(variableName);
        }

        return variableInfoList.toArray(new String[variableInfoList.size()]);
    }

    public static void setVariableValue(String className, String variableName, String value) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        if (value == null) {
            sendPrivateMessageToSelfError("Invalid value: null");
            return;
        }

        String fullClassName = "de.hype.bbsentials.client.common.config" + "." + className;
        Object obj = null;
        Class<?> clazz = Class.forName(fullClassName);
        Field field = clazz.getDeclaredField(variableName);
        field.setAccessible(true);

        Class<?> fieldType = field.getType();
        Object convertedValue = parseValue(value, fieldType);

        if (Modifier.isStatic(field.getModifiers())) {
            field.set(null, convertedValue);
        } else {
            obj = clazz.getDeclaredConstructor().newInstance();
            field.set(obj, convertedValue);
        }

        sendPrivateMessageToSelfSuccess("The variable " + field.getName() + " is now: " + field.get(obj));
    }

    public static void getVariableValue(String className, String variableName) {
        String fullClassName = "de.hype.bbsentials.client.common.config" + "." + className;

        try {
            Class<?> clazz = Class.forName(fullClassName);
            Field field = clazz.getDeclaredField(variableName);
            field.setAccessible(true);

            Object obj = clazz.getDeclaredConstructor().newInstance();
            sendPrivateMessageToSelfSuccess("The variable " + field.getName() + " is: " + field.get(obj));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InstantiationException |
                 InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static Object parseValue(String value, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else {
            // For other types, return the original string value
            return value;
        }
    }

    public static void sendPrivateMessageToSelfError(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.RED);
    }

    public static void sendPrivateMessageToSelfFatal(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.DARK_RED);
    }

    public static void sendPrivateMessageToSelfSuccess(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.GREEN);
    }

    public static void sendPrivateMessageToSelfInfo(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.YELLOW);
    }

    public static void sendPrivateMessageToSelfImportantInfo(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.GOLD);
    }

    public static void sendPrivateMessageToSelfDebug(String message) {
        sendPrivateMessageToSelfBase(message, Formatting.AQUA);
    }

    public static void sendPrivateMessageToSelfBase(String message, Formatting... formatting) {
        sendPrivateMessageToSelfBase(message, String.join("", Arrays.stream(formatting).map(Formatting::getMCCode).toList()));
    }

    public static void sendPrivateMessageToSelfBase(String message, String formatting) {
        EnvironmentCore.chat.sendClientSideMessage(Message.of(formatting + message.replace("§r", "§r" + formatting)), false);
    }

    public static void sendPrivateMessageToSelfText(Message message) {
        EnvironmentCore.chat.sendClientSideMessage(message);
    }

    public static void sendCommand(String s) {
        BBsentials.sender.addSendTask(s);
    }

    public static void setChatPromtId(String logMessage) {
        String yesClickAction = "/chatprompt ([a-fA-F0-9-]+) YES";
        Pattern yesPattern = Pattern.compile(yesClickAction);
        Matcher yesMatcher = yesPattern.matcher(logMessage);
        String lastPrompt = null;
        if (yesMatcher.find()) {
            lastPrompt = yesMatcher.group(1);
            setChatCommand("/chatprompt " + lastPrompt + " YES", 10);
        }
    }

    /**
     * @param command          the command to be executed
     * @param timeBeforePerish in seconds before its reset to nothing
     */
    public static void setChatCommand(String command, int timeBeforePerish) {
        BBsentials.temporaryConfig.lastChatPromptAnswer = new ChatPrompt(command, timeBeforePerish);
        if (BBsentials.developerConfig.isDevModeEnabled()) {
            Chat.sendPrivateMessageToSelfDebug("set the last prompt action too + \"" + command + "\"");
        }
    }

    /**
     * @param task             the command to be executed
     * @param timeBeforePerish in seconds before its reset to nothing
     */
    public static void setChatCommand(Runnable task, int timeBeforePerish) {
        BBsentials.temporaryConfig.lastChatPromptAnswer = new ChatPrompt(task, timeBeforePerish);
        if (BBsentials.developerConfig.isDevModeEnabled()) {
            Chat.sendPrivateMessageToSelfDebug("set a Chat Prompt Task");
        }
    }

    public static String getFirstGreenSelectOption(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray extraArray = jsonObject.getAsJsonArray("extra");

        for (JsonElement element : extraArray) {
            JsonObject extraObject = element.getAsJsonObject();
            String text = extraObject.get("text").getAsString();

            // Check if the text contains green color code
            if (text.contains("§a")) {
                JsonObject clickEvent = extraObject.getAsJsonObject("clickEvent");
                if (clickEvent != null && "run_command".equals(clickEvent.get("action").getAsString())) {
                    return clickEvent.get("value").getAsString();
                }
            }
        }
        return null;
    }

    public Message onEvent(de.hype.bbsentials.client.common.chat.Message text, boolean actionbar) {
        if (!actionbar && !isSpam(text.getString())) {
            if (BBsentials.developerConfig.isDetailedDevModeEnabled()) {
                System.out.println("got a message: " + text.getJson());
            }
            BBsentials.executionService.execute(() -> processThreaded(text));
            return processNotThreaded(text, actionbar);
        }
        return text; // Return the original message if it is spam
    }

    //Handle in the messages which need to be modified here
    public de.hype.bbsentials.client.common.chat.Message processNotThreaded(de.hype.bbsentials.client.common.chat.Message message, boolean actionbar) {
        if (actionbar && !BBsentials.funConfig.overwriteActionBar.isEmpty()) {
            if (message.getUnformattedString().equals(BBsentials.funConfig.overwriteActionBar.replaceAll("§.", ""))) {
                return message;
            }
            return null;
        }
        if (message.isServerMessage() && BBsentials.partyConfig.hidePartyPreAndSuffix) {
            if (message.getJson().equals("{\"text\":\"-----------------------------------------------------\",\"strikethrough\":true,\"color\":\"blue\"}")) {
                return null;
            }
            if (BBsentials.partyConfig.hidePartyDisconnet() && message.getMessageContent().endsWith("has disconnected, they have 5 minutes to rejoin before they are removed from the party.")) {
                return null;
            }
            if (BBsentials.partyConfig.hidePartyJoinOrLeave() && (message.getMessageContent().endsWith("joined the party.") || message.getMessageContent().endsWith("left the party."))) {
                return null;
            }
        }
        if (actionbar) return message;
        if (message.isFromReportedUser()) {
            sendPrivateMessageToSelfBase("B: " + message.getUnformattedString(), Formatting.RED);
            return null;
        }
        if (BBsentials.generalConfig.doPartyChatCustomMenu && message.isFromParty()) {
            message.replaceInJson("/viewprofile \\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}", StringEscapeUtils.escapeJava("/socialoptions party " + message.getPlayerName() + " " + message.getUnformattedString()));
        } else if (BBsentials.generalConfig.doGuildChatCustomMenu && message.isFromGuild()) {
            message.replaceInJson("/viewprofile \\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}", StringEscapeUtils.escapeJava("/socialoptions guild " + message.getPlayerName() + " " + message.getUnformattedString()));
        } else if (BBsentials.generalConfig.doAllChatCustomMenu) {
            try {
                message.replaceInJson("/socialoptions " + message.getPlayerName(), StringEscapeUtils.escapeJava("/socialoptions sb " + message.getPlayerName() + " " + message.getUnformattedString()));
            } catch (Exception e) {
                Chat.sendPrivateMessageToSelfError("Error with Message: " + message.getUnformattedString());
            }
        }

        return message;
    }

    public void processThreaded(de.hype.bbsentials.client.common.chat.Message message) {
        if (message.getString() != null) {
            String messageUnformatted = message.getUnformattedString();
            String username = message.getPlayerName();
            Islands island = BBsentials.dataStorage.getIsland();
            boolean isBingo = EnvironmentCore.utils.isSelfBingo();
            if (!EnvironmentCore.utils.isWindowFocused()) {
                if (BBsentials.generalConfig.doDesktopNotifications) {
                    if ((messageUnformatted.matches(".*is visiting Your Garden !") || messageUnformatted.endsWith(".*is visiting Your Island !")) && !EnvironmentCore.utils.isWindowFocused() && BBsentials.generalConfig.doDesktopNotifications) {
                        sendNotification("BBsentials Visit-Watcher", messageUnformatted);
                    } else if (message.isMsg()) {
                        sendNotification("BBsentials Message Notifier", username + " sent you the following message: " + message.getMessageContent());
                    } else if (message.isServerMessage() && messageUnformatted.startsWith("HOPPITY'S HUNT")) {
                        sendNotification("BBsentials Hoppity Notifier", messageUnformatted);
                    }
                    boolean matchesNick = message.getMessageContent().toLowerCase().matches("(^|\s)%s($|\s)".formatted(BBsentials.generalConfig.nickname.toLowerCase())) && BBsentials.generalConfig.notifForMessagesType.equalsIgnoreCase("nick");
                    boolean matchesAll = message.getMessageContent().toLowerCase().contains(BBsentials.generalConfig.getUsername().toLowerCase()) || (BBsentials.generalConfig.notifForMessagesType.equalsIgnoreCase("all"));
                    boolean none = BBsentials.generalConfig.notifForMessagesType.equalsIgnoreCase("none");
                    if (!none && (matchesAll || matchesNick)) {
                        if (message.isFromParty())
                            sendNotification("BBsentials Party Chat Notifier", username + " : " + message.getMessageContent());
                        else if (message.source == de.hype.bbsentials.shared.objects.Message.MessageSource.ALL_CHAT)
                            sendNotification("BBsentials Chat Notifier", username + " : " + message.getMessageContent());
                    }
                }
            }
            if (message.isFromReportedUser()) {
                if (BBsentials.developerConfig.devMode) {
                    Chat.sendPrivateMessageToSelfError("B: " + messageUnformatted);
                }
            } else if (message.isServerMessage()) {
                if (messageUnformatted.startsWith("Select an option:")) {
                    setChatCommand(getFirstGreenSelectOption(message.getJson()), 10);
                } else if (messageUnformatted.contains(".*disbanded the party")) {
                    lastPartyDisbandedUsername = message.getNoRanks().split(" ")[0];
                    partyDisbandedMap.put(lastPartyDisbandedUsername, Instant.now());
                } else if (message.contains(".*invited you to join their party")) {
                    username = message.getNoRanks().replace("-", "").replace("\n", "").trim().split(" ")[0];
                    if (lastPartyDisbandedUsername != null && partyDisbandedMap != null) {
                        Instant lastDisbandedInstant = partyDisbandedMap.get(lastPartyDisbandedUsername);
                        if (BBsentials.partyConfig.acceptReparty) {
                            if (lastDisbandedInstant != null && lastDisbandedInstant.isAfter(Instant.now().minusSeconds(20)) && (username.equals(lastPartyDisbandedUsername))) {
                                sendCommand("/p accept " + username);
                            }
                        }
                    }
                    if (!EnvironmentCore.utils.isWindowFocused()) {
                        sendNotification("BBsentials Party Notifier", "You got invited too a party by: " + username);
                    }
                } else if (message.startsWith("Party Members (")) {
                    BBsentials.partyConfig.partyMembers = new ArrayList<>();
                } else if (message.startsWith("Party Moderators:")) {
                    String temp = messageUnformatted.replace("Party Moderators:", "").replace(" ●", "").replaceAll("\\s*\\[[^\\]]+\\]", "").trim();
                    if (temp.contains(",")) {
                        for (int i = 0; i < temp.split(",").length; i++) {
                            BBsentials.partyConfig.partyMembers.add(temp.split(",")[i - 1]);
                        }
                    } else {
                        BBsentials.partyConfig.partyMembers.add(temp);
                    }
                } else if (message.startsWith("Party Members:")) {
                    String temp = messageUnformatted.replace("Party Members:", "").replace(" ●", "").replaceAll("\\s*\\[[^\\]]+\\]", "").trim();
                    if (temp.contains(",")) {
                        for (int i = 0; i < temp.split(",").length; i++) {
                            System.out.println("Added to plist: " + (temp.split(",")[i - 1]));
                            BBsentials.partyConfig.partyMembers.add(temp.split(",")[i - 1]);
                        }
                    } else {
                        BBsentials.partyConfig.partyMembers.add(temp);
                    }
                } else if ((message.startsWith("Party Leader:") && !message.contains(BBsentials.generalConfig.getUsername())) || message.equals("You are not currently in a party.") || (message.contains("warped the party into a Skyblock Dungeon") && !message.startsWith(BBsentials.generalConfig.getUsername()) ||
                        (message.startsWith("The party was transferred to ") && !message.getNoRanks().startsWith("The party was transferred to " + BBsentials.generalConfig.getUsername())))
                        || message.endsWith(BBsentials.generalConfig.getUsername() + " is now a Party Moderator")
                        || (message.startsWith("The party was disbanded")) || (message.startsWith("You have joined ")
                        && message.endsWith("'s party!")) || (message.startsWith("Party Leader, ") && message.contains(" , summoned you to their server."))
                        || (message.contains("warped to your dungeon"))) {
                    BBsentials.partyConfig.isPartyLeader = false;
                    if (BBsentials.developerConfig.isDetailedDevModeEnabled()) {
                        sendPrivateMessageToSelfDebug("Leader: " + BBsentials.partyConfig.isPartyLeader);
                    }
//                    if (HPModAPIPacket.PARTYINFO.complete().getLeader().get().equals(BBsentials.generalConfig.getMCUUID())) {
//                        BBsentials.partyConfig.isPartyLeader = true;
//                    }
                } else if (BBsentials.partyConfig.partyMembers.isEmpty() && messageUnformatted.endsWith("to the party! They have 60 seconds to accept")) {
                    BBsentials.partyConfig.isPartyLeader = true;
                } else if (messageUnformatted.startsWith("You'll be partying with:")) {
                    List<String> members = new ArrayList<>();
                    for (String users : messageUnformatted.replace("You'll be partying with:", "").replaceAll("\\[[^\\]]*\\]", "").trim().split(",")) {
                        if (users.contains("and ")) {
                            break;
                        }
                        members.add(users);
                    }
                    BBsentials.partyConfig.partyMembers = members;
                } else if (((messageUnformatted.startsWith("Party Leader: ") && messageUnformatted.endsWith(BBsentials.generalConfig.getUsername() + " ●")))
                        || (message.contains(BBsentials.generalConfig.getUsername() + " warped the party to a SkyBlock dungeon!")) ||
                        (message.getNoRanks().startsWith("The party was transferred to " + BBsentials.generalConfig.getUsername()))
                        || message.getNoRanks().endsWith(" has promoted " + BBsentials.generalConfig.getUsername() + " to Party Leader") ||
                        (message.contains("warped to your dungeon"))) {
                    ClientboundPartyInfoPacket partyPacket = HPModAPIPacket.PARTYINFO.complete();
                    BBsentials.partyConfig.isPartyLeader = partyPacket.getLeader().get().equals(BBsentials.generalConfig.getMCUUID());
                    if (BBsentials.developerConfig.isDetailedDevModeEnabled()) {
                        sendPrivateMessageToSelfDebug("Leader: " + BBsentials.partyConfig.isPartyLeader);
                    }
//                    if (HPModAPIPacket.PARTYINFO.complete().getLeader().get().equals(BBsentials.generalConfig.getMCUUID())) {
//                        BBsentials.partyConfig.isPartyLeader = true;
//                    }
                } else if (messageUnformatted.startsWith("BUFF! You splashed yourself with")) {
                    if (UpdateListenerManager.splashStatusUpdateListener != null) {
                        UpdateListenerManager.splashStatusUpdateListener.setStatus(StatusConstants.SPLASHING);
                    }
                } else if (messageUnformatted.equals("Click here to purchase a new 6 hour pass for 10,000 Coins")) {
                    Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",\"You can press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to buy it.\"]"));
                    setChatCommand("/purchasecrystallhollowspass", 30);
                } else if (messageUnformatted.equals("You have reached the daily cap of 500,000 Enchanting EXP. Keep in mind EXP from experiments bypasses this cap!")) {
                    EnvironmentCore.utils.playsound("block.anvil.destroy");
                } else if (message.contains("[OPEN MENU]") || message.contains("[YES]")) {
                    setChatPromtId(message.getJson());
                } else if (message.getUnformattedString().endsWith("Return to the Trapper soon to get a new animal to hunt!")) {
                    setChatCommand("/warp trapper", 10);
                    Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"Press (\",\"color\":\"green\"},{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"gold\"},{\"text\":\") to warp back to the trapper\",\"color\":\"green\"}]"));
                } else if (messageUnformatted.startsWith("BINGO GOAL COMPLETE! ")) {
                    BBsentials.connection.sendPacket(new CompletedGoalPacket("", messageUnformatted.replace("BINGO GOAL COMPLETE!", "").trim(), "", "", CompletedGoalPacket.CompletionType.GOAL, -1, BBsentials.visualConfig.broadcastGoalAndCardCompletion));
                } else if (messageUnformatted.matches("You completed all 20 goals for the \\w+ \\d{4} Bingo Event!")) {
                    Chat.sendPrivateMessageToSelfImportantInfo("BB: Detected Card Completion. GG!\nThis will be verified shortly. If you want to get special Roles enable your APIs ASAP");
                    EnvironmentCore.utils.playsound("ui.toast.challenge_complete");
                    BBsentials.connection.sendPacket(new CompletedGoalPacket("", "", "", "", CompletedGoalPacket.CompletionType.CARD, -1, BBsentials.visualConfig.broadcastGoalAndCardCompletion));
                } else if (messageUnformatted.startsWith("Profile ID: ")) {
                    String id = messageUnformatted.replace("Profile ID: ", "").trim();
                    BBsentials.generalConfig.profileIds.add(id);
                    BBsentials.dataStorage.currentProfileID = id;
                } else if (messageUnformatted.startsWith("DUNGEON LEVEL UP The Catacombs")) {
                    String levelString = messageUnformatted.split("→")[1].trim();
                    if (!levelString.matches("\\d+")) {
                        Chat.sendPrivateMessageToSelfError("Error parsing Dungeon Level: " + levelString);
                    } else {
                        int level = Integer.parseInt(levelString);
                        if (level == 1 || level == 3 || level == 5 && isBingo) {
                            BBsentials.sender.addSendTask("/pc Catacombs Level " + level + " reached", 1);
                        }
                    }
                } else if (island == Islands.DUNGEON && isBingo && message.contains("You") && !messageUnformatted.contains("only") && (messageUnformatted.contains("Milestone ❷") || messageUnformatted.contains("Milestone ❸"))) {
                    BBsentials.sender.addSendTask("/pc Catacombs Milestone %s reached".formatted(messageUnformatted.replaceAll("[a-zA-Z\s:/0-9,!]+", "").trim()), 1);
                }
            } else if (message.isFromGuild()) {

            } else if (message.source == de.hype.bbsentials.shared.objects.Message.MessageSource.NPC) {
                if (message.getUnformattedString().endsWith("animal near the Desert Settlement.") || message.getUnformattedString().endsWith("animal near the Oasis.")) {
                    setChatCommand("/warp desert", 10);
                    Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"Press (\",\"color\":\"green\"},{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"gold\"},{\"text\":\") to warp to the \",\"color\":\"green\"},{\"text\":\"Desert Settelment\",\"color\":\"gold\"}]"));
                }
                lastNPCMessage = new Date();
            } else if (message.isFromParty()) {
                if (message.getMessageContent().equalsIgnoreCase("@" + BBsentials.generalConfig.getUsername().toLowerCase() + " bb:dev getlog") && username.equals("Hype_the_Time")) {
                    Chat.sendPrivateMessageToSelfFatal(Formatting.DARK_RED.toString() + Formatting.BOLD + "Don't worry its a" + Formatting.LIGHT_PURPLE + " meme" + Formatting.DARK_RED + Formatting.BOLD + " nothing happens actually. This is to troll Party and it would be irresponsible to send logs without consent.");
                    BBsentials.sender.addSendTask("/pc @Hype_the_Time log packet has been sent ID: " + ((int) (Math.random() * 10000)), 3);
                }
                if ((message.getMessageContent().equals("warp") || message.getMessageContent().equals("!warp")) && BBsentials.partyConfig.isPartyLeader && !message.isFromSelf()) {
                    if (BBsentials.partyConfig.partyMembers.size() == 1) {
                        Chat.sendCommand("/p warp");
                    } else if (BBsentials.partyConfig.partyMembers.size() >= 10) {
                        //ignored because soo many players
                    } else if (BBsentials.partyConfig.partyMembers.size() > 1) {
                        Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"@username\",\"color\":\"red\"},\" \",\"is requesting a warp. Press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to warp the entire \",{\"text\":\"Party\",\"color\":\"gold\"},\".\"]".replace("@username", username)));
                        setChatCommand("/p warp", 10);
                    }
                } else if (BBsentials.partyConfig.isPartyLeader && message.getMessageContent().equals("!ptme") && !message.isFromSelf()) {
                    Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"@username\",\"color\":\"red\"},\" \",\"is requesting a party transfer. Press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to transfer the party to them \",\".\"]".replace("@username", username)));
                    setChatCommand("/p transfer " + username, 10);
                }
                else
                    if (message.getMessageContent().equals("r?") || message.getMessageContent().equals("ready?")) {
                        Message.tellraw("[\"\",{\"text\":\"@username\",\"color\":\"red\"},\" \",\"is requesting a party transfer. Press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to transfer the party to them \",\".\"]".replace("@username", username));
                        setChatCommand("/pc r " + username, 10);
                    }

            } else if (message.isMsg()) {
                String messageContent = message.getMessageContent();
                if (messageContent.startsWith("bb:party") && !message.isFromSelf()) {
                    if (messageContent.startsWith("bb:party me")) {
                        if (BBsentials.partyConfig.allowBBinviteMe) {
                            BBsentials.sender.addSendTask("/p invite " + username, 1);
                            message.replyToUser("Command executed");
                        }
                    } else if (messageContent.startsWith("bb:party list")) {
                        BBsentials.sender.addSendTask("/p list", 1);
                        message.replyToUser("Command executed");
                    } else {
                        ClientboundPartyInfoPacket partyInfoPacket = HPModAPIPacket.PARTYINFO.complete();
                        ClientboundPartyInfoPacket.PartyRole role = partyInfoPacket.getMemberMap().get(BBsentials.generalConfig.getMCUUIDID()).getRole();
                        TrustedPartyMember person = BBsentials.partyConfig.getTrustedUsername(username);
                        if (person == null) {
                            message.replyToUser("Permission Denied");
                        } else {
                            String[] splittedParams = messageContent.replace("bb:party", "").trim().split(" ");
                            String actionParamter = "";
                            String targetName = BBsentials.generalConfig.getUsername();
                            try {
                                actionParamter = splittedParams[0].trim();
                                if (splittedParams.length < 2) {
                                    message.replyToUser("Incorrect arguments");
                                    return;
                                }
                                targetName = splittedParams[1].trim();
                            } catch (Exception ignored) {

                            }
                            if (messageContent.equalsIgnoreCase("bb:party accept")) {
                                if (person.canInvite()) {
                                    BBsentials.sender.addSendTask("/p accept " + username, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("join")) {
                                if (person.partyAdmin()) {
                                    BBsentials.sender.addSendTask("/p join " + targetName, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (!partyInfoPacket.isInParty()) {
                                message.replyToUser("Currently not in a party");
                            } else if (actionParamter.equalsIgnoreCase("leave")) {
                                if (person.partyAdmin()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask("/pc leaving party due too request from %s".formatted(message.getPlayerName()), 1);
                                    BBsentials.sender.addSendTask("/p leave", 1);
                                    message.replyToUser("Invite Sent. (If not received check Party Permissions or All invite may not be enabled.)");
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("invite")) {
                                if (person.canInvite()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName), 1);
                                    BBsentials.sender.addSendTask("/p " + actionParamter + " " + targetName, 1);
                                    message.replyToUser("Invite Sent. (If not received check Party Permissions or All invite may not be enabled.)");
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (role.equals(ClientboundPartyInfoPacket.PartyRole.MEMBER)) {
                                message.replyToUser("I am currently only Party Member → insufficient privileges");
                            } else if (actionParamter.equalsIgnoreCase("mute")) {
                                if (person.canMute()) {
                                    BBsentials.sender.addSendTask("/pc " + username + " muted the party", 1);
                                    BBsentials.sender.addSendTask("/p " + actionParamter, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (role.equals(ClientboundPartyInfoPacket.PartyRole.MOD)) {
                                message.replyToUser("I am currently only Party Member → insufficient privileges");
                            } else if (actionParamter.equalsIgnoreCase("promote")) {
                                if (person.partyAdmin()) {
                                    BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName));
                                    BBsentials.sender.addSendTask("/p " + actionParamter + " " + targetName, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("demote")) {
                                if (person.partyAdmin()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName), 1);
                                    BBsentials.sender.addSendTask("/p " + actionParamter + " " + targetName, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("kickoffline") || (actionParamter.equalsIgnoreCase("kick") && username.equalsIgnoreCase("offline"))) {
                                if (person.canKickOffline()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName), 1);
                                    BBsentials.sender.addSendTask("/p kickoffline", 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("kick")) {
                                if (person.canKick()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName), 1);
                                    BBsentials.sender.addSendTask("/p " + actionParamter + " " + targetName, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("ban")) {
                                if (person.canBan()) {
                                    if (!targetName.equalsIgnoreCase(username)) {
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName));
                                        BBsentials.sender.addSendTask("/p kick " + targetName, 1);
                                        BBsentials.sender.addSendTask("/ignore add " + targetName, 1);
                                    }
                                    message.replyToUser("canceled! you can not ban yourself");
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("stream")) {
                                if (person.partyAdmin()) {
                                    int amount = 24;
                                    try {
                                        amount = Integer.parseInt(targetName);
                                    } catch (Exception ignored) {
                                    }
                                    BBsentials.sender.addSendTask("/stream open " + amount, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("transfer")) {
                                if (person.partyAdmin()) {
                                    if (BBsentials.partyConfig.announceRemoteMsgPartyCommands)
                                        BBsentials.sender.addSendTask(getPartyAnnounceAction(username, actionParamter, targetName), 1);
                                    BBsentials.sender.addSendTask("/p transfer " + targetName, 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("disband")) {
                                if (person.partyAdmin()) {
                                    BBsentials.sender.addSendTask("/pc " + username + " disbanded the party.", 1);
                                    BBsentials.sender.addSendTask("/p disband ", 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("warp")) {
                                if (person.canRequestWarp()) {
                                    BBsentials.sender.addSendTask("/pc " + username + " warped the party. So blame them not me", 1);
                                    BBsentials.sender.addSendTask("/p warp", 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("poll")) {
                                if (person.canRequestWarp()) {
                                    BBsentials.sender.addSendTask("/pc posting poll in name of " + username, 1);
                                    BBsentials.sender.addSendTask("/p poll " + messageContent.replace("bb:party poll", "").trim(), 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else if (actionParamter.equalsIgnoreCase("allinvite")) {
                                if (person.canRequestWarp()) {
                                    BBsentials.sender.addSendTask("/pc " + username + "triggered toggle of All invite", 1);
                                    BBsentials.sender.addSendTask("/p settings allinvite", 1);
                                } else {
                                    message.replyToUser("Insufficient Privileges");
                                }
                            } else {
                                message.replyToUser("Incorrect Arguments");
                            }
                        }
                    }
                }
            }

        }
        BBsentials.discordIntegration.receivedInGameMessage(message);
        if (BBsentials.socketAddonConfig.useSocketAddons) {
            BBsentials.addonManager.notifyAllAddonsReceievedMessage(message);
        }
    }

    public boolean isSpam(String message) {
        if (message == null) return true;
        else if (message.isEmpty()) return true;
        else if (message.contains("Achievement Points")) return true;
        return false;
    }

    public String test() {
        //put test code here
        sendNotification("test", "This is an example which was run of the h:test test");
        return new String();
    }

    public void sendNotification(String title, String text) {
        SystemUtils.sendNotification(title, text);
    }

    public void sendNotification(String title, String text, float volume) {
        BBsentials.executionService.execute(() -> {
            EnvironmentCore.utils.playCustomSound("/sounds/mixkit-sci-fi-confirmation-914.wav", 0);
        });
        List<String> argsList = new ArrayList<>();
        argsList.add("--title");
        argsList.add(title);
        argsList.add("--passivepopup");
        argsList.add(text);
        argsList.add("5");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("kdialog");
            processBuilder.command().addAll(argsList);

            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getPartyAnnounceAction(String username, String actionParamter, String targetName) {
        String updatedActionParamter = new String(actionParamter);
        if (actionParamter.equalsIgnoreCase("transfer")) {
            updatedActionParamter = "transferred the party to";
        } else if (actionParamter.endsWith("e"))
            updatedActionParamter += "d";
        else updatedActionParamter += "ed";
        String updatedTargetName = new String(targetName);
        if (targetName.equalsIgnoreCase(username)) {
            updatedTargetName = "themself";
        }
        return "/pc " + username + " " + updatedActionParamter + " " + updatedTargetName;
    }


}
