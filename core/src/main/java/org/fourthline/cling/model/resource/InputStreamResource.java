package org.teleal.cling.model.resource;

import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;

import org.teleal.common.util.MimeType;

public abstract class InputStreamResource<M> extends Resource<M> {
	
	final private static Logger log = Logger.getLogger(InputStreamResource.class.getName());
	
	protected static MimeType MIME_TYPE_OCTET_STREAM = new MimeType("application", "octet-stream");
	
	protected String mimeType;  		
			

	protected long size;
	
	public InputStreamResource(URI localURI, M model, String mimeType, long size) {
		super(localURI, model);
		this.mimeType = mimeType;
		this.size = size;
	}

	public MimeType getMimeType() {
		if(mimeType != null) {
			try {
				return MimeType.valueOf(mimeType);
			} catch(IllegalArgumentException e) {
				log.warning(String.format("cannot parse mime-type %s", mimeType));
			}
		}
		return MIME_TYPE_OCTET_STREAM;
	}

	public long getSize() throws Exception {
		return size;
	}

	public abstract InputStream getInputStream() throws Exception;
	
}
