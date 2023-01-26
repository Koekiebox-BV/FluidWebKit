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

package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.backing.utility.WebUtil;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.DecimalMetaFormat;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bean storing all access for the logged in user.
 */
@RequestScoped
@Named("webKitConfHelpBean")
public class WebKitConfigHelperBean extends ABaseManagedBean {
	@Inject
	private WebKitConfigBean webKitConfigBean;

	/**
	 * Request params.
	 */
	private static final class WebParam {
		private static final String ErrorMessage = "errorMessage";
		private static final String InfoMessageHeader = "infoMessageHeader";
		private static final String InfoMessage = "infoMessage";
	}

	public static final class Masked {
		public static final String MASKED = "Masked";//0,1,2,3,4,5,6,7

		public static String getMaskedValue(String storedMaskedValue) {
			if (UtilGlobal.isBlank(storedMaskedValue)) return null;

			int maskedLength = Masked.MASKED.length();
			if (storedMaskedValue.length() >= maskedLength) {
				String prefix = storedMaskedValue.substring(0, maskedLength);
				if (!Masked.MASKED.equals(prefix)) return null;

				String maskedValue = storedMaskedValue.substring(maskedLength);
				if (maskedValue == null) return null;

				return maskedValue;
			}
			return null;
		}

		public static int[] getMaskedIndexesFromValue(String storedMaskedValue) {
			String maskedValue = getMaskedValue(storedMaskedValue);
			if (maskedValue == null) return null;

			String[] splitted = maskedValue.split(UtilGlobal.REG_EX_COMMA);
			if (splitted == null || splitted.length == 0) return null;

			List<Integer> listOfIndexes = new ArrayList();
			for (String val : splitted) {
				int index = UtilGlobal.toIntSafe(val);
				if (index == -1) continue;

				listOfIndexes.add(Integer.valueOf(index));
			}

			int[] returnVal = new int[listOfIndexes.size()];
			for (int index = 0;index < returnVal.length;index++) {
				returnVal[index] = listOfIndexes.get(index).intValue();
			}

			return returnVal;
		}

		public static List<String> getMaskedIndexesFromValueAsStringList(String storedMaskedValue) {
			int[] indexesAsIntArray = getMaskedIndexesFromValue(storedMaskedValue);
			if (indexesAsIntArray == null) return null;

			List<String> returnVal = new ArrayList();
			for (int toAdd : indexesAsIntArray) returnVal.add(Integer.toString(toAdd));

			return returnVal;
		}

		public static boolean isMasked(String storedMaskedValue) {
			String theVal = Masked.getMaskedValue(storedMaskedValue);

			if (theVal == null) return false;

			return true;
		}

		public static String getInputValueAsEncryptedMasked(String metaData, String inputValueParam) {
			int[] maskedIndexes = Masked.getMaskedIndexesFromValue(metaData);

			String inputValue = inputValueParam;
			if (maskedIndexes == null || maskedIndexes.length == 0) return inputValue;

			if (UtilGlobal.isBlank(inputValue)) return UtilGlobal.EMPTY;

			int inputLength = inputValue.length();
			StringBuilder builder = new StringBuilder(inputValue);
			for (int maskedIndex :maskedIndexes) {
				int realIndex = (maskedIndex - 1);
				if (realIndex < 0) realIndex = 0;
				if (realIndex < inputLength) builder.setCharAt(realIndex, '*');
			}

			return builder.toString();
		}
	}

	/**
	 * Retrieve the FusionReactor script for tracking user experience.
	 * @return JS for user tracking
	 */
	public String getFRUserTrackingScript() {
		return Globals.getFRUserTrackingScript();
	}


	/**
	 * Get the configured or server URL.
	 *
	 * @return {@code FluidServerURL} value or server url.
	 */
	public String getFluidWebAppServletPushURL() {
		String fluidServerUrlConfig = this.webKitConfigBean.getFluidServerURL();

		if (fluidServerUrlConfig != null && (!fluidServerUrlConfig.trim().isEmpty()) &&
				fluidServerUrlConfig.toLowerCase().startsWith("https")) {
			return new WebUtil().getConstructCompleteCallbackURL(true);
		}

		//From the request...
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

		String serverName = externalContext.getRequestServerName();
		int port = externalContext.getRequestServerPort();
		String contextPath = externalContext.getRequestContextPath();

		return externalContext.getRequestScheme().concat("://").concat(
				serverName).concat(":").concat(Integer.toString(port)).concat(contextPath).toString();
	}

