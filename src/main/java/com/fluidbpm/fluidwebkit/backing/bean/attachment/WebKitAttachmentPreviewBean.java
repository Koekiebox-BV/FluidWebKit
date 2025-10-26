/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
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

package com.fluidbpm.fluidwebkit.backing.bean.attachment;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet.Cell;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet.Row;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet.Worksheet;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import lombok.Getter;
import lombok.Setter;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@SessionScoped
@Named("webKitAttachmentPreviewBean")
@Getter
@Setter
public class WebKitAttachmentPreviewBean extends ABaseManagedBean {
	@Inject
	private WebKitAttachmentBean attachmentBean;

	//TODO @Inject
	//private Conversation conversation;

	private List<Attachment> allAttachmentsForForm;
	private Attachment currentViewedAttachmentForForm;
	private WorkspaceFluidItem workspaceFluidItem;

	private int currentAttachmentIndex;
	private int dialogWidth;

	private List<Worksheet> worksheets;

	public void actionLoadAttachmentsForForm(WorkspaceFluidItem fluidItem, int dialogWidth) {
		/*TODO if (this.conversation.isTransient()) {
			this.conversation.setTimeout(TimeUnit.MINUTES.toMillis(180));
			this.conversation.begin();
		}*/

		this.setDialogWidth(dialogWidth);
		this.setWorkspaceFluidItem(fluidItem);
		this.setDialogHeaderTitle("[Unknown]");

		try {
			this.setAllAttachmentsForForm(this.attachmentBean.actionFetchAttachmentsForForm(fluidItem.getFluidItemForm()));
		} catch (Exception except) {
			this.raiseError(except);
		} finally {
			this.setCurrentAttachmentIndex(0);

			if (this.getAllAttachmentsForForm() == null) this.setAllAttachmentsForForm(new ArrayList<>());

			if (this.getAllAttachmentsForForm().isEmpty()) this.setCurrentViewedAttachmentForForm(new Attachment());
			else this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(0));

			this.setDialogHeaderTitle(String.format("%s - %d Total",
					fluidItem.getFluidItemForm().getTitle(),
					this.getAllAttachmentsForForm().size()));
		}
	}

	public void actionLoadSheetsForExcelAttachment(long attIdParam) {
		this.setWorksheets(new ArrayList<>());
		if (attIdParam > 0) {
			this.setWorksheets(this.attachmentBean.fetchExcelWorksheets(attIdParam, null));
		}
	}

	public Row actionGetRowAtIndex(int sheetIndexParam, int rowIndexParam) {
		Worksheet worksheet = this.actionGetWorksheetAtIndex(sheetIndexParam);
		if (worksheet == null) return null;

		return worksheet.getRows().get(rowIndexParam);
	}
	
	public int actionGetColumnWidth(int sheetIndexParam, int columnIndexParam) {
		Worksheet worksheet = this.actionGetWorksheetAtIndex(sheetIndexParam);
		if(worksheet == null) return 5;

		List<Row> rows = worksheet.getRows();
		if(rows == null) return 10;

		int maxCharsInColumn = 0;
		for(Row row : rows) {
			List<Cell> cells = row.getCells();
			if (cells != null && (columnIndexParam < cells.size())) {
				String cellValue = cells.get(columnIndexParam).getValue();
				if(cellValue == null) continue;

				maxCharsInColumn = Math.max(maxCharsInColumn,cellValue.length());
			}
		}

		if (maxCharsInColumn > 0) {
			return 100;
		} else {
			return 10;
		}
	}

	public Worksheet actionGetWorksheetAtIndex(int sheetIndexParam) {
		if (this.getWorksheets() == null) return null;

		if (sheetIndexParam < this.getWorksheets().size()) return this.getWorksheets().get(sheetIndexParam);

		return null;
	}

	public void actionNextAttachment() {
		this.setCurrentAttachmentIndex(this.getCurrentAttachmentIndex() + 1);
		this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(this.getCurrentAttachmentIndex()));
	}

	public void actionPreviousAttachment() {
		this.setCurrentAttachmentIndex(this.getCurrentAttachmentIndex() - 1);
		this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(this.getCurrentAttachmentIndex()));
	}

	public void actionLoadThumbnailClicked(int index) {
		int intReq = this.getIntRequestParam("attIndexToLoad");

		this.setCurrentAttachmentIndex(intReq);
		this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(this.getCurrentAttachmentIndex()));
	}

	public void actionHandleIndexChange() {
		int currentIndex = this.getCurrentAttachmentIndex();

		this.setCurrentAttachmentIndex(currentIndex);
		this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(this.getCurrentAttachmentIndex()));
	}

	public boolean isAtFirstAttachment() {
		return this.currentAttachmentIndex == 0;
	}

	public boolean isAtLastAttachment() {
		if (this.getAllAttachmentsForForm() == null || this.getAllAttachmentsForForm().isEmpty()) return true;
		return (this.currentAttachmentIndex + 1) == this.getAllAttachmentsForForm().size();
	}

	public void actionCloseDialog() {
		//TODO if (!this.conversation.isTransient()) this.conversation.end();
	}

	public List<SelectItem> getAttachmentIndexesAsSelectItems() {
		List<SelectItem> returnVal = new ArrayList<>();
		if (this.getAllAttachmentsForForm() != null) {
			AtomicInteger atInt = new AtomicInteger(0);
			this.getAllAttachmentsForForm().forEach(itm -> {
				int pageNumber = atInt.getAndIncrement();
				returnVal.add(new SelectItem(Integer.valueOf(pageNumber), Integer.toString(pageNumber + 1)));
			});
		}
		return returnVal;
	}
}
