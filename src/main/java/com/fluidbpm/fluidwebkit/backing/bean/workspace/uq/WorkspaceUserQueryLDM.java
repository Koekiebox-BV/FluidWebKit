package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.program.api.vo.item.FluidItemListing;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkspaceUserQueryLDM extends ABaseLDM<WorkspaceFluidItem> {
	private final Long userQueryId;
	private final String label;
	private final FluidClientDS clientDS;
	private WebKitUserQueryBean baseWorkspaceBean;

	public WorkspaceUserQueryLDM(
		FluidClientDS clientDS,
		Long userQueryId,
		String label,
		WebKitUserQueryBean baseWorkspaceBean
	) {
		super();
		this.clientDS = clientDS;
		this.userQueryId = userQueryId;
		this.label = label;
		this.baseWorkspaceBean = baseWorkspaceBean;
	}

	@Override
	public List<WorkspaceFluidItem> load(
		int first,
		int pageSize,
		String sortField,
		SortOrder sortOrder,
		Map<String, FilterMeta> filters
	) {
		this.baseWorkspaceBean.setEmptyMessage(String.format("No items for '%s'.", this.label));
		this.setRowCount(0);
		if (this.dataListing == null) {
			return null;
		}

		try {
			UserQueryClient userQueryClient = this.clientDS.getUserQueryClient();
			UserQuery userQueryToExec = new UserQuery(this.userQueryId);

			FluidItemListing result = userQueryClient.executeUserQuery(userQueryToExec, false, pageSize, first);

			List<WorkspaceFluidItem> wfiList = new ArrayList<>();
			this.setRowCount(result.getListingCount());
			result.getListing().forEach(flItm -> {
				UserQueryItemVO vo = this.baseWorkspaceBean.createABaseWebVO(null, null, null, flItm);
				wfiList.add(new WorkspaceFluidItem(vo));
			});
			return wfiList;
		} catch (Exception err) {
			this.baseWorkspaceBean.raiseError(err);
			this.baseWorkspaceBean.setEmptyMessage(String.format("Error: %s", err.getMessage()));
			return null;
		}
	}
}
