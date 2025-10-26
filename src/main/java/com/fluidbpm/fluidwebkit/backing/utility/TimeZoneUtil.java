package com.fluidbpm.fluidwebkit.backing.utility;

import com.fluidbpm.program.api.util.UtilGlobal;

import jakarta.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

public class TimeZoneUtil {
	public static enum ZoneDescription {
		InternationalDateLineWest(-12.0f,"International Date Line West"),
		None__11_5(-11.5f,""),
		MidwayIslandSamoa(-11.0f,"Midway Island, Samoa"),
		None__10_5(-10.5f,""),
		Hawaii(-10.0f,"Hawaii"),
		None__9_5(-9.5f,""),
		Alaska(-9.0f,"Alaska"),
		None__8_5(-8.5f,""),
		PacificTime(-8.0f,"Pacific Time (US & Canada)"),
		None__7_5(-7.5f,""),
		MountainTime(-7.0f,"Mountain Time (US & Canada)"),
		None__6_5(-6.5f,""),
		CentralAmerica(-6.0f,"Central America"),
		None__5_5(-5.5f,""),
		BogotaLimaQuitoRio(-5.0f,"Bogota, Lima, Quito, Rio Branco"),
		None__4_5(-4.5f,""),
		Santiago(-4.0f,"Santiago"),
		Newfoundland(-3.5f,"Newfoundland"),
		Greenland(-3.0f,"Greenland"),
		None__2_5(-2.5f,""),
		MidAtlantic(-2.0f,"Mid-Atlantic"),
		None__1_5(-1.5f,""),
		CapeVerde(-1.0f,"Cape Verde Is."),
		None__0_5(-0.5f,""),
		GreenwichMeanTime(0.0f,"Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"),
		None_0_5(0.5f,""),
		AmsterdamBerlinBernRome(1.0f,"Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"),
		None_1_5(1.5f,""),
		HararePretoria(2.0f,"Harare, Pretoria"),
		None_2_5(2.5f,""),
		MoscowStPetersburg(3.0f,"Moscow, St. Petersburg, Volgograd"),
		Tehran(3.5f,"Tehran"),
		AbuDhabi(4.0f,"Abu Dhabi, Muscat"),
		Kabul(4.5f,"Kabul"),
		Islamabad(5.0f,"Islamabad, Karachi, Tashkent"),
		Chennai(5.5f,"Chennai, Kolkata, Mumbai, New Delhi"),
		Astana(6.0f,"Astana, Dhaka"),
		Yangon(6.5f,"Yangon (Rangoon)"),
		Bankkok(7.0f,"Bangkok, Hanoi, Jakarta"),
		None_7_5(7.5f,""),
		Beijing(8.0f,"Beijing, Chongqing, Hong Kong, Urumqi"),
		None_8_5(8.5f,""),
		Osaka(9.0f,"Osaka, Sapporo, Tokyo"),
		Darwin(9.5f,"Darwin"),
		Canberra(10.0f,"Canberra, Melbourne, Sydney"),
		None_10_5(10.5f,""),
		Magadan(11.0f,"Magadan, Solomon Is., New Caledonia"),
		None_11_5(11.5f,""),
		Auckland(12.0f,"Auckland, Wellington"),;

		private float timeValue;
		private String description;

		private static final String Format ="%02d";
		ZoneDescription(float timeValueParam, String descriptionParam)
		{
			this.timeValue = timeValueParam;
			this.description = descriptionParam;
		}

		public static String getDescriptionForTimeValue(float timeValueParam) {
			ZoneDescription[] values = ZoneDescription.values();
			for(ZoneDescription atIndex : values)
				if (atIndex.timeValue == timeValueParam) return atIndex.description;
			return UtilGlobal.EMPTY;
		}

		public static String getFullDescriptionForTimeValue(Float timeValueParam) {
			if (timeValueParam == null) return null;
			float timeValueParamAsFloat = timeValueParam.floatValue();

			int hour = ((int)(timeValueParamAsFloat)),minute = 0;
			if (timeValueParamAsFloat % 1.0 != 0) minute = Math.abs((int) ((timeValueParamAsFloat % 1.0) * 60f));

			String label =
					(timeValueParamAsFloat > 0 ? "+":"")+
							(String.format(Format,hour) + ":" + String.format(Format,minute));
			label += " ";
			label += ZoneDescription.getDescriptionForTimeValue(timeValueParamAsFloat);
			return label;
		}
	}

	public static ZoneDescription getZoneDescriptionBy(Float timeZoneValueParam) {
		if(timeZoneValueParam == null) return null;

		for (ZoneDescription zoneDesc : ZoneDescription.values())
			if(zoneDesc.timeValue == timeZoneValueParam.floatValue()) return zoneDesc;
		return null;
	}

	public static List<SelectItem> getAvailableTimeZonesAsSelectItems() {
		List<SelectItem> returnVal = new ArrayList();
		for(float index = -12;index < 12.1;index += 0.5f) {
			String label = ZoneDescription.getFullDescriptionForTimeValue(index);
			returnVal.add(new SelectItem(Float.valueOf(index), label));
		}
		return returnVal;
	}
}
