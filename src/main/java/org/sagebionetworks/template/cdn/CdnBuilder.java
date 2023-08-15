package org.sagebionetworks.template.cdn;

public interface CdnBuilder {
	enum Type {
		DATA,
		PORTAL
	}

	public void buildCdn(Type type);

}
