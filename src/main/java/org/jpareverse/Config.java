package org.jpareverse;

import java.util.List;

public class Config {

    private String datasource_username;
    private String datasource_databaseDriver;
    private String datasource_password;
    private String datasource_databaseUrl;
    private String datasource_schema;
    private String destination_db;
    private String destination_path;
    private String output_package;
    private String use_model;
    private String version_field;
    private String base_model_package;
    private String flat_mode;
    private String lombok;
    private String ebean_annotations;
    private String size_constants;
    private List<TableDefinition> tables;
    private List<TableDefinition> tables_postpone;
    private List<BaseModel> baseModels;

    public Config() {
    }

    public String getUse_model() {
        return use_model;
    }

    public void setUse_model(String use_model) {
        this.use_model = use_model;
    }

    public String getDatasource_schema() {
        return datasource_schema;
    }

    public String getVersion_field() {
        return version_field;
    }

    public void setVersion_field(String version_field) {
        this.version_field = version_field;
    }

    public void setDatasource_schema(String datasource_schema) {
        this.datasource_schema = datasource_schema;
    }

    public String getSize_constants() {
        return size_constants;
    }

    public void setSize_constants(String size_constants) {
        this.size_constants = size_constants;
    }

    public String getBase_model_package() {
        return base_model_package;
    }

    public void setBase_model_package(String base_model_package) {
        this.base_model_package = base_model_package;
    }

    public String getFlat_mode() {
        return flat_mode;
    }

    public void setFlat_mode(String flat_mode) {
        this.flat_mode = flat_mode;
    }

    public String getEbean_annotations() {
        return ebean_annotations;
    }

    public void setEbean_annotations(String ebean_annotations) {
        this.ebean_annotations = ebean_annotations;
    }

    public String getLombok() {
        return lombok;
    }

    public void setLombok(String lombok) {
        this.lombok = lombok;
    }

    public String getDatasource_username() {
        return datasource_username;
    }

    public void setDatasource_username(String datasource_username) {
        this.datasource_username = datasource_username;
    }

    public String getDatasource_databaseDriver() {
        return datasource_databaseDriver;
    }

    public void setDatasource_databaseDriver(String datasource_databaseDriver) {
        this.datasource_databaseDriver = datasource_databaseDriver;
    }

    public String getDatasource_password() {
        return datasource_password;
    }

    public void setDatasource_password(String datasource_password) {
        this.datasource_password = datasource_password;
    }

    public String getDatasource_databaseUrl() {
        return datasource_databaseUrl;
    }

    public void setDatasource_databaseUrl(String datasource_databaseUrl) {
        this.datasource_databaseUrl = datasource_databaseUrl;
    }

    public String getDestination_db() {
        return destination_db;
    }

    public void setDestination_db(String destination_db) {
        this.destination_db = destination_db;
    }

    public String getDestination_path() {
        return destination_path;
    }

    public void setDestination_path(String destination_path) {
        this.destination_path = destination_path;
    }

    public String getOutput_package() {
        return output_package;
    }

    public void setOutput_package(String output_package) {
        this.output_package = output_package;
    }

    public List<TableDefinition> getTables() {
        return tables;
    }

    public void setTables(List<TableDefinition> tables) {
        this.tables = tables;
    }

    public List<TableDefinition> getTables_postpone() {
        return tables_postpone;
    }

    public void setTables_postpone(List<TableDefinition> tables_postpone) {
        this.tables_postpone = tables_postpone;
    }

    public List<BaseModel> getBaseModels() {
        return baseModels;
    }

    public void setBaseModels(List<BaseModel> baseModelModels) {
        this.baseModels = baseModelModels;
    }
}
