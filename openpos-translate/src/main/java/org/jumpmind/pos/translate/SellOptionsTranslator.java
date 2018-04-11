package org.jumpmind.pos.translate;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jumpmind.pos.core.flow.Action;
import org.jumpmind.pos.core.model.Form;
import org.jumpmind.pos.core.screen.MenuItem;
import org.jumpmind.pos.core.screen.ScreenType;
import org.jumpmind.pos.core.screen.SellScreen;
import org.jumpmind.pos.core.template.SellTemplate;

public class SellOptionsTranslator extends AbstractLegacyScreenTranslator<SellScreen> {

    Set<String> excludeLabelTags = new HashSet<>();
    
    InteractionMacro undoMacro;

    @Deprecated
    public SellOptionsTranslator(ILegacyScreen headlessScreen, String icon, String appId, Properties properties, String... excludeActions) {
        super(headlessScreen, SellScreen.class, appId, properties);
        if (excludeActions != null) {
            for (String string : excludeActions) {
                this.excludeLabelTags.add(string);
            }
        }
        screen.setType(ScreenType.Options);
        screen.setIcon(icon);        
    }
    
    public SellOptionsTranslator(ILegacyScreen headlessScreen, String icon, String... excludeActions) {
    	this(headlessScreen, icon, null, null, excludeActions);
    }
    
    public void setUndoMacro(InteractionMacro undoMacro) {
        this.undoMacro = undoMacro;
    }

    @Override
    protected void buildMainContent() {
        super.buildMainContent();
        screen.setTemplate(new SellTemplate());
        screen.setLocalMenuItems(generateUIActionsForLocalNavButtons(MenuItem.class, true, excludeLabelTags.toArray(new String[]{})));
        if (screen.getLocalMenuItems().size() > 0) {
            screen.setPrompt("Choose Option");
            screen.setInstructions("Please select an option from the menu to the right");
        }
    }
    
    @Override
    public void handleAction(ITranslationManagerSubscriber subscriber, TranslationManagerServer tmServer, Action action,
            Form formResults) {
        if ("Undo".equals(action.getName()) && undoMacro != null) {
            tmServer.executeMacro(undoMacro);
        } else {
            super.handleAction(subscriber, tmServer, action, formResults);
        }
    }
}
