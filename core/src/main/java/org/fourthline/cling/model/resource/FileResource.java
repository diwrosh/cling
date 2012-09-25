package org.teleal.cling.model.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;

public class FileResource extends InputStreamResource<String> {
	
	public FileResource(URI localURI, String model, String mimeType, long size) {
		super(localURI, model, mimeType, size);
	}

	public long getSize() {
		if(size == -1) {
			File file = new File(getModel());
			size = file.length();
			if(size == 0) {
				size = -1;
			}
		}
		return size;
	}

	public InputStream getInputStream() throws FileNotFoundException {
		return new FileInputStream(getModel());
	}
}
