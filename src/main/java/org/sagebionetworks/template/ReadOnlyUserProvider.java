package org.sagebionetworks.template;

public interface ReadOnlyUserProvider {
    public void createReadOnlyUser(String readOnlyUserName, String userPassword, String schema);
}
