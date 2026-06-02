package com.eurowall.gtfactoryplanner.data;

import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Item identity: Item instance + metadata. NBT is deliberately ignored — GT machine
 * recipes do not meaningfully discriminate on NBT for planning purposes.
 *
 * TODO: OreDictionary unification (e.g. all copper ingots) so alternative inputs
 * collapse into one node. For now keys are exact item+meta.
 */
public final class ItemKey implements ResourceKey {

    public final Item item;
    public final int meta;

    private ItemKey(Item item, int meta) {
        this.item = item;
        this.meta = meta;
    }

    public static ItemKey of(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getItemDamage());
    }

    public ItemStack toStack(int amount) {
        return new ItemStack(item, amount, meta);
    }

    @Override
    public String displayName() {
        try {
            return toStack(1).getDisplayName();
        } catch (Exception e) {
            // Some meta-items misbehave when asked for names outside their valid range.
            return item.getUnlocalizedName() + ":" + meta;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey)) return false;
        ItemKey other = (ItemKey) o;
        return item == other.item && meta == other.meta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(item), meta);
    }

    @Override
    public String toString() {
        return "item:" + Item.itemRegistry.getNameForObject(item) + "@" + meta;
    }
}
