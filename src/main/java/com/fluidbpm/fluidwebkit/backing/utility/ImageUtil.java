package com.fluidbpm.fluidwebkit.backing.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
/*
	INSERT INTO attachment_content_type (description, extensions) VALUES ('image/png', 'png');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('image/jpeg', 'jpe,jpeg,jpg');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('image/tiff', 'tiff,tif');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('image/gif', 'gif');

	INSERT INTO attachment_content_type (description, extensions) VALUES ('text/plain', 'txt');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('text/xml', 'xml');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('text/html', 'shtml,html,htm');

	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/json', 'json');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/mp4', 'mp4');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/pdf', 'pdf');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/zip', 'zip');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/rtf', 'rtf');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/msword', 'doc,dot');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'docx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.wordprocessingml.template', 'dotx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.ms-excel', 'xls');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'xlsx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.spreadsheetml.template', 'xltx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.ms-powerpoint', 'ppt,pps,ppa,pot');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.presentationml.presentation', 'pptx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.presentationml.template', 'potx');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.openxmlformats-officedocument.presentationml.slideshow', 'ppsx');


	INSERT INTO attachment_content_type (description, extensions) VALUES ('audio/mpeg3', 'mp3');


	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/java-archive', 'jar');
	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/x-java-class', 'class');

	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/vnd.ms-outlook', 'msg');

	INSERT INTO attachment_content_type (description, extensions) VALUES ('application/x-tar', 'tar,tar.gz');
/**Image**/

	public static byte[] getThumbnailPlaceholderImageFor(
		String contentType
	) throws IOException {
		final String contentTypeLower = (contentType == null) ? null : contentType.toLowerCase();
		if (contentTypeLower != null) {
			switch (contentTypeLower) {
				case "application/pdf":
					return getContentForPath("/image/thumbnail/pdf.png");
				case "application/msword":
				case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
					return getContentForPath("/image/thumbnail/word.png");
			}
		}

		return getContentForPath("/image/thumbnail/lost.png");
	}

	public static byte[] getThumbnailPlaceholderImageForNoAccess() throws IOException {
		return getContentForPath("/image/thumbnail/no-access.png");
	}

	public static byte[] getThumbnailPlaceholderImageForPDF() throws IOException {
		return getContentForPath("/image/thumbnail/pdf.png");
	}

	public static byte[] getThumbnailPlaceholderImageForTXT() throws IOException {
		return getContentForPath("/image/thumbnail/txt.png");
	}

	public static byte[] getThumbnailPlaceholderImageForEXCEL() throws IOException {
		return getContentForPath("/image/thumbnail/excel.png");
	}

	public static byte[] getThumbnailPlaceholderImageForWORD() throws IOException {
		return getContentForPath("/image/thumbnail/word.png");
	}

	public static byte[] getThumbnailPlaceholderImageForImage() throws IOException {
		return getContentForPath("/image/thumbnail/image.png");
	}

	public static byte[] getThumbnailPlaceholderImageForLost() throws IOException {
		return getContentForPath("/image/thumbnail/lost.png");
	}

	public static byte[] getContentForPath(String path) throws IOException {
		if (path == null || path.isEmpty()) {
			throw new IOException("Path to content in package is not set!");
		}

		InputStream inputStream = ImageUtil.class.getClassLoader().getResourceAsStream(path);
		if (inputStream == null) {
			throw new IOException("Unable to find '"+ path+"'.");
		}
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			int readVal = -1;
			while ((readVal = inputStream.read()) != -1) {
				baos.write(readVal);
			}
			baos.flush();
			return baos.toByteArray();
		}
	}
}
