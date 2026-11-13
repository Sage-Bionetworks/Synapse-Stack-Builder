package org.sagebionetworks.template.cdn;

public interface CdnBuilder {
	enum Type {
		DATA,
		PORTAL,
		STATIC_CONTENT
	}

	public void buildCdn(Type type);

}
