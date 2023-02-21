package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.item.FluidItemListing;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkspaceUserQueryLDM extends ABaseLDM<WorkspaceFluidItem> {
	private final Long userQueryId;
	private final String label;
	private final FluidClientDS clientDS;
	private WebKitUserQueryBean webKitUserQueryBean;

	public WorkspaceUserQueryLDM(
		FluidClientDS clientDS,
		Long userQueryId,
		String label,
		WebKitUserQueryBean webKitUserQueryBean
	) {
		super();
		this.clientDS = clientDS;
		this.userQueryId = userQueryId;
		this.label = label;
		this.webKitUserQueryBean = webKitUserQueryBean;
	}

	@Override
	public List<WorkspaceFluidItem> load(
		int first,
		int pageSize,
		Map<String, SortMeta> sortMeta,
		Map<String, FilterMeta> filters
	) {
		long now = System.currentTimeMillis();
		this.webKitUserQueryBean.setEmptyMessage(String.format("No items for '%s'.", this.label));
		this.setRowCount(0);
		if (this.dataListing == null) return null;

		WebKitUserQuery webKitUserQuery = this.webKitUserQueryBean.getContentView().getWkUserQuery();
		boolean executeCustomProgramLabels = false;
		if (webKitUserQuery != null) {
			executeCustomProgramLabels = webKitUserQuery.isEnableCalculatedLabels();
		}

		List<Field> inputFields = this.webKitUserQueryBean.getInputFieldValues();
		try {
			UserQueryClient userQueryClient = this.clientDS.getUserQueryClient();
			UserQuery userQueryToExec = new UserQuery(this.userQueryId);
			userQueryToExec.setInputs(inputFields);

			FluidItemListing result = userQueryClient.executeUserQuery(
				userQueryToExec,
				false,
				executeCustomProgramLabels,
				pageSize,
				first,
				false
			);
			long end = System.currentTimeMillis();

			List<WorkspaceFluidItem> wfiList = new ArrayList<>();
			this.setRowCount(result.getListingCount());

			if (result.getListingCount() > 0) {
				result.getListing().forEach(flItm -> {
					UserQueryItemVO vo = this.webKitUserQueryBean.createABaseWebVO(null, null, null, flItm);
					wfiList.add(new WorkspaceFluidItem(vo));
				});
			}

			if (result.getListingCount() > 0) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Success.", String.format("Total of %d results took %d millis.",
						result.getListingCount(), end - now));
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			} else {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_WARN,
						"No Result.", String.format("Took %d millis to return No Results.", end - now));
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			}
			return wfiList;
		} catch (Exception err) {
			this.webKitUserQueryBean.raiseError(err);
			this.webKitUserQueryBean.setEmptyMessage(String.format("Error: %s", err.getMessage()));
			return null;
		}
	}
}
