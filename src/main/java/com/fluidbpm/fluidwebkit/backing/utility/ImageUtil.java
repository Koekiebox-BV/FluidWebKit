package com.fluidbpm.fluidwebkit.backing.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
	public static byte[] getNonImagePreviewForContentType(String contentType) throws IOException {
		if (contentType == null) return null;

		String contentTypeLower = contentType.trim().toLowerCase();
		switch (contentTypeLower) {
			case "application/pdf" :
				return ImageUtil.getThumbnailPlaceholderImageForPDF();
			case "text/plain" :
				return ImageUtil.getThumbnailPlaceholderImageForTXT();
			case "text/xml" :
				return ImageUtil.getThumbnailPlaceholderImageForXML();
			case "application/json" :
				return ImageUtil.getThumbnailPlaceholderImageForJSON();
			case "application/mp4" :
				return ImageUtil.getThumbnailPlaceholderImageForMP4();
			case "text/html" :
				return ImageUtil.getThumbnailPlaceholderImageForHTML();
			case "application/zip" :
				return ImageUtil.getThumbnailPlaceholderImageForZIP();
			case "application/rtf" :
				return ImageUtil.getThumbnailPlaceholderImageForRTF();
			case "audio/mpeg3" :
				return ImageUtil.getThumbnailPlaceholderImageForMPEG3();
			case "application/java-archive" :
				return ImageUtil.getThumbnailPlaceholderImageForJAR();
			case "application/x-java-class" :
				return ImageUtil.getThumbnailPlaceholderImageForJAVACLASS();
			case "application/vnd.ms-outlook" :
				return ImageUtil.getThumbnailPlaceholderImageForOUTLOOK();
			case "application/x-tar" :
				return ImageUtil.getThumbnailPlaceholderImageForTAR();
			case "text/csv" :
				return ImageUtil.getThumbnailPlaceholderImageForCSV();
			case "application/vnd.ms-powerpoint" :
			case "application/vnd.openxmlformats-officedocument.presentationml.presentation" :
			case "application/vnd.openxmlformats-officedocument.presentationml.template" :
			case "application/vnd.openxmlformats-officedocument.presentationml.slideshow" :
				return ImageUtil.getThumbnailPlaceholderImageForPOWERPOINT();
			case "application/vnd.ms-excel" :
			case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" :
			case "application/vnd.openxmlformats-officedocument.spreadsheetml.template" :
				return ImageUtil.getThumbnailPlaceholderImageForEXCEL();
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" :
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.template" :
			case "application/msword" :
				return ImageUtil.getThumbnailPlaceholderImageForWORD();
			case "image/png" :
			case "image/jpeg" :
			case "image/tiff" :
			case "image/gif" :
				return null;
			default: {
				new RaygunUtil().raiseErrorToRaygun(new Exception(
						String.format("Content Type [%s] is not yet mapped for default image.", contentTypeLower)), null);
				return null;
			}
		}
	}

	public static byte[] getThumbnailPlaceholderImageForNoAccess() throws IOException {
		return getContentForPath("/image/thumbnail/no-access.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForPDF() throws IOException {
		return getContentForPath("/image/thumbnail/pdf.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForTXT() throws IOException {
		return getContentForPath("/image/thumbnail/txt.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForXML() throws IOException {
		return getContentForPath("/image/thumbnail/xml.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForJSON() throws IOException {
		return getContentForPath("/image/thumbnail/json.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForMP4() throws IOException {
		return getContentForPath("/image/thumbnail/mp4.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForHTML() throws IOException {
		return getContentForPath("/image/thumbnail/html.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForZIP() throws IOException {
		return getContentForPath("/image/thumbnail/zip.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForRTF() throws IOException {
		return getContentForPath("/image/thumbnail/rtf.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForMPEG3() throws IOException {
		return getContentForPath("/image/thumbnail/mp3.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForJAR() throws IOException {
		return getContentForPath("/image/thumbnail/java.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForJAVACLASS() throws IOException {
		return getContentForPath("/image/thumbnail/java-class.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForOUTLOOK() throws IOException {
		return getContentForPath("/image/thumbnail/outlook.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForTAR() throws IOException {
		return getContentForPath("/image/thumbnail/tar.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForCSV() throws IOException {
		return getContentForPath("/image/thumbnail/csv.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForEXCEL() throws IOException {
		return getContentForPath("/image/thumbnail/excel.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForPOWERPOINT() throws IOException {
		return getContentForPath("/image/thumbnail/powerpoint.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForWORD() throws IOException {
		return getContentForPath("/image/thumbnail/word.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForImage() throws IOException {
		return getContentForPath("/image/thumbnail/image.svg");
	}

	public static byte[] getThumbnailPlaceholderImageForNone() throws IOException {
		return getContentForPath("/image/thumbnail/none.svg");
	}

	public static byte[] getContentForPath(String path) throws IOException {
		if (path == null || path.isEmpty()) throw new IOException("Path to content in package is not set!");

		InputStream inputStream = ImageUtil.class.getClassLoader().getResourceAsStream(path);
		if (inputStream == null) throw new IOException("Unable to find '"+ path+"'.");

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			int readVal = -1;
			while ((readVal = inputStream.read()) != -1) baos.write(readVal);

			baos.flush();
			return baos.toByteArray();
		}
	}
}
