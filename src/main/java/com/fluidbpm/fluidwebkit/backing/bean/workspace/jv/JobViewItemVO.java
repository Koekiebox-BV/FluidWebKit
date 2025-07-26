package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
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

/**
 * Value Object representing a Job View item in the WebKit interface.
 * 
 * This class encapsulates information about a job item in a workflow, including:
 * - Basic information (title, type, state)
 * - Workflow information (step, current user)
 * - Identifiers (fluidItemId, formContainerId)
 * - Timestamps (dateLastUpdated, stepEnteredTime, dateCreated)
 * 
 * It provides functionality to create a JobViewItemVO from a FluidItem and to clone
 * a JobViewItemVO with additional information.
 *
 * @author jasonbruwer
 * @since 1.0
 */
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

	public static final String COMPLETED_NOT_IN_FLOW_SENT = "[Completed / Not In Flow]";

	/**
	 * Inner class representing foreground and background colors for a job view item.
	 * This is used for visual styling of job items in the UI based on their state or other properties.
	 */
	@Getter
	@Setter
	@AllArgsConstructor
	public static class ForegroundBackground {
		/** The foreground (text) color */
		private final Color foreground;
		
		/** The background color */
		private final Color background;
	}

	/**
	 * Constructs a JobViewItemVO from a FluidItem.
	 * 
	 * Extracts and initializes all relevant information from the FluidItem and its associated Form,
	 * including title, identifiers, timestamps, workflow information, and state.
	 * 
	 * @param fluidItm The FluidItem from which to create this JobViewItemVO
	 */
	public JobViewItemVO(FluidItem fluidItm) {
		super(fluidItm);
		Form form = this.getForm();
		this.fluidItemId = fluidItm.getId();
		this.title = form.getTitle();
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

	/**
	 * Creates a clone of this JobViewItemVO with additional information.
	 * 
	 * This method creates a new JobViewItemVO from the provided FluidItem and
	 * sets additional information such as viewable fields, editable fields, and users.
	 * 
	 * @param fluidItem The FluidItem to use for the new JobViewItemVO
	 * @param fieldsViewable List of fields that are viewable
	 * @param fieldsEditable List of fields that are editable
	 * @param allUsers List of all users
	 * @return A new JobViewItemVO with the specified information
	 */
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
}
