package com.fluidbpm.fluidwebkit.servlet;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigHelperBean;
import com.fluidbpm.fluidwebkit.backing.utility.RaygunUtil;
import com.fluidbpm.fluidwebkit.ds.FluidClientPool;
import com.fluidbpm.program.api.vo.user.User;
import lombok.Getter;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ABaseFWKServlet extends HttpServlet {

	@Inject
	@Getter
	protected transient FluidClientPool fcp;

	@Inject
	private WebKitConfigHelperBean webKitHelpBean;

	protected User getLoggedInUser(HttpServletRequest req) {
		if (req == null) return null;
		
		HttpSession httpSession = req.getSession(false);
		Object loggedInUser = null;
		if (httpSession != null) {
			loggedInUser = httpSession.getAttribute(ABaseManagedBean.SessionVariable.USER);
			if (loggedInUser instanceof User) return (User)loggedInUser;
		}
		return null;
	}

	public void raiseError(Exception exception, HttpServletRequest req) {
		this.webKitHelpBean.getLogger().error(exception.getMessage(), exception);
		if (RaygunUtil.isRaygunEnabled()) new RaygunUtil(this.webKitHelpBean.getRaygunUITag()).raiseErrorToRaygun(exception, this.getLoggedInUser(req));

		if (FacesContext.getCurrentInstance() == null) return;

		FacesMessage fMsg = new FacesMessage(
				FacesMessage.SEVERITY_ERROR, "Failed.", exception.getMessage());
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}


}
