package com.youssgm3o8.rokidragon.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;

/**
 * Handles form responses for the dragon management GUI.
 */
public class FormResponseListener implements Listener {
    private final DragonPlugin plugin;
    
    public FormResponseListener(DragonPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        int formId = event.getFormID();
        
        // Skip if player closed the form
        if (event.wasClosed()) {
            if (formId == FormBasedDragonGUI.FORM_FIRST_NAMING) {
                plugin.getLogger().info("[Form Debug] Player " + player.getName() + " closed the naming form without responding");
                player.sendMessage("§eYou need to name your dragon before summoning it. Try using summoning it again.");
            }
            return;
        }
        
        // Check if this form ID belongs to our plugin
        // This check is critical to prevent our plugin from interfering with forms from other plugins
        // We only process form responses where the ID matches one of our predefined form IDs
        if (!isRokiDragonForm(formId)) {
            return;
        }
        
        try {
            // For debugging naming form issues
            if (formId == FormBasedDragonGUI.FORM_FIRST_NAMING) {
                plugin.getLogger().info("[Form Debug] Processing naming form response from " + player.getName() + 
                                      ". Window type: " + event.getWindow().getClass().getSimpleName());
            }
            
            // Determine form type and process
            if (event.getWindow() instanceof FormWindowSimple) {
                FormWindowSimple window = (FormWindowSimple) event.getWindow();
                plugin.getDragonGUI().handleFormResponse(player, formId, window.getResponse().getClickedButtonId());
            } 
            else if (event.getWindow() instanceof FormWindowModal) {
                FormWindowModal window = (FormWindowModal) event.getWindow();
                plugin.getDragonGUI().handleFormResponse(player, formId, window.getResponse().getClickedButtonId() == 0);
            } 
            else if (event.getWindow() instanceof FormWindowCustom) {
                FormWindowCustom window = (FormWindowCustom) event.getWindow();
                plugin.getDragonGUI().handleFormResponse(player, formId, window.getResponse());
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error handling form response: " + e.getMessage(), e);
            if (formId == FormBasedDragonGUI.FORM_FIRST_NAMING) {
                plugin.getLogger().error("[Form Debug] Error in naming form for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the form ID belongs to RokiDragon plugin
     * @param formId The form ID to check
     * @return true if this is one of our forms
     */
    private boolean isRokiDragonForm(int formId) {
        // Check against all our precomputed form IDs
        return formId == FormBasedDragonGUI.FORM_MAIN_MENU ||
               formId == FormBasedDragonGUI.FORM_CUSTOMIZATION ||
               formId == FormBasedDragonGUI.FORM_STORAGE ||
               formId == FormBasedDragonGUI.FORM_BUY_EGG ||
               formId == FormBasedDragonGUI.FORM_LOST_EGGS ||
               formId == FormBasedDragonGUI.FORM_LOST_EGG_CONFIRM ||
               formId == FormBasedDragonGUI.FORM_HELP_MENU ||
               formId == FormBasedDragonGUI.FORM_INFO ||
               formId == FormBasedDragonGUI.FORM_RECIPES ||
               formId == FormBasedDragonGUI.FORM_CUSTOMIZATION_RESULT ||
               formId == FormBasedDragonGUI.FORM_CUSTOMIZATION_ERROR ||
               formId == FormBasedDragonGUI.FORM_PURCHASE_RESULT ||
               formId == FormBasedDragonGUI.FORM_PURCHASE_ERROR ||
               formId == FormBasedDragonGUI.FORM_INVENTORY_STORAGE ||
               formId == FormBasedDragonGUI.FORM_FIRST_NAMING;
    }
} 