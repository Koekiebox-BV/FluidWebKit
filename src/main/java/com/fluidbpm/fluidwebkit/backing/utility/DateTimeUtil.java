package com.fluidbpm.fluidwebkit.backing.utility;

import lombok.Getter;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DateTimeUtil {

	public static enum DateFormat {
		//  .
		dot_yyyy_MM_dd("yyyy.MM.dd"),
		dot_yyyy_MMM_dd("yyyy.MMM.dd"),
		dot_yyyy_MMMM_dd("yyyy.MMMM.dd"),

		dot_yy_MM_dd("yy.MM.dd"),
		dot_yy_MMM_dd("yy.MMM.dd"),
		dot_yy_MMMM_dd("yy.MMMM.dd"),

		dot_dd_MM_yy("dd.MM.yy"),
		dot_dd_MMM_yy("dd.MMM.yy"),
		dot_dd_MMMM_yy("dd.MMMM.yy"),

		dot_MM_dd_yy("MM.dd.yy"),
		dot_MMM_dd_yy("MMM.dd.yy"),
		dot_MMMM_dd_yy("MMMM.dd.yy"),

		dot_MM_dd_yyyy("MM.dd.yyyy"),
		dot_MMM_dd_yyyy("MMM.dd.yyyy"),
		dot_MMMM_dd_yyyy("MMMM.dd.yyyy"),

		//  space
		space_yyyy_MM_dd("yyyy MM dd"),
		space_yyyy_MMM_dd("yyyy MMM dd"),
		space_yyyy_MMMM_dd("yyyy MMMM dd"),

		space_yy_MM_dd("yy MM dd"),
		space_yy_MMM_dd("yy MMM dd"),
		space_yy_MMMM_dd("yy MMMM dd"),

		space_dd_MM_yy("dd MM yy"),
		space_dd_MMM_yy("dd MMM yy"),
		space_dd_MMMM_yy("dd MMMM yy"),

		space_MM_dd_yy("MM dd yy"),
		space_MMM_dd_yy("MMM dd yy"),
		space_MMMM_dd_yy("MMMM dd yy"),

		space_MM_dd_yyyy("MM dd yyyy"),
		space_MMM_dd_yyyy("MMM dd yyyy"),
		space_MMMM_dd_yyyy("MMMM dd yyyy"),

		//  -
		yyyy_MM_dd("yyyy-MM-dd"),
		yyyy_MMM_dd("yyyy-MMM-dd"),
		yyyy_MMMM_dd("yyyy-MMMM-dd"),

		yy_MM_dd("yy-MM-dd"),
		yy_MMM_dd("yy-MMM-dd"),
		yy_MMMM_dd("yy-MMMM-dd"),

		dd_MM_yy("dd-MM-yy"),
		dd_MMM_yy("dd-MMM-yy"),
		dd_MMMM_yy("dd-MMMM-yy"),

		MM_dd_yy("MM-dd-yy"),
		MMM_dd_yy("MMM-dd-yy"),
		MMMM_dd_yy("MMMM-dd-yy"),

		MM_dd_yyyy("MM-dd-yyyy"),
		MMM_dd_yyyy("MMM-dd-yyyy"),
		MMMM_dd_yyyy("MMMM-dd-yyyy"),

		//  /
		front_slash_yy_MM_dd("yy/MM/dd"),
		front_slash_yy_MMM_dd("yy/MMM/dd"),
		front_slash_yy_MMMM_dd("yy/MMMM/dd"),

		front_slash_yyyy_MM_dd("yyyy/MM/dd"),
		front_slash_yyyy_MMM_dd("yyyy/MMM/dd"),
		front_slash_yyyy_MMMM_dd("yyyy/MMMM/dd"),

		front_slash_dd_MM_yy("dd/MM/yy"),
		front_slash_dd_MMM_yy("dd/MMM/yy"),
		front_slash_dd_MMMM_yy("dd/MMMM/yy"),

		front_slash_MM_dd_yy("MM/dd/yy"),
		front_slash_MMM_dd_yy("MMM/dd/yy"),
		front_slash_MMMM_dd_yy("MMMM/dd/yy"),

		front_slash_MM_dd_yyyy("MM/dd/yyyy"),
		front_slash_MMM_dd_yyyy("MMM/dd/yyyy"),
		front_slash_MMMM_dd_yyyy("MMMM/dd/yyyy"),
		;

		@Getter
		private String format;

		DateFormat(String formatParam)
		{
			this.format = formatParam;
		}
	}

	public static enum TimeFormat {
		HH_mm_ss_aa("HH:mm:ss aa"),
		HH_mm_aa("HH:mm aa"),
		HH_mm("HH:mm"),

		hh_mm_ss_aa("hh:mm:ss aa"),
		hh_mm_aa("hh:mm aa"),
		hh_mm("hh:mm"),

		KK_mm_ss_aa("KK:mm:ss aa"),
		KK_mm_aa("KK:mm aa"),
		KK_mm("KK:mm"),

		kk_mm_ss_aa("kk:mm:ss aa"),
		kk_mm_aa("kk:mm aa"),
		kk_mm("kk:mm"),
		;

		@Getter
		private String format;

		TimeFormat(String formatParam)
		{
			this.format = formatParam;
		}
	}

	public static List<SelectItem> getAvailableDateFormatsAsSelectItems() {
		List<SelectItem> returnVal = new ArrayList();
		for (DateFormat dateFormat : DateFormat.values()) {
			returnVal.add(new SelectItem(
					dateFormat.getFormat(),
					dateFormat.getFormat()));
		}
		return returnVal;
	}

	public static List<SelectItem> getAvailableTimeFormatsAsSelectItems() {
		List<SelectItem> returnVal = new ArrayList();
		for(TimeFormat dateTimeFormat : TimeFormat.values()) {
			returnVal.add(new SelectItem(
					dateTimeFormat.getFormat(),
					dateTimeFormat.getFormat()));
		}

		return returnVal;
	}

}
