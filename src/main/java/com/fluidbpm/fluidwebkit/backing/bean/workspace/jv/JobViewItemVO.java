package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class JobViewItemVO extends ABaseWebVO {
	private String title;
	private String step;
	private String state;
	private String type;
	private String currentUser;
	private Long fluidItemId;
	private Long formContainerId;
	private Date dateLastUpdated;
	private Date stepEnteredTime;
	private Date dateCreated;

	private transient Map<String, ForegroundBackground> groupingColors;

	public static final String COMPLETED_NOT_IN_FLOW_SENT = "[Completed / Not In Flow]";

	@Getter
	@Setter
	@AllArgsConstructor
	public static class ForegroundBackground {
		private final Color foreground;
		private final Color background;
	}

	public JobViewItemVO(FluidItem fluidItm) {
		super(fluidItm);
		Form form = this.getForm();
		this.fluidItemId = fluidItm.getId();
		this.title = form.getTitle();
		this.groupingColors = this.getGroupingColors();
		this.formContainerId = form.getId();
		this.dateLastUpdated = form.getDateLastUpdated();
		this.stepEnteredTime = fluidItm.getStepEnteredTime();
		this.dateCreated = form.getDateCreated();

		this.step = COMPLETED_NOT_IN_FLOW_SENT;
		if (fluidItm.getStep() != null && !fluidItm.getStep().trim().isEmpty()) this.step = fluidItm.getStep();

		this.type = form.getFormType();
		this.state = form.getState();

		this.currentUser = (form.getCurrentUser() == null ||
				form.getCurrentUser().getUsername().trim().isEmpty()) ? "[None]" :
				form.getCurrentUser().getUsername();
	}

	public JobViewItemVO cloneVO(
		FluidItem fluidItem,
		List<Field> fieldsViewable,
		List<Field> fieldsEditable,
		List<User> allUsers
	) {
		JobViewItemVO returnVal = new JobViewItemVO(fluidItem);
		returnVal.setFieldsViewable(fieldsViewable);
		returnVal.setFieldsEditable(fieldsEditable);
		returnVal.setAllUsers(allUsers);
		return returnVal;
	}

	public boolean isGrouping() {
		return (this.groupingColors != null && !this.groupingColors.isEmpty());
	}

	public boolean isGroupingPresentForField(String fieldName) {
		return (this.groupingColors != null && !this.groupingColors.isEmpty());
	}

	public String getGroupingAdditionalClass(String fieldName) {
		if (UtilGlobal.isBlank(fieldName)) return UtilGlobal.EMPTY;

		if (this.groupingColors.containsKey(fieldName)) {
			return "customer-badge status-renewal";
		}

		return UtilGlobal.EMPTY;
	}

	public String getGroupingAdditionalStyle(String fieldName) {
		if (UtilGlobal.isBlank(fieldName)) return UtilGlobal.EMPTY;

		ForegroundBackground fb = this.groupingColors.get(fieldName);
		if (fb == null) return UtilGlobal.EMPTY;

		Color foreground = fb.getForeground();
		Color background = fb.getBackground();

		String hexForeground = String.format("#%s", Integer.toHexString(foreground.getRGB()).substring(2));
		String hexBackground = String.format("#%s", Integer.toHexString(background.getRGB()).substring(2));

		return String.format("color: %s; background: %s;", hexForeground, hexBackground);
	}
}
