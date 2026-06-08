package dev.zm.rankup.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class MenuItem {

    private final ItemStack itemStack;
    private final Consumer<InventoryClickEvent> action;

    public MenuItem(ItemStack itemStack) {
        this(itemStack, null);
    }

    public MenuItem(ItemStack itemStack, Consumer<InventoryClickEvent> action) {
        this.itemStack = itemStack;
        this.action = action;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Consumer<InventoryClickEvent> getAction() {
        return action;
    }

    public void onClick(InventoryClickEvent event) {
        if (action != null) {
            action.accept(event);
        }
    }
}
