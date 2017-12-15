package org.jumpmind.pos.translate;

public interface ILegacyPOSBeanService {

    ILegacyAssignmentSpec getLegacyAssignmentSpec(ILegacyScreen legacyScreen, String panelKey);
    ILegacyBeanSpec getLegacyBeanSpec(ILegacyScreen legacyScreen, String beanSpecName);
    ILegacyButtonSpec getLegacyButtonSpec(ILegacyScreen legacyScreen);
    ILegacyRegisterStatusService getLegacyRegisterStatusService(ILegacyScreen legacyScreen);
    ILegacyUtilityManager getLegacyUtilityManager(ILegacyScreen legacyScreen);
    ILegacyUIModel getLegacyUIModel(ILegacyScreen legacyScreen);
    ILegacyBus getLegacyBus(ILegacyScreen legacyScreen);
    ILegacyPromptAndResponseModel getLegacyPromptAndResponseModel(ILegacyScreen legacyScreen);
    ILegacyPOSBaseBeanModel getLegacyPOSBaseBeanModel(ILegacyScreen legacyScreen);
    ILegacyResourceBundleUtil getLegacyResourceBundleUtil();
    ILegacyLocaleUtilities getLegacyLocaleUtilities();
    ILegacyUIUtilities getLegacyUIUtilities();
    ILegacySummaryTenderMenuBeanModel getLegacySummaryTenderMenuBeanModel(ILegacyScreen legacyScreen);
    ILegacyStatusBeanModel getLegacyStatusBeanModel(ILegacyScreen legacyScreen);
}
