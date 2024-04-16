package de.hype.bbsentials.fabric.mixins;

import de.hype.bbsentials.client.common.api.Formatting;
import de.hype.bbsentials.client.common.client.BBsentials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.*;

public abstract class RenderingDefinitions {
    /**
     * @param stack Use this stack for matching conditions based on your needs. However do not edit it since the data is copied. This is due too some things kicking you otherwise.
     * @param texts the Custom item tooltip.
     * @return the tooltip you want to be displayed when hovering the item
     */
    public static Map<Integer, RenderingDefinitions> defsBlocking = new HashMap<>();
    public static Map<Integer, RenderingDefinitions> defsNonBlocking = new HashMap<>();

    private static Integer renderDefIdCounter = 0;
    public final Integer renderDefId = renderDefIdCounter++;

    public RenderingDefinitions() {
        defsBlocking.put(renderDefId, this);
    }

    /**
     * @param blocking is the information youre modifying final or may it be process from something else as well?
     */
    public RenderingDefinitions(boolean blocking) {
        if (blocking) defsBlocking.put(renderDefId, this);
        else defsNonBlocking.put(renderDefId, this);
    }

    public static void clearAndInitDefaults() {
        defsBlocking.clear();
        defsNonBlocking.clear();
        new RenderingDefinitions(false) {
            @Override
            public boolean modifyItem(ItemStack stack, RenderStackItemCheck check, String itemName) {
                if (!BBsentials.developerConfig.hypixelItemInfo) return false;
                try {
                    NbtCompound nbt = stack.getOrCreateNbt();
                    NbtCompound extraAttributes = nbt.getCompound("ExtraAttributes");
                    List<Text> itemTooltip = check.getTextTooltip();
                    Set<String> keys = extraAttributes.getKeys();
                    for (String key : keys) {
                        if (key.equals("enchantments")) continue;
                        if (key.equals("timestamp")) {
                            Long stamp = extraAttributes.getLong(key);
                            itemTooltip.add(Text.of("timestamp(Creation Date): " + stamp + "(" + Instant.ofEpochMilli(stamp) + ")"));
                            continue;
                        }
                        itemTooltip.add(Text.of(key + ": " + extraAttributes.get(key)));
                    }
                    stack.getNbt().putBoolean("addedDebug", true);
                } catch (NullPointerException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        };

        new RenderingDefinitions() {
            @Override
            public boolean modifyItem(ItemStack stack, RenderStackItemCheck check, String itemName) {
                if (BBsentials.splashConfig.showSmallestHub && (BBsentials.splashConfig.smallestHubName != null)) {
                    if (itemName.equals(BBsentials.splashConfig.smallestHubName)) {
                        check.setTexturePath("bbsentials:customitems/low_player_hub");
                    }
                }
                return true;
            }
        };

        new RenderingDefinitions() {
            @Override
            public boolean modifyItem(ItemStack stacks, RenderStackItemCheck check, String itemName) {
                Item stackItem = stacks.getItem();
                if (BBsentials.visualConfig.showContributorPositionInCount && (stackItem == Items.EMERALD_BLOCK || stackItem == Items.IRON_BLOCK)) {
                    List<Text> text = check.getTextTooltip();
                    if (text.size() >= 20) {
                        String line = text.get(2).getString();
                        if (!line.equals("Community Goal")) return false;
                        String[] temp = itemName.split(" ");
                        String tierString = temp[temp.length - 1];
                        switch (tierString) {
                            case "V", "5" -> check.renderAsItem(Items.NETHERITE_BLOCK);
                            case "IV", "4" -> check.renderAsItem(Items.DIAMOND_BLOCK);
                            case "III", "3" -> check.renderAsItem(Items.GOLD_BLOCK);
                            case "II", "2" -> check.renderAsItem(Items.IRON_BLOCK);
                            case "I", "1" -> check.renderAsItem(Items.COPPER_BLOCK);
                            default -> check.renderAsItem(Items.REDSTONE_BLOCK);
                        }
                        Integer position = null;
                        Double topPos = null;
                        for (int i = 20; i < text.size(); i++) {

                            line = text.get(i).getString();
                            if (line.contains("contributor")) {
                                position = Integer.parseInt(line.replaceAll("\\D", ""));
                            }
                            if (line.contains("Top")) {
                                topPos = Double.parseDouble(line.replaceAll("[^0-9.]", ""));
                            }
                        }
                        if (topPos != null) {
                            if (position != null) {
                                //Display Position not %
                                if (position == 1) {
                                    check.setItemCount(Formatting.YELLOW + "#1");
                                }
                                else if (position == 2) {
                                    check.setItemCount(Formatting.WHITE + "#2");
                                }
                                else if (position == 3) {
                                    check.setItemCount(Formatting.GOLD + "#3");
                                }
                                else {
                                    check.setItemCount(Formatting.GRAY + "#" + position);
                                }
                            }
                            else {
                                //Display Top %
                                if (topPos <= 1) {
                                    check.setItemCount(Formatting.GREEN + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                                else if (topPos <= 5) {
                                    check.setItemCount(Formatting.GOLD + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                                else if (topPos <= 10) {
                                    check.setItemCount(Formatting.YELLOW + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                                else if (topPos <= 25) {
                                    check.setItemCount(Formatting.RED + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                                else {
                                    check.setItemCount(Formatting.DARK_RED + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                            }
                        }
                    }
                }

                return true;
            }
        };
        new RenderingDefinitions(false) {
            @Override
            public boolean modifyItem(ItemStack stack, RenderStackItemCheck check, String itemName) {
                if (BBsentials.funConfig.hub29Troll) {
                    if (itemName.startsWith("SkyBlock Hub #")) {
                        stack.setCustomName(Text.translatable("§aSkyBlock Hub #29 (" + itemName.replaceAll("\\D", "") + ")"));
                    }
                }
                else if (BBsentials.funConfig.hub17To29Troll) {
                    if (itemName.equals("SkyBlock Hub #17")) {
                        stack.setCustomName(Text.translatable("§aSkyBlock Hub #29"));
                    }
                }
                return false;
            }
        };
    }

    public boolean isRegistered() {
        return defsBlocking.get(renderDefId) != null || defsNonBlocking.get(renderDefId) != null;
    }

    public void removeFromPool() {
        defsBlocking.remove(renderDefId);
        defsNonBlocking.remove(renderDefId);
    }

    public Runnable getSelfRemove() {
        return this::removeFromPool;
    }

    /**
     * Try to filter out non matching items out as soon as you can before you do the intensive stuff since it takes more performance otherwise!
     *
     * @return return value defines whether you want to stop after the check
     */
    public abstract boolean modifyItem(ItemStack stack, RenderStackItemCheck check, String itemName);

    public static class RenderStackItemCheck {
        private final ItemStack stack;
        private String texturePath = null;
        private String itemCount = null;
        private List<Text> texts = null;
        private List<Text> itemLore = null;

        public RenderStackItemCheck(ItemStack stack) {
            this.stack = stack;
            String itemName = stack.getName().getString();
            for (RenderingDefinitions def : defsNonBlocking.values()) {
                def.modifyItem(stack, this, itemName);
            }
            for (RenderingDefinitions def : defsBlocking.values()) {
                if (def.modifyItem(stack, this, itemName)) {
                    return;
                }
            }
        }

        public List<Text> getTextTooltip() {
            if (texts != null) return texts;
            NbtCompound b = stack.getNbt();
            if (b == null) return new ArrayList<>();
            NbtCompound b1 = b.getCompound("display");
            if (b1 == null) return new ArrayList<>();
            NbtList list = b1.getList("Lore", NbtElement.STRING_TYPE);
            List<Text> text = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Text.Serialization.fromJson(list.get(i).asString());
            }
            texts = text;
            return text;
        }

        public List<Text> getItemLore() {
            if (itemLore != null) return itemLore;
            itemLore = new ArrayList<>();
            NbtCompound b = stack.getNbt();
            if (b == null) return itemLore;
            NbtCompound b1 = b.getCompound("display");
            if (b1 == null) return itemLore;
            NbtList list = b1.getList("Lore", NbtElement.STRING_TYPE);
            if (list == null) return itemLore;
            itemLore = new ArrayList<>(list.stream().map(t -> Text.Serialization.fromJson(t.asString())).toList());
            return itemLore;
        }

        public List<Text> getTextCopy() {
            List<Text> text = getTextTooltip();
            return new ArrayList<>(text);
        }

        public String getItemCount() {
            return itemCount;
        }

        public void setItemCount(String itemCount) {
            this.itemCount = itemCount;
        }

        public void setItemcount(int value) {
            this.itemCount = String.valueOf(value);
        }

        public String getTexturePath() {
            return texturePath;
        }

        public void setTexturePath(String texturePath) {
            this.texturePath = texturePath;
        }

        public void setTexts(List<Text> texts) {
            this.texts = texts;
        }

        public void renderAsItem(Item item) {
            texturePath = Registries.ITEM.getId(item).getPath();
        }

        public Text getItemStackName() {
            return getTextTooltip().get(0);
        }

        public void setItemStackName(Text itemStackName) {
            getTextTooltip().set(0, itemStackName);
        }
    }
}