package org.sagebionetworks.template.repo.glue;

import java.util.Objects;

public class GlueColumn {
    private String name;
    private String type;
    private String comment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlueColumn that = (GlueColumn) o;
        return name.equals(that.name) && type.equals(that.type) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, comment);
    }
}
