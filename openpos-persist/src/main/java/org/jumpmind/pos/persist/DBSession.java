package org.jumpmind.pos.persist;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.Row;
import org.jumpmind.pos.persist.cars.EntityId;
import org.jumpmind.pos.persist.cars.ServiceInvoice;
import org.jumpmind.pos.persist.cars.ServiceInvoiceId;
import org.jumpmind.pos.persist.impl.DatabaseSchema;
import org.jumpmind.pos.persist.impl.DefaultMapper;
import org.jumpmind.pos.persist.impl.QueryTemplate;
import org.jumpmind.pos.persist.impl.QueryTemplates;
import org.jumpmind.pos.persist.impl.Transaction;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

public class DBSession {

    private Transaction currentTransaction;

    private String catalogName;
    private String schemaName;
    private DatabaseSchema databaseSchema;
    private IDatabasePlatform databasePlatform;
    private JdbcTemplate jdbcTemplate;
    private Map<String, String> sessionContext;
    private Map<String, QueryTemplate> queryTemplates;

    public DBSession(String catalogName, String schemaName, DatabaseSchema databaseSchema, IDatabasePlatform databasePlatform,
            Map<String, String> sessionContext, Map<String, QueryTemplate> queryTemplates) {
        super();
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.databaseSchema = databaseSchema;
        this.databasePlatform = databasePlatform;
        this.sessionContext = sessionContext;
        this.jdbcTemplate = new JdbcTemplate(databasePlatform.getDataSource());
        this.queryTemplates = queryTemplates;
    }

    public <T> List<T> findAll(Class<T> clazz) {
        return null;
    }

    public <T extends Entity> T findByRowId(Class<T> entityClass, String id) {
        List<T> results = this.find(entityClass, 
                getValidatedTable(entityClass), 
                getRowIdWhereClause(entityClass), 
                Arrays.asList(id));
        if (results != null && results.size() == 1) {
            return results.get(0);
        } else {
            return null;
        }
    }

    public <T extends Entity> T findByNaturalId(Class<T> entityClass, Object id) {
        Map<String, String> naturalColumnsToFields = databaseSchema.getNaturalColumnsToFields(entityClass);

        if (naturalColumnsToFields.size() != 1) {
            throw new PersistException(String.format("findByNaturalId cannot be used with a single 'id' "
                    + "param because the entity defines %s natural key fields.", naturalColumnsToFields.size()));
        }
        
        EntityId entityId = new EntityId() {
            public Map<String, Object> getIdFields() {
                Map<String, Object> fields = new HashMap<>();
                String fieldName = naturalColumnsToFields.values().iterator().next();
                fields.put(fieldName, id);
                return fields;
            }
        };
        
        return findByNaturalId(entityClass, entityId);
    }

