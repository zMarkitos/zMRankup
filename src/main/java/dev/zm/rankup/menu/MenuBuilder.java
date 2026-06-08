package dev.zm.rankup.menu;

import dev.zm.rankup.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MenuBuilder implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, MenuItem> items = new HashMap<>();
    private final int rows;

    public MenuBuilder(String title, int rows) {
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, ColorUtil.parse(title));
    }

    public MenuBuilder setItem(int slot, MenuItem item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            items.put(slot, item);
            inventory.setItem(slot, item.getItemStack());
        }
        return this;
    }

    public void fill(MenuItem item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!items.containsKey(i)) {
                setItem(i, item);
            }
        }
    }

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    public int getRows() {
        return rows;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
