package org.jumpmind.pos.persist.impl;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.pos.persist.PersistException;

public class QueryTemplate implements Cloneable {

    private String name;
    private List<String> selects;
    private List<String> optionalWhereClauses;
    private String groupBy;
    private String where;
    private String orderBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setOptionalWhereClauses(List<String> optionalWhereClauses) {
        this.optionalWhereClauses = optionalWhereClauses;
    }
    
    public void setSelects(List<String> selects) {
        this.selects = selects;
    }
    
    public List<String> getSelects() {
        if (selects == null) {
            selects = new ArrayList<>();
        }
        return selects;
    }

    public List<String> getOptionalWhereClauses() {
        if (optionalWhereClauses == null) {
            optionalWhereClauses = new ArrayList<>();
        }
        return optionalWhereClauses;
    }

    public QueryTemplate optionalWhere(String optionalWhere) {
        this.getOptionalWhereClauses().add(optionalWhere);
        return this;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public QueryTemplate copy() {
        try {
            return (QueryTemplate)this.clone();
        } catch (CloneNotSupportedException e) {
            throw new PersistException(e);
        }
    }

}
