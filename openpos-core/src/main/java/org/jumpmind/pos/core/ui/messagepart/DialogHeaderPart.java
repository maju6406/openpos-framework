package org.jumpmind.pos.core.ui.messagepart;

import org.jumpmind.pos.core.screen.ActionItem;
import org.jumpmind.pos.core.ui.IHasBackButton;

import java.io.Serializable;

public class DialogHeaderPart implements IHasBackButton, Serializable {
    private static final long serialVersionUID = 1L;

    private String headerText;
    private String headerIcon;
    private ActionItem backButton;

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public String getHeaderIcon() {
        return headerIcon;
    }

    public void setHeaderIcon(String headerIcon) {
        this.headerIcon = headerIcon;
    }

    public ActionItem getBackButton() {
        return backButton;
    }

    public void setBackButton(ActionItem backButton) {
        this.backButton = backButton;
    }
}
