package dev.zm.rankup.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MenuBuilder builder) {
            event.setCancelled(true);
            
            if (event.getClickedInventory().getHolder() == holder) {
                MenuItem item = builder.getItem(event.getSlot());
                if (item != null) {
                    item.onClick(event);
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuBuilder) {
            event.setCancelled(true);
        }
    }
}