	public void actionDisplayErrorRemote() {
		String value = this.getFacesRequestParameterMap().get(WebParam.ErrorMessage);
		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, value, UtilGlobal.EMPTY);
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	public void actionDisplayInformationRemote() {
		Map<String,String> reqParamMap = this.getFacesRequestParameterMap();

		String infoMessageHeader = reqParamMap.get(WebParam.InfoMessageHeader);
		String infoMessage = reqParamMap.get(WebParam.InfoMessage);

		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, infoMessageHeader, infoMessage);
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}
	
	public int getUserBrowserSessionDialogHeight() {
		return (this.getUserBrowserSessionWindowHeight() - 370);
	}

	public int getUserBrowserSessionDialogWidth() {
		return (this.getUserBrowserSessionWindowWidth() - 200);
	}

	public int getUserBrowserSessionScrollableHeight() {
		return (this.getUserBrowserSessionWindowHeight() - 100);
	}

	public int getUserBrowserSessionWindowHeight() {
		Object httpSessionObj =
				FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		if (httpSessionObj instanceof HttpSession) {
			HttpSession casted = (HttpSession)httpSessionObj;
			Object heightValue = casted.getAttribute(SessionVariable.USER_BROWSER_WINDOW_HEIGHT);
			if (heightValue != null && heightValue instanceof Number) return ((Number)heightValue).intValue();
		}
		return 900;
	}

	public int getUserBrowserSessionWindowWidth() {
		Object httpSessionObj =
				FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		if (httpSessionObj instanceof HttpSession) {
			HttpSession casted = (HttpSession)httpSessionObj;
			Object heightValue = casted.getAttribute(SessionVariable.USER_BROWSER_WINDOW_WIDTH);
			if (heightValue != null && heightValue instanceof Number) return ((Number)heightValue).intValue();
		}
		return 500;
	}

	public void actionDoNothing() {
		//Do nothing...
		//this.getLogger().debug("Action fired to do nothing.");
	}

	public String convertToUpper(String toConvert) {
		return toConvert.toUpperCase();
	}

	public String objectHashCode(Object objectAddress) {
		if (objectAddress == null) return UtilGlobal.NONE;
		return String.format("%s@%s",
				objectAddress.getClass().getName(),
				Integer.toHexString(objectAddress.hashCode()));
	}

	public boolean isFieldTypeDecimalMetaTypeRating(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Rating");
	}

	public boolean isFieldTypeDecimalMetaTypeSpinner(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Spinner");
	}

	public boolean isFieldTypeDecimalMetaTypeSlider(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Slider");
	}

	public boolean parseDecimalFormatIsStepFactorNoPrecision(String metaDataToParse) {
		DecimalMetaFormat parsed = DecimalMetaFormat.parse(metaDataToParse);
		return parsed.isPrecisionZeroForStepFactor();
	}

	public DecimalMetaFormat parseDecimalFormat(String metaDataToParse) {
		return DecimalMetaFormat.parse(metaDataToParse);
	}

	public boolean isFieldTypeTextMetaTypeBarcode(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		String barcodeType = this.getBarcodeType(metaDataTxt);
		return barcodeType != null && !barcodeType.trim().isEmpty();
	}

	public boolean isFieldTypeTextMetaTypeBarcodeQR(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		String barcodeType = this.getBarcodeType(metaDataTxt);
		return (barcodeType != null && "qr".equals(barcodeType.trim()));
	}

	public boolean isFieldTypeLabelMetaTypeAnchor(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Anchor_");
	}

	public boolean isFieldTypeLabelMetaTypePayment(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("MakePayment");
	}

	public boolean isFieldTypeLabelMetaTypeCustomWebAction(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("CustomWebAction");
	}

	public boolean isFieldTypeLabelMetaTypeJavascript(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Javascript_");
	}

	public boolean isFieldTypeMultiMetaTypeNotPlain(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return !this.isFieldTypeMultiMetaTypePlain(metaDataTxt);
	}

	public boolean isFieldTypeMultiMetaTypePlain(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return false;
		return metaDataTxt.startsWith("Plain");
	}

	public boolean isFieldTypeEncryptedMasked(String metaDataTxt) {
		return Masked.isMasked(metaDataTxt);
	}

	public String getJavascriptValueFromMetaData(String metaDataTxt) {
		return metaDataTxt.substring(11);
	}

	public String getAnchorValueFromMetaData(String metaDataTxt) {
		return metaDataTxt.substring(7);
	}

	public String extractDateTimeFormatFromMetaData(String fieldName, String metaDataTxt) {
		if ("Expiry Date Date".equals(fieldName)) return "yyyy-MM";

		if ("Date".equals(metaDataTxt)) {
			return this.getDateFormat();
		} else if ("Date and Time".equals(metaDataTxt)) {
			return this.getDateAndTimeFormat();
		}
		return "yyyy-MM-dd hh:mm";
	}

	public String extractDateAsTxt(String fieldName, Object dateValue, String fieldMetaTxt) {
		if (dateValue == null) return null;

		String format = this.extractDateTimeFormatFromMetaData(fieldName, fieldMetaTxt);
		if (dateValue instanceof Date) {
			return new SimpleDateFormat(format).format((Date)dateValue);
		} else {
			this.raiseError(new IllegalStateException(String.format("Object of type '%s' is not supported for date conversion.", dateValue.getClass().getName())));
			return "[Not Supported]";
		}
	}

	private static final String BARCODE_TXT_META = "Barcode";
	public String getBarcodeType(String metaDataTxt) {
		if (UtilGlobal.isBlank(metaDataTxt)) return null;

		int maskedLength = BARCODE_TXT_META.length();
		if (metaDataTxt.length() >= maskedLength) {
			String prefix = metaDataTxt.substring(0, maskedLength);
			if(!BARCODE_TXT_META.equals(prefix)) return null;

			String validBarcodeValue = metaDataTxt.substring(maskedLength);
			return validBarcodeValue;
		}
		return null;
	}

	public String createLabelFor(Object forLabel) {
		if (forLabel == null) return "";

		if (forLabel instanceof SelectItem) return ((SelectItem)forLabel).getLabel();

		if (forLabel instanceof List) {
			if (((List<String>)forLabel).isEmpty()) return "None Selected.";

			return ((List<String>)forLabel).stream().collect(Collectors.joining(", "));
		}
		
		return forLabel.toString();
	}
}
