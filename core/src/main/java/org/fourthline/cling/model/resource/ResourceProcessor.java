package org.teleal.cling.model.resource;

import java.net.URI;



public interface ResourceProcessor {
	public Resource process(URI uri) throws Exception;
}