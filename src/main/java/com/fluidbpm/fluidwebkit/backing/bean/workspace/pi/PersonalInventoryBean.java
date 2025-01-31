/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2020] Koekiebox (Pty) Ltd
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of Koekiebox and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to Koekiebox
 * and its suppliers and may be covered by South African and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly
 * forbidden unless prior written permission is obtained from Koekiebox Innovations.
 */

package com.fluidbpm.fluidwebkit.backing.bean.workspace.pi;

import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.IConversationCallback;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitPersonalInvBean")
public class PersonalInventoryBean extends ABaseWorkspaceBean<PersonalInventoryItemVO, ContentViewPI>
implements IConversationCallback {

    @Inject
    private PerformanceBean performanceBean;

    @Inject
    private WebKitViewContentModelBean webKitViewContentModelBean;

    @Inject
    private WebKitAccessBean accessBean;

    @Inject
    private WebKitWorkspaceLookAndFeelBean lookAndFeelBean;

    @Getter
    @Setter
    private String formsListGroupName;

    @Getter
    @Setter
    private List<WebKitForm> formsListForGroup;

    @Override
    @PostConstruct
    public void actionPopulateInit() {
        //Do nothing...
    }

    @Override
    public void actionOpenForm(WorkspaceFluidItem workspaceFluidItem) {
        this.openFormBean.actionFreshLoadFormAndSet(workspaceFluidItem);
    }

    @Override
    public void actionOpenMainPage() {
        this.contentView = this.actionOpenMainPage(null, null);
    }

    public String actionOpenMainPageNonAjax() {
        if (this.openFormBean != null) {
            if (this.openFormBean.getContextMenuModel() != null) {
                this.openFormBean.getContextMenuModel().getElements().clear();
            }
            this.openFormBean.setAreaToUpdateAfterSave(null);
            this.openFormBean.endConversation();
        }
        // actionPreRenderProcess will be executed with [freshLookup] param.
        return this.getDestinationNavigationForCancel();
    }

    @Override
    public String getDestinationNavigationForCancel() {
        return "pi";
    }

    @Override
    public void actionPreRenderProcess() {
        String freshLookup = this.getStringRequestParam(RequestParam.FRESH_LOOKUP);
        if (UtilGlobal.isNotBlank(freshLookup)) {
            this.currentlyHaveItemOpen = false;
            this.currentOpenFormTitle = null;
            this.dialogDisplay = false;
        }
        this.actionOpenMainPage();
    }

    @Override
    protected ContentViewPI actionOpenMainPage(
        WebKitViewGroup webKitGroup,
        WebKitViewSub selectedSub
    ) {
        try {
            if (this.getFluidClientDS() == null) return null;

            PersonalInventoryClient persInvClient = this.getFluidClientDS().getPersonalInventoryClient();
            List<FluidItem> piItems = persInvClient.getPersonalInventoryItems();
            List<WorkspaceFluidItem> wsFldItms = new ArrayList<>();
            piItems.forEach(flItm -> {
                wsFldItms.add(new WorkspaceFluidItem(this.createABaseWebVO(webKitGroup, selectedSub, null, flItm)));
            });

            Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data = new HashMap<>();
            Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>> wsFluidItmsMap = new HashMap<>();
            wsFluidItmsMap.put(new WebKitWorkspaceJobView(new JobView(ContentViewPI.PI)), wsFldItms);
            data.put(new WebKitViewSub(), wsFluidItmsMap);

            ContentViewPI contentViewPI = new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
            contentViewPI.refreshData(data);
            return contentViewPI;
        } catch (Exception fce) {
            if (fce instanceof FluidClientException) {
                FluidClientException casted = (FluidClientException)fce;
                if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
                    return new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
                }
            }
            this.raiseError(fce);
            return new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
        }
    }

    @Override
    protected PersonalInventoryItemVO createABaseWebVO(
            WebKitViewGroup group,
            WebKitViewSub sub,
            WebKitWorkspaceJobView view,
            FluidItem item
    ) {
        PersonalInventoryItemVO returnVal = new PersonalInventoryItemVO(item);
        return returnVal;
    }

    /**
     * Open an 'Form' for editing or viewing.
     * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
     *
     * @see #actionOpenForm(WorkspaceFluidItem)
     */
    public void actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem wfItem) {
        this.setAreaToUpdateForDialogAfterSubmit(null);
        this.currentOpenFormTitle = null;
        this.currentlyHaveItemOpen = false;
        // Dialog or Workspace for form layout;
        this.dialogDisplay = !this.isOpenItemInWorkspace(wfItem);

        try {
            this.openFormBean.startConversation();
            this.openFormBean.setAreaToUpdateAfterSave(":frmPIContent");
            this.openFormBean.setConversationCallback(this);
            this.actionOpenForm(wfItem);

            if (this.dialogDisplay) {
                // Now open:
                this.executeJavaScript("PF('varFormDialog').show();");
            } else {
                // Only set the title if not dialog display:
                this.currentOpenFormTitle = wfItem.getFluidItemTitle();
                this.openFormBean.setAreaToUpdateAfterSave(":panelBreadcrumb: :panelWorkspace");
            }
            this.currentlyHaveItemOpen = true;
        } catch (Exception except) {
            this.raiseError(except);
        }
    }

    @Override
    public void afterSaveProcessing(WorkspaceFluidItem workspaceItemSaved) {
        this.dialogDisplay = false;
        this.currentlyHaveItemOpen = false;
        this.currentOpenFormTitle = null;
        this.actionOpenMainPage();
    }

    @Override
    public void afterSendOnProcessing(WorkspaceFluidItem workspaceItemSaved) {
        this.afterSaveProcessing(workspaceItemSaved);
    }

    @Override
    public void closeFormDialog() {
        this.actionCloseOpenForm();
    }

    public void actionRemoveSelectedItemsFromPI() {
        try {
            if (this.getContentView().getFluidItemsSelectedList() == null) return;

            final PersonalInventoryClient piClient = this.getFluidClientDS().getPersonalInventoryClient();
            List<Long> idsToRemove = new ArrayList<>();
            this.getContentView().getFluidItemsSelectedList().forEach(itm -> {
                Form form = itm.getFluidItemForm();
                try {
                    piClient.removeFromPersonalInventory(form);
                    idsToRemove.add(form.getId());
                } catch (Exception err) {
                    this.raiseError(err);
                }
            });
            int numberOfSelected = this.getContentView().getFluidItemsSelectedList().size();
            this.getContentView().getFluidItemsSelectedList().clear();

            List<WorkspaceFluidItem> piItems =
                    this.getContentView().getWorkspaceFluidItemsForSection(ContentViewPI.PI);
            if (piItems != null && !idsToRemove.isEmpty()) {
                idsToRemove.forEach(formId -> {
                    piItems.stream()
                            .filter(workFluidItm -> formId.equals(workFluidItm.getFluidItemFormId()))
                            .findFirst().ifPresent(piItems::remove);
                });
            }

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(String.format("%d Removed.", numberOfSelected)));
        } catch (Exception fce) {
            this.raiseError(fce);
        }
    }

    public void actionRemoveAllItemsFromPI() {
        try {
            List<WorkspaceFluidItem> piItems =
                    this.getContentView().getWorkspaceFluidItemsForSection(ContentViewPI.PI);
            if (piItems == null) return;

            int numberOfSelected = piItems.size();
            final PersonalInventoryClient piClient = this.getFluidClientDS().getPersonalInventoryClient();
            piClient.clearPersonalInventoryItems();
            piItems.clear();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(String.format("%d Removed.", numberOfSelected)));
        } catch (Exception fce) {
            this.raiseError(fce);
        }
    }

    public void actionRemoveItemFromPI(WorkspaceFluidItem toRemove) {
        try {
            if (toRemove == null) return;
            final PersonalInventoryClient piClient = this.getFluidClientDS().getPersonalInventoryClient();
            piClient.removeFromPersonalInventory(toRemove.getFluidItemForm());

            List<WorkspaceFluidItem> piItems =
                    this.getContentView().getWorkspaceFluidItemsForSection(ContentViewPI.PI);
            piItems.remove(toRemove);

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(String.format("'%s' Removed.",
                    toRemove.getFluidItemTitle())));
        } catch (Exception fce) {
            this.raiseError(fce);
        }
    }

    public int getNumberOfPersonalInventoryItems() {
        return performanceBean.getUserStatsReport().getPiCount();
    }

    public int getNumberOfPersonalInventoryLockedItems() {
        return performanceBean.getUserStatsReport().getPiLockedCount();
    }

    /**
     * Prepare to create a new instance of a form.
     * @return initiated {@code WorkspaceFluidItem}
     */
    public WorkspaceFluidItem actionPrepToCreateNewInstanceOf() {
        this.setAreaToUpdateForDialogAfterSubmit(null);
        this.currentlyHaveItemOpen = false;
        this.setDialogHeaderTitle(null);

        Long formIdType = this.getLongRequestParam(RequestParam.FORM_TYPE_ID);
        String formType = this.getStringRequestParam(RequestParam.FORM_TYPE);

        if (this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted() == null) return null;

        Form formDefWithNewInstanceAccess =
                this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted().stream()
                .filter(itm -> itm.getFormType().equals(formType) ||
                                (itm.getFormTypeId() != null && itm.getFormTypeId().equals(formIdType)))
                .findFirst()
                .orElse(null);
        if (formDefWithNewInstanceAccess == null) {
            FacesMessage fMsg = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed.", String.format(
                            "You do not have access to '%s'.", formType));
            FacesContext.getCurrentInstance().addMessage(null, fMsg);
            return null;
        }

        FluidItem newFluidItem = new FluidItem(new Form(formDefWithNewInstanceAccess.getFormType()));
        newFluidItem.getForm().setFormTypeId(formDefWithNewInstanceAccess.getFormTypeId());
        WorkspaceFluidItem newItem = new WorkspaceFluidItem(new PersonalInventoryItemVO(newFluidItem));
        try {
            this.openFormBean.startConversation();
            this.actionOpenForm(newItem);
            this.currentlyHaveItemOpen = true;
            return this.openFormBean.getWsFluidItem();
        } catch (Exception except) {
            this.raiseError(except);
            return null;
        }
    }

    /**
     * List all the items for a group.
     */
    public void actionPrepToListItemsForGroupToCreateNewInstanceOf() {
        this.formsListForGroup = new ArrayList<>();
        this.formsListGroupName = this.getStringRequestParam(RequestParam.GROUP_NAME);
        if (UtilGlobal.isBlank(this.formsListGroupName)) return;

        try {
            this.formsListForGroup = this.lookAndFeelBean.getFormDefinitionsCanCreateInstanceOfGrouped().get(
                    this.formsListGroupName
            );
        } catch (Exception except) {
            this.raiseError(except);
        }
    }
}