    public <T extends Entity> T findByNaturalId(Class<T> entityClass, EntityId id) {
        Map<String, String> naturalColumnsToFields = databaseSchema.getNaturalColumnsToFields(entityClass);

        QueryTemplate queryTemplate = new QueryTemplate();

        try {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> fieldValues = id.getIdFields();
            
            for (String columnName : naturalColumnsToFields.keySet()) {   
                String fieldName = naturalColumnsToFields.get(columnName);
                queryTemplate.optionalWhere(String.format("%s = ${%s}", columnName, fieldName));
                if (fieldValues != null) {
                    Object value = fieldValues.get(fieldName);
                    params.put(fieldName, value);
                } else {
                    Field field = id.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(id);
                    params.put(fieldName, value);
                }
            }

            Query<T> query = new Query<T>().result(entityClass);
            query.setQueryTemplate(queryTemplate);
            List<T> results = query(query, params);

            if (results != null) {
                if (results.size() == 1) {                
                    return results.get(0);
                } else {
                    throw new PersistException(String.format("findByNaturalId must result in 0 or 1 rows, "
                            + "but instead resulted in %s rows. Sql used: %s", results.size(), query.generateSQL(params).getSql()));
                }
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new PersistException("findByNaturalId failed.", ex);            
        }
    }

    public int executeSql(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }
    
    public <T> List<T> query(Query<T> query) {
        return query(query, (Map<String, Object>)null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> query(Query<T> query, String singleParam) {
        populdateDefaultSelect(query);
        try {
            SqlStatement sqlStatement = query.generateSQL(singleParam);
            return (List<T>) queryInternal(query.getResultClass(), sqlStatement.getSql(), Arrays.asList(singleParam));
        } catch (Exception ex) {
            throw new PersistException("Failed to query target class " + query.getResultClass(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> query(Query<T> query, Map<String, Object> params) {
        populdateDefaultSelect(query);
        try {
            SqlStatement sqlStatement = query.generateSQL(params);
            return (List<T>) queryInternal(query.getResultClass(), sqlStatement.getSql(), sqlStatement.getValues());
        } catch (Exception ex) {
            throw new PersistException("Failed to query result class " + query.getResultClass(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> void populdateDefaultSelect(Query<T> query) {
        boolean isEntityResult = Entity.class.isAssignableFrom(query.getResultClass()); 
        
        if (query.getQueryTemplate() == null) {
            if (!StringUtils.isEmpty(query.getName())) {                
                query.setQueryTemplate(queryTemplates.get(query.getName()));
            } else {
                query.setQueryTemplate(new QueryTemplate());
            }
        }
        
        if (isEntityResult
            && StringUtils.isEmpty(query.getQueryTemplate().getSelect())) {
            Class<? extends Entity> entityClass = (Class<? extends Entity>) query.getResultClass();
            query.getQueryTemplate().setSelect(getSelectSql(entityClass));
        }
    }

    // TODO support transactions.
    public void startTransaction() {
        checkTransactionNotStarted();
        currentTransaction = new Transaction();
    }

    private void checkTransactionNotStarted() {
        if (currentTransaction != null) {
            throw new PersistException("Transaction already started.  The previous transaction must be committed "
                    + "or rolled back before starting another transacstion. " + currentTransaction);
        }        
    }

    public void commitTransaction() {
        checkTransactionStarted();
    }

    public void rollbackTransaction() {
        checkTransactionStarted();
    }

    protected LinkedHashMap<String, Column> mapObjectToTable(Class<?> resultClass, Table table) {
        LinkedHashMap<String, Column> columnNames = new LinkedHashMap<String, Column>();
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(resultClass);
        for (int i = 0; i < pds.length; i++) {
            String propName = pds[i].getName();
            Column column = table.getColumnWithName(DatabaseSchema.camelToSnakeCase(propName));
            if (column != null) {
                columnNames.put(propName, column);
            }
        }
        return columnNames;
    }    

    public String save(Entity entity) {
        Table table = getValidatedTable(entity);

        String rowId = entity.getRowId();
        if (StringUtils.isEmpty(rowId)) {
            rowId = generateRowId();
            entity.setRowId(rowId);
            insert(entity, table);
        } else {            
            if (update(entity, table) == 0) {
                insert(entity, table);
            }
        }

        return rowId;
    }

    protected String getSelectSql(Class<? extends Entity> entity) {
        Table table = getValidatedTable(entity);  
        LinkedHashMap<String, Column> objectToTableMapping = mapObjectToTable(entity, table);

        Column[] columns = objectToTableMapping.values().toArray(new Column[objectToTableMapping.size()]);

        DmlStatement statement = databasePlatform.createDmlStatement(DmlType.SELECT, table.getCatalog(), table.getSchema(),
                table.getName(), null, columns, null, null);
        String sql = statement.getSql();
        return sql;
    }

    private void setRowModificationData(Entity entity) {
    }

    protected void insert(Entity entity, Table table) {
        if (StringUtils.isEmpty(entity.getCreateBy())) {
            entity.setCreateBy(sessionContext.get("CREATE_BY"));
        }
        if (StringUtils.isEmpty(entity.getLastUpdateBy())) {
            entity.setLastUpdateBy(sessionContext.get("LAST_UPDATE_BY"));
        }        
        excecuteDml(DmlType.INSERT, entity, table);
    }

    protected int update(Entity entity, Table table) {
        if (StringUtils.isEmpty(entity.getCreateBy())) {
            entity.setCreateBy(sessionContext.get("CREATE_BY"));
        }        
        if (StringUtils.isEmpty(entity.getLastUpdateBy())) {
            entity.setCreateBy(sessionContext.get("LAST_UPDATE_BY"));
        }

        return excecuteDml(DmlType.UPDATE, entity, table);
    }

    protected int excecuteDml(DmlType type, Object object, org.jumpmind.db.model.Table table) {

        LinkedHashMap<String, Column> objectToTableMapping = mapObjectToTable(object.getClass(), table);
        LinkedHashMap<String, Object> objectValuesByColumnName = getObjectValuesByColumnName(object, objectToTableMapping);

        Column[] columns = objectToTableMapping.values().toArray(new Column[objectToTableMapping.size()]);
        List<Column> keys = new ArrayList<Column>(1);
        for (Column column : columns) {
            if (column.isPrimaryKey()) {
                keys.add(column);
            }
        }

        boolean[] nullKeyValues = new boolean[keys.size()];
        int i = 0;
        for (Column column : keys) {
            nullKeyValues[i++] = objectValuesByColumnName.get(column.getName()) == null;
        }

        DmlStatement statement = databasePlatform.createDmlStatement(type, table.getCatalog(), table.getSchema(), table.getName(),
                keys.toArray(new Column[keys.size()]), columns, nullKeyValues, null);
        String sql = statement.getSql();
        Object[] values = statement.getValueArray(objectValuesByColumnName);
        int[] types = statement.getTypes();

        return jdbcTemplate.update(sql, values, types);

    }

    protected LinkedHashMap<String, Object> getObjectValuesByColumnName(Object object, LinkedHashMap<String, Column> objectToTableMapping) {
        try {
            LinkedHashMap<String, Object> objectValuesByColumnName = new LinkedHashMap<String, Object>();
            Set<String> propertyNames = objectToTableMapping.keySet();
            for (String propertyName : propertyNames) {
                objectValuesByColumnName.put(objectToTableMapping.get(propertyName).getName(),
                        PropertyUtils.getProperty(object, propertyName));
            }
            return objectValuesByColumnName;
        } catch (Exception ex) {
            throw new PersistException("Failed to getObjectValuesByColumnName on object " + object + " objectToTableMapping: " + objectToTableMapping);
        }
    }    

    protected org.jumpmind.db.model.Table getValidatedTable(Entity entity) {
        if (entity == null) {
            throw new PersistException("Failed to locate a database table for null entity class.");
        }
        return getValidatedTable(entity.getClass());
    }

    protected Table getValidatedTable(Class<?> entityClass) {
        org.jumpmind.db.model.Table table = databaseSchema.getTable(entityClass);
        if (table == null) {
            throw new PersistException("Failed to locate a database table for entity class: " + entityClass);
        }
        return table;
    }    

    protected String generateRowId() {
        return UUID.randomUUID().toString();
    }

    protected void checkTransactionStarted() {
        if (currentTransaction == null) {
            throw new PersistException("No transaction was started - call startTransaction() before using this.");
        }        
    }

    protected <T extends Entity> List<T> find(Class<T> entityClass, Table table, String whereClause, List<Object> params) {
        try {
            T entity = entityClass.newInstance();
            String sql = getSelectSql(entityClass) + whereClause;
            return queryInternal(entityClass, sql, params);
        } catch (Exception ex) {
            throw new PersistException("Failed to query entityClass " + entityClass + " table: " + table, ex);
        }
    }

    protected <T> List<T> queryInternal(Class<T> resultClass, String sql, List<Object> params)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        List<Row> rows = jdbcTemplate.query(sql, new DefaultMapper(), params.toArray());
        List<T> objects = new ArrayList<T>();

        for (Row row : rows) {
            T object = resultClass.newInstance();

            LinkedHashMap<String, Column> columnNames = new LinkedHashMap<String, Column>();
            PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(object);
            for (int i = 0; i < pds.length; i++) {
                String propertyName = pds[i].getName();
                String columnName = DatabaseSchema.camelToSnakeCase(propertyName);

                if (row.containsKey(columnName)) {
                    Object value = row.get(columnName);
                    BeanUtils.copyProperty(object, propertyName, value);    
                }
            }
            objects.add(object);
        }

        if (objects != null && !objects.isEmpty()) {
            return objects;
        } else {
            return null;            
        }
    }

    protected <T> String getRowIdWhereClause(Class<T> entityClass) {
        return String.format("row_id = ?");
    }




}
