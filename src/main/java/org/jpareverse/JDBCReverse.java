package org.jpareverse;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.jpareverse.JDBCtypes.*;


public class JDBCReverse {


    private enum DestDataBase {
        POSTGRES,
        SQLSERVER,
        ORACLE
    }

    private String className, URL, user, password, sourcePath = "";
    private Connection connection;
    private DestDataBase DestinationDb;

    JDBCReverse(String className, String URL, String user, String password) {
        this.className = className;
        this.URL = URL;
        this.user = user;
        this.password = password;
        this.connection = null;
    }

    public void setDestinationPostgres() {
        DestinationDb = DestDataBase.POSTGRES;
    }

    public void setDestinationSqlserver() {
        DestinationDb = DestDataBase.SQLSERVER;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        new File(sourcePath).mkdirs();
    }

    public void getConnection() {

        //Load the driver class
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ex) {
            System.out.println("Unable to load the class. Terminating the program");
            System.exit(-1);
        }

        //get the connection
        try {
            connection = DriverManager.getConnection(URL, user, password);
        } catch (SQLException ex) {
            System.out.println("Error getting connection: " + ex.getMessage());
            System.exit(-1);
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            System.exit(-1);
        }

        if (connection != null) {
            System.out.println("Connected Successfully!");
        }

    }

    List<TableDefinition> tableList;

    public void setTableList(List<TableDefinition> tableList) {
        this.tableList = tableList;
    }

    class TableColumn {
        String COLUMN_NAME;
        String TYPE_NAME;
        String COLUMN_SIZE;
        String DECIMAL_DIGITS;
        String IS_NULLABLE;
        String IS_AUTOINCREMENT;
        String COLUMN_DEF;
        String REMARKS;
        String JavaType;

    }

    class IKey {
        String INDEX_NAME;
        String COLUMN_NAME;
        String NON_UNIQUE;
        String ASC_OR_DESC;
        short ORDINAL_POSITION;
    }

    class FKey {
        String PKTABLE_NAME;
        String PKCOLUMN_NAME;
        String FKTABLE_NAME;
        String FKCOLUMN_NAME;
        String FK_NAME;
        short KEY_SEQ;
    }

    class GetterSetter {
        String type;
        String name;

        public GetterSetter(String name, String type) {
            this.type = type;
            this.name = name;
        }
    }

    DatabaseMetaData databaseMetaData;
    List<String> beginLines;
    List<String> mainLines;
    List<String> pkFields = new ArrayList<>();
    HashMap<String, Integer> fkNames = new HashMap<>();
    List<FKey> fKeys = new ArrayList<>();
    List<GetterSetter> getterSetters = new ArrayList<>();
    List<TableColumn> tableColumns = new ArrayList<>();
    List<TableDefinition> tablesGerar;
    String model = "Model";

    public void generateTableJpa(List<TableDefinition> tables, int idxTable) {
        tablesGerar = tables;
        String tableName = tables.get(idxTable).getAlias();
        String tablePhysical = tables.get(idxTable).getName();
        if (tableName == null || tableName.isEmpty()) {
            tableName = tablePhysical;
        }
        tableInit();
        tableColumns = new ArrayList<>();


        try {
            databaseMetaData = connection.getMetaData();
            if (!loadColumns(tablePhysical)) {
                return;
            }
            printHeader(String.format("package %s;\n", App.config.getOutput_package()));
            printHeader("import javax.persistence.*;\n");
            loadForeignKeys(tablePhysical);

            if (App.config.getEbean_annotations().equals("Y")) {
                generateEbeanIndexes(tablePhysical);
            }

            print("@Entity");
            print(String.format("@Table(name = \"%s\")", tablePhysical));
            if (App.config.getLombok().equals("Y")) {
                printHeader("import lombok.Getter;");
                printHeader("import lombok.Setter;");
                print("@Getter");
                print("@Setter");
            }

            if (App.config.getUse_model().startsWith("N")) {
                print(String.format("public class %s {\n", toCamelCase(tableName)));

            } else {
                model = "Model";
                if (checkModel()) {
                    model = "BaseModel";
                    printHeader(String.format("import %s.%s;", App.config.getBase_model_package(), model));
                } else {
                    printHeader("import io.ebean.Model;");
                }

                print(String.format("public class %s extends %s {\n", toCamelCase(tableName), model));
            }

            searchPrimaryKeys(tablePhysical);

            if (App.config.getSize_constants().equals("Y")) {
                generateSizeConstants(tablePhysical);
                print("\n\n");
            }


            generateColumns(tablePhysical);


            // Constructor
            print("\n\n");
            print(String.format("    public %s() {", toCamelCase(tableName)));
            print("    }\n\n");


            if (pkFields.size() > 1) {
                // Send compound primary key to list of get/set
                getterSetters.add(new GetterSetter(toVariable(tableName) + "PK", toCamelCase(tableName) + "PK"));
            }

            columnsGetSet(tablePhysical);

            if (App.config.getFlat_mode().equals("N")) {
                generateForeignKeys(tablePhysical);

                generateReferencedTables(tablePhysical);
            }

            if (App.config.getLombok().equals("N")) {
                // do getters/setters
                getterSetters.forEach(gs -> generateGetSet(gs.name, gs.type));
            }

            print("\n\n}\n");

            printAll(tableName, "");
            if (pkFields.size() > 1) {
                generateTablePKJpa(tablePhysical);
                printAll(tableName, "PK");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
//            System.exit(-1);
        }
    }

    String baseFields = "";
    int bfCount = 0;

    private boolean checkModel() {

        if (App.config.getBaseModels() == null) {
            return false;
        }
        if (baseFields.isEmpty()) {
            baseFields = "," + App.config.getBaseModels().get(0).fields.replace(" ", "") + ',';
            bfCount = App.config.getBaseModels().get(0).fields.split(",").length;
        }
        int count = 0;
        for (TableColumn columns : tableColumns) {
            if (baseFields.contains("," + columns.COLUMN_NAME + ",")) {
                count++;
            }
        }
        return bfCount == count;
    }

    private boolean checkFieldModel(String field) {
        if (model.equals("Model")) {
            return false;
        }

        return baseFields.contains("," + field + ",");
    }

    private boolean loadColumns(String tableName) throws SQLException {
        int cols = 0;
        ResultSet columns = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
        while (columns.next()) {
            cols++;
            TableColumn column = new TableColumn();
            column.COLUMN_NAME = columns.getString("COLUMN_NAME");
            if (sqlKeyword.contains(column.COLUMN_NAME)) {
                column.COLUMN_NAME = "\\\"" + column.COLUMN_NAME + "\\\"";
            }
            column.TYPE_NAME = columns.getString("TYPE_NAME");
            column.COLUMN_SIZE = columns.getString("COLUMN_SIZE");
            column.DECIMAL_DIGITS = columns.getString("DECIMAL_DIGITS");
            column.IS_NULLABLE = columns.getString("IS_NULLABLE");
            column.IS_AUTOINCREMENT = columns.getString("IS_AUTOINCREMENT");
            column.COLUMN_DEF = columns.getString("COLUMN_DEF");
            column.REMARKS = columns.getString("REMARKS");
            tableColumns.add(column);
        }
        if (cols == 0) {
            System.out.printf("Table not found: %s\n", tableName);
        }
        return cols > 0;
    }

    private void generateReferencedTables(String tableName) throws SQLException {
        //
        //Get Exported Keys
        ResultSet EK = databaseMetaData.getExportedKeys(null, App.config.getDatasource_schema(), tableName);
        String mine = toVariable(getTableAlias(tablesGerar, tableName));
        List<String> lists = new ArrayList<>();
        while (EK.next()) {
            String ekName = EK.getString("FKTABLE_NAME");
            if (lists.contains(ekName)) {
                continue;
            }
            if (!getTableName(tableList, ekName)) {
                System.out.printf(" ->Sem refer; %s\n", ekName);
                continue;
            }
            lists.add(ekName);
            ekName = getTableAlias(tablesGerar, ekName);
            print(String.format("    @OneToMany(cascade = CascadeType.ALL, mappedBy = \"%s\", fetch = FetchType.LAZY)", mine));
            print(String.format("    private List<%s> %sList;", toCamelCase(ekName), toVariable(ekName)));
            getterSetters.add(new GetterSetter(toVariable(ekName) + "List", String.format("List<%s>", toCamelCase(ekName))));
            printHeader(SqlTypesImport.get("List"));
        }
        print("\n\n");
    }

    private void loadForeignKeys(String tableName) throws SQLException {
        //
        //Get Foreign Keys
        ResultSet FK = databaseMetaData.getImportedKeys(null, App.config.getDatasource_schema(), tableName);
        fkNames = new HashMap<>();

        while (FK.next()) {
            String pkTable = FK.getString("PKTABLE_NAME");
            if (!getTableName(tablesGerar, pkTable)) {
                continue;
            }
            Integer cont = fkNames.get(FK.getString("FK_NAME"));
            if (cont == null) {
                fkNames.put(FK.getString("FK_NAME"), 1);
            } else {
                fkNames.put(FK.getString("FK_NAME"), ++cont);
            }
            FKey fkey = new FKey();
            fkey.FKCOLUMN_NAME = FK.getString("FKCOLUMN_NAME");
            if (sqlKeyword.contains(fkey.FKCOLUMN_NAME)) {
                fkey.FKCOLUMN_NAME = "\\\"" + fkey.FKCOLUMN_NAME + "\\\"";
            }
            fkey.FK_NAME = FK.getString("FK_NAME");
            fkey.FKTABLE_NAME = FK.getString("FKTABLE_NAME");
            fkey.KEY_SEQ = FK.getShort("KEY_SEQ");
            fkey.PKCOLUMN_NAME = FK.getString("PKCOLUMN_NAME");
            if (sqlKeyword.contains(fkey.PKCOLUMN_NAME)) {
                fkey.PKCOLUMN_NAME = "\\\"" + fkey.PKCOLUMN_NAME + "\\\"";
            }
            fkey.PKTABLE_NAME = FK.getString("PKTABLE_NAME");
            fKeys.add(fkey);

        }
    }

    private void generateForeignKeys(String tableName) throws SQLException {
        HashMap<String, Integer> usedFkVars = new HashMap<>();
        List<String> oneFieldFks = new ArrayList<>();
        for (String fkName : fkNames.keySet()) {
            int cont = fkNames.get(fkName);
            if (cont == 1) {
                FKey fkey = getFkfield(fKeys, fkName, 1);
                if (checkFieldModel(fkey.PKCOLUMN_NAME)) {
                    continue;
                }
                oneFieldFks.add(fkey.PKCOLUMN_NAME);
            }
        }

        for (String fkName : fkNames.keySet()) {
            int cont = fkNames.get(fkName);
            FKey fkey = null;
            if (cont == 1) {
                fkey = getFkfield(fKeys, fkName, 1);
                if (checkFieldModel(fkey.PKCOLUMN_NAME)) {
                    continue;
                }
                String colname = fkey.FKCOLUMN_NAME;
                String insertable = pkFields.contains(colname) ? ", insertable = false, updatable = false" : "";

                print(String.format("    @JoinColumn(name = \"%s\", referencedColumnName = \"%s\"%s)",
                        colname, fkey.PKCOLUMN_NAME, insertable));
            } else {
                print("    @JoinColumns({");
                for (int seq = 1; seq <= cont; seq++) {
                    fkey = getFkfield(fKeys, fkName, seq);
                    String insertable = pkFields.contains(fkey.FKCOLUMN_NAME) ? ", insertable = false, updatable = false" : "";
                    if (insertable.isEmpty()) {
                        insertable = oneFieldFks.contains(fkey.FKCOLUMN_NAME) ? ", insertable = false, updatable = false" : "";
                    }
                    if (checkFieldModel(fkey.FKCOLUMN_NAME)) {
                        insertable = ", insertable = false, updatable = false";
                    }

                    String comma = seq == cont ? "})" : ",";
                    print(String.format("      @JoinColumn(name = \"%s\", referencedColumnName = \"%s\"%s)%s",
                            fkey.FKCOLUMN_NAME, fkey.PKCOLUMN_NAME, insertable, comma));

                }

            }

            String varName = toVariable(getTableAlias(tablesGerar, fkey.PKTABLE_NAME));
            if (usedFkVars.get(varName) == null) {
                usedFkVars.put(varName, 1);
            } else {
                Integer idx = usedFkVars.get(varName);
                usedFkVars.put(varName, ++idx);
                varName = varName + idx;
            }
//            print("    @ManyToOne(optional = false, fetch = FetchType.LAZY)");
            print("    @ManyToOne(fetch = FetchType.LAZY)");
            print(String.format("    private %s %s;\n", toCamelCase(getTableAlias(tablesGerar, fkey.PKTABLE_NAME)), varName));
            getterSetters.add(new GetterSetter(varName, toCamelCase(getTableAlias(tablesGerar, fkey.PKTABLE_NAME))));
        }
        print("\n");

    }


    private void columnsGetSet(String tableName) throws SQLException {
        //
        //get columns Getter/Setter
        for (TableColumn columns : tableColumns) {
            String columnName = columns.COLUMN_NAME;
            if (pkFields.size() > 1 && pkFields.contains(columnName)) {
                continue;
            }
            if (App.config.getFlat_mode().equals("N") && null != getFkfieldName(fKeys, columnName)) {
                continue;
            }

            if (checkFieldModel(columnName)) {
                continue;
            }
            String datatype = columns.TYPE_NAME;
            String isNullable = columns.IS_NULLABLE;
            String defaultval = columns.COLUMN_DEF;

            String javaType = columns.JavaType; //SqlJavaTypes.get(datatype + (isNullable.equals("YES") ? "-null" : ""));
            if (javaType == null) {
                System.out.printf("Type not found: %s", datatype);
                System.exit(1);
            }
            /*if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("String")
                    && defaultval != null && defaultval.contains("getdate")) {
                javaType = "Date";
            }*/
            if (javaKeyword.contains(columnName)) {
                columnName += "1";
            }
            getterSetters.add(new GetterSetter(toVariable(columnName.toLowerCase()), javaType));

        }

    }

    private void generateSizeConstants(String tableName) throws SQLException {
        for (TableColumn column : tableColumns) {
            String columnName = column.COLUMN_NAME.replace("\\\"", "");
            String datatype = column.TYPE_NAME;
            String columnsize = column.COLUMN_SIZE;
            String javaType = SqlJavaTypes.get(datatype);

            if (SqlSizeTypes.contains(javaType)) {
                print(String.format("    public static final int %s_SIZE = %s;",
                        columnName.toUpperCase(), columnsize));

            }
        }
    }

    /**
     *
     */
    private void generateColumns(String tableName) throws SQLException {
        //
        //get columns
        for (TableColumn column : tableColumns) {

            String columnName = column.COLUMN_NAME;
            String datatype = column.TYPE_NAME;
            String columnsize = column.COLUMN_SIZE;
            String decimaldigits = column.DECIMAL_DIGITS;
            String isNullable = column.IS_NULLABLE;
            String is_autoIncrment = column.IS_AUTOINCREMENT;
            String defaultval = column.COLUMN_DEF;
            String remarks = column.REMARKS;


            if (pkFields.contains(columnName)) {
                if (pkFields.size() == 1) {
                    if (NUM_TYPES.contains(column.TYPE_NAME)) {
                        print("    @Id");
                        defaultval = "";
                    }
                } else {
                    continue;
                }
            }
            if (App.config.getFlat_mode().equals("N") && null != getFkfieldName(fKeys, columnName)) {
                continue;
            }

            if (checkFieldModel(columnName)) {
                continue;
            }

            if (App.config.getFlat_mode().equals("N") && null != getFkfieldName(fKeys, columnName)) {
                isNullable = "YES";
                defaultval = "";
            }
            //Printin(isNullable.equals("YES") ? "-null" : "")g results

            String javaType = SqlJavaTypes.get(datatype + (isNullable.equals("YES") ? "-null" : ""));
            if (javaType == null) {
                System.out.printf("Datatype [%s] não encontrado", datatype);
                System.exit(1);
            }

            String nullable = "";
            if (isNullable.equals("NO")) {
                if (javaType.equals("Date")
                        && defaultval != null && defaultval.equals("('')")) {
                    defaultval = null;
                } else {
                    nullable = ", nullable = false";
                }
            }

            String size = "";
            if (SqlSizeTypes.contains(javaType)) {
                if (Long.parseLong(columnsize) >= 10485760L) {
                    size = ",columnDefinition = \"text\"";
                } else {
                    size = String.format(", length=%s", columnsize);
                }
            }
            if (SqlDecimalTypes.contains(javaType)) {
                size = String.format(", precision=%s, scale=%s", columnsize, decimaldigits);
            }
            String upsert = "";
            if (",bigint identity,bigserial,)".contains("," + datatype + ",") && !pkFields.contains(columnName)) {
                nullable = "";
                upsert = ", insertable = false, updatable = false";
                if (DestinationDb == DestDataBase.POSTGRES) {
                    upsert += ",columnDefinition = \"bigserial\"";
                }
            }

            if (defaultval != null) {
                if (defaultval.contains("getdate()") ||
                        (javaType.equals("String") &&
                                (columnName.startsWith("dat_") || columnName.startsWith("dta_"))
                                || columnName.startsWith("data_") || columnName.contains("date"))
                        || columnName.equals("dvenc")) {
                    if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("String")) {
                        size = "";
                    }
                } else if (datatype.equals("bigserial")) {
                    defaultval = "";
                } else if (defaultval.contains("::")) {
                    defaultval = "$RAW:" + defaultval;

                } else if (defaultval.contains("CURRENT_TIMESTAMP") || defaultval.contains("now")) {
                    defaultval = "$RAW:now()"; //"now()";
                }

            }


            String colname = columnName;
            /*if (sqlKeyword.contains(colname)) {
                   colname = "\""+colname+"\"";
            }*/
            if (datatype.startsWith("jsonb")) {
                printHeader(SqlTypesImport.get("DbJsonB"));
                printHeader("import java.util.Map;");
                print("    @DbJsonB");
            } else if (datatype.startsWith("json")) {
                printHeader(SqlTypesImport.get("DbJson"));
                printHeader("import java.util.Map;");
                print("    @DbJson");
            }
            if (colname.equals(App.config.getVersion_field())) {
                print("    @Version");
            }
            print(String.format("    @Column(name = \"%s\"%s%s%s)", colname, nullable, size, upsert));
            if (javaType.equals("String") &&
                    (columnName.startsWith("dat_") || columnName.startsWith("dta_"))
                    || columnName.startsWith("data_") || columnName.contains("date")
                    || columnName.equals("dvenc")) {
                if (DestinationDb == DestDataBase.POSTGRES && App.config.getEbean_annotations().equals("Y")
                        && javaType.equals("String")) {
                    javaType = "Date";//"LocalDateTime";
                    if (column.IS_NULLABLE.equals("NO")) {
                        defaultval = "$RAW:now()"; //"now";
                        printHeader(SqlTypesImport.get("DbDefault"));
                        print(String.format("    @DbDefault(\"%s\")",
                                defaultval.replace("\"", "\\\"")));
                    }
                }
            } else if (defaultval != null) {
                if (defaultval.startsWith("((")) {
                    defaultval = defaultval.substring(2);
                }
                if (defaultval.endsWith("))")) {
                    defaultval = defaultval.substring(0, defaultval.length() - 2);
                }
                if (defaultval.startsWith("(")) {
                    defaultval = defaultval.substring(1);
                }
                if (defaultval.endsWith(")")) {
                    defaultval = defaultval.substring(0, defaultval.length() - 1);
                }
                if (defaultval.endsWith("(")) {
                    defaultval += ")";
                }
                if (javaType.equalsIgnoreCase("boolean")) {
                    if (defaultval.equals("0")) {
                        defaultval = "false";
                    }
                    if (defaultval.equals("1")) {
                        defaultval = "true";
                    }
                }
                if (defaultval.equals("''") && NUM_TYPES.contains(datatype)) {
                    defaultval = "0";

                }

                if (defaultval.equals("CONVERT([nchar],getdate(),(120")) {
                    if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("String")
                            && defaultval != null && defaultval.contains("getdate")) {
                        javaType = "Date";//"LocalDateTime";
                        //defaultval = "to_char(now(), 'YYYY-MM-DD HH24:MI:SS')";
                        defaultval = "$RAW:now()"; //"now";
                    }
                }
                if (defaultval.equals("getdate(")) {
                    if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("Date")
                            && defaultval != null && defaultval.contains("getdate")) {
                        defaultval = "$RAW:now()";
                    }
                }
                //if ("dat_|dta_|data_|date")

                if (DestinationDb == DestDataBase.POSTGRES && !defaultval.isEmpty() && App.config.getEbean_annotations().equals("Y")) {
                    printHeader(SqlTypesImport.get("DbDefault"));
                    print(String.format("    @DbDefault(\"%s\")",
                            defaultval.replace("\"", "\\\"")));
                }
            }
            if (remarks != null && App.config.getEbean_annotations().equals("Y")) {
                printHeader(SqlTypesImport.get("DbComment"));
                print(String.format("    @DbComment(\"%s\")", remarks));
            }
            if (SqlTypesAnnotation.get(datatype) != null) {
                print(String.format("    %s", SqlTypesAnnotation.get(datatype)));
            }
            printHeader(SqlTypesImport.get(javaType));
            if (javaKeyword.contains(columnName)) {
                columnName += "1";
            }
            print(String.format("    private %s %s;\n", javaType, toVariable(columnName.toLowerCase())));
            column.JavaType = javaType;
        }
    }

    /**
     *
     */
    private void searchPrimaryKeys(String tableName) throws SQLException {
        //
        //GetPrimarykeys
        ResultSet PK = databaseMetaData.getPrimaryKeys(null, App.config.getDatasource_schema(), tableName);
        while (PK.next()) {
            String pkColName = PK.getString("COLUMN_NAME");
            if (sqlKeyword.contains(pkColName)) {
                pkColName = "\\\"" + pkColName + "\\\"";
            }
            pkFields.add(pkColName);
        }

        if (pkFields.size() > 1) {
            print("    @EmbeddedId");
            print(String.format("    protected %sPK %sPK;\n", toCamelCase(getTableAlias(tablesGerar, tableName)), toVariable(getTableAlias(tablesGerar, tableName))));
        }
    }

    /**
     *
     */
    private void generateEbeanIndexes(String tableName) {
        //
        //Get Index Keys
        List<IKey> keys = new ArrayList<>();
        HashMap<String, Integer> indexes = new HashMap<>();
        ResultSet IX = null;
        try {
            IX = databaseMetaData.getIndexInfo(null, App.config.getDatasource_schema(), tableName, false, false);
//            System.out.println("------------INDEX KEYS-------------");
            while (IX.next()) {
                String indexName = IX.getString("INDEX_NAME");
                if (indexName == null || indexName.contains("PK")) {
                    continue;
                }
                Integer cont = indexes.get(indexName);
                if (cont == null) {
                    indexes.put(indexName, 1);
                } else {
                    indexes.put(indexName, ++cont);
                }
                IKey key = new IKey();
                key.ASC_OR_DESC = IX.getString("ASC_OR_DESC");
                key.COLUMN_NAME = IX.getString("COLUMN_NAME");
                if (sqlKeyword.contains(key.COLUMN_NAME)) {
                    key.COLUMN_NAME = "\\\"" + key.COLUMN_NAME + "\\\"";
                }
                key.INDEX_NAME = indexName;
                key.NON_UNIQUE = IX.getString("NON_UNIQUE");
                key.ORDINAL_POSITION = IX.getShort("ORDINAL_POSITION");
                keys.add(key);
//                System.out.println(IX.getString("INDEX_NAME") + "---" + IX.getString("COLUMN_NAME") + "===" + IX.getString("ORDINAL_POSITION") + "---" + IX.getString("NON_UNIQUE") + "---" + IX.getString("ASC_OR_DESC"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (String indexName : indexes.keySet()) {
            printHeader("import io.ebean.annotation.Index;");
            int cont = indexes.get(indexName);
            String idx = "";

            for (int seq = 1; seq <= cont; seq++) {
                IKey key = getIxfield(keys, indexName, seq);
                if (seq > 1) {
                    idx += ",";
                }
                idx += String.format("\"%s\"", key.COLUMN_NAME);
            }
            print(String.format("@Index(name = \"%s\", columnNames = {%s})", indexName, idx));
        }

    }


    public void generateTablePKJpa(String tableName) {
        tableInit();
        printHeader(String.format("package %s;\n", App.config.getOutput_package()));
        printHeader("import javax.persistence.*;\n");
        print("@Embeddable");
        print(String.format("public class %sPK {\n", toCamelCase(getTableAlias(tablesGerar, tableName))));
        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            //GetPrimarykeys
            ResultSet PK = databaseMetaData.getPrimaryKeys(null, App.config.getDatasource_schema(), tableName);
//            System.out.println("------------PRIMARY KEYS-------------");
            while (PK.next()) {
                String pkColName = PK.getString("COLUMN_NAME");
                if (sqlKeyword.contains(pkColName)) {
                    pkColName = "\\\"" + pkColName + "\\\"";
                }
                pkFields.add(pkColName);
            }


            //get columns
            if (App.config.getSize_constants().equals("Y")) {
                ResultSet column = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
                while (column.next()) {
                    String columnName = column.getString("COLUMN_NAME");
                    if (sqlKeyword.contains(columnName)) {
                        columnName = "\\\"" + columnName + "\\\"";
                    }
                    if (!pkFields.contains(columnName)) {
                        continue;
                    }
                    String datatype = column.getString("TYPE_NAME");
                    String columnsize = column.getString("COLUMN_SIZE");

                    String javaType = SqlJavaTypes.get(datatype);

                    if (SqlSizeTypes.contains(javaType)) {
                        print(String.format("    public static final int %s_SIZE = %s;",
                                columnName.toUpperCase().replace("\\\"", ""), columnsize));

                    }
                }
                print("\n\n");
            }


            //get columns
            ResultSet columns = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (sqlKeyword.contains(columnName)) {
                    columnName = "\\\"" + columnName + "\\\"";
                }
                if (!pkFields.contains(columnName)) {
                    continue;
                }
                String datatype = columns.getString("TYPE_NAME");
                String columnsize = columns.getString("COLUMN_SIZE");
                String decimaldigits = columns.getString("DECIMAL_DIGITS");
                String isNullable = columns.getString("IS_NULLABLE");
                String is_autoIncrment = columns.getString("IS_AUTOINCREMENT");
                String remarks = columns.getString("REMARKS");

                //Printing results
//                System.out.println(columnName + "---" + datatype + "---" + columnsize + "---"
//                        + decimaldigits + "---" + isNullable + "---" + is_autoIncrment + "---" + defaultval);
                String nullable = "";
                if (isNullable.equals("NO")) {
                    nullable = ", nullable = false";
                }

                String size = "";
                String javaType = SqlJavaTypes.get(datatype + (isNullable.equals("YES") ? "-null" : ""));
                assert (javaType != null);

                if (SqlSizeTypes.contains(javaType)) {
                    size = String.format(", length=%s", columnsize);
                }
                String colname = columnName;
                if (sqlKeyword.contains(colname)) {
                    colname = "\\\"" + colname + "\\\"";
                }
                print(String.format("    @Column(name = \"%s\"%s%s)", colname, nullable, size));
                /*if (remarks != null) {
                    print(String.format("    @DbComment(\"%s\")", remarks));
                } ## verificar*/
                printHeader(SqlTypesImport.get(javaType));
                print(String.format("    private %s %s;\n", javaType, toVariable(columnName)));
            }

            print("\n\n");
            //
            print(String.format("    public %sPK() {", toCamelCase(getTableAlias(tablesGerar, tableName))));
            print("    }\n\n");


            //get columns
            StringBuilder fieldsParam = new StringBuilder();
            StringBuilder fieldsAssign = new StringBuilder();
            columns = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (sqlKeyword.contains(columnName)) {
                    columnName = "\\\"" + columnName + "\\\"";
                }
                if (!pkFields.contains(columnName)) {
                    continue;
                }
                String datatype = columns.getString("TYPE_NAME");
                String isNullable = columns.getString("IS_NULLABLE");
                String defaultval = columns.getString("COLUMN_DEF");

                String javaType = SqlJavaTypes.get(datatype + (isNullable.equals("YES") ? "-null" : ""));
                if (javaType == null) {
                    System.out.printf("PK Datatype [%s] não encontrado", datatype);
                    System.exit(1);
                }

                if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("String")
                        && defaultval != null && defaultval.contains("getdate")) {
                    javaType = "Date"; //"LocalDateTime";
                }
                //generateGetSet(toVariable(columnName), javaType);
                if (fieldsParam.length() > 0) {
                    fieldsParam.append(", ");
                }
                fieldsParam.append(javaType).append(" ").append(toVariable(columnName));
                fieldsAssign.append("         this.").append(toVariable(columnName))
                        .append(" = ").append(toVariable(columnName)).append(";\n");
            }

            print(String.format("    public %sPK(%s) {", toCamelCase(getTableAlias(tablesGerar, tableName)), fieldsParam));
            print(String.format("%s    }\n\n", fieldsAssign));

            //get columns
            columns = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (sqlKeyword.contains(columnName)) {
                    columnName = "\\\"" + columnName + "\\\"";
                }
                if (!pkFields.contains(columnName)) {
                    continue;
                }
                String datatype = columns.getString("TYPE_NAME");
                String isNullable = columns.getString("IS_NULLABLE");
                String defaultval = columns.getString("COLUMN_DEF");

                String javaType = SqlJavaTypes.get(datatype + (isNullable.equals("YES") ? "-null" : ""));
                if (javaType == null) {
                    System.out.printf("PK Datatype [%s] não encontrado", datatype);
                    System.exit(1);
                }

                if (DestinationDb == DestDataBase.POSTGRES && javaType.equals("String")
                        && defaultval != null && defaultval.contains("getdate")) {
                    javaType = "Date"; //"LocalDateTime";
                }
                generateGetSet(toVariable(columnName), javaType);
            }

            generateHashEquals(toCamelCase(getTableAlias(tablesGerar, tableName)) + "PK", pkFields);
            print("\n}");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        }
    }


    private void generateHashEquals(String pkName, List<String> pkFields) {
        print("    @Override\n" +
                "    public int hashCode() {\n" +
                "        int hash = 0;");
        for (String field : pkFields) {
            field = field.replace("\n", "");
            TableColumn col = getTableField(field);
            if (NUM_TYPES.contains(col.TYPE_NAME)) {
                print(String.format("        hash += (int) %s;", toVariable(field)));
            } else {
                print(String.format("        hash += (%s != null ? %s.hashCode() : 0);",
                        toVariable(field), toVariable(field)));
            }
        }
        print("        return hash;\n" +
                "    }\n");

        print(String.format("    @Override\n" +
                "    public boolean equals(Object object) {\n" +
                "        if (!(object instanceof %s)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        %s other = (%s) object;\n", pkName, pkName, pkName));
        for (String field : pkFields) {
            TableColumn col = getTableField(field);
            field = field.replace("\n", "");
            String varName = toVariable(field);
            if (NUM_TYPES.contains(col.TYPE_NAME)) {
                print(String.format("        if (this.%s != other.%s) {\n" +
                        "            return false;\n" +
                        "        }", varName, varName));
            } else {
                print(String.format("       if ((this.%s == null && other.%s != null) || (this.%s != null && !this.%s.equals(other.%s))) {\n" +
                                "                return false;\n" +
                                "                }\n",
                        varName, varName, varName, varName, varName));
            }
        }

        print("        return true;\n" +
                "}\n");

    }

    /* @Override
    public boolean equals(Object object) {
        if (!(object instanceof SivaSysWssUsuarioPK)) {
            return false;
        }
        SivaSysWssUsuarioPK other = (SivaSysWssUsuarioPK) object;
        if (this.codigoEmpresa != other.codigoEmpresa) {
            return false;
        }
        if ((this.usuario == null && other.usuario != null) || (this.usuario != null && !this.usuario.equals(other.usuario))) {
            return false;
        }
        return true;
    }*/

    private void generateGetSet(String nomeSql, String tipoJ) {

        nomeSql = nomeSql.replace("\"", "");
        print("\tpublic " + tipoJ + " get" + toUpperCaseFirst(nomeSql) + "() { "
                + "return " + nomeSql + "; }\n");
        String linha = "\tpublic void set" + toUpperCaseFirst(nomeSql) + "(";
        linha += tipoJ + " " + nomeSql + ") {";
        linha += " this." + nomeSql + " = " + nomeSql + "; }\n";
        print(linha);


    }

    private static String toUpperCaseFirst(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1);
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    private static String toCamelCase(String s) {
        String[] parts = s.split("_");
        if (parts.length == 1) {
            return toProperCase(s);
        }
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }


    private static String toVariable(String s) {
        s = toCamelCase(s.replace("\\\"", ""));
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private void tableInit() {
        beginLines = new ArrayList<>();
        mainLines = new ArrayList<>();
        pkFields = new ArrayList<>();
        getterSetters = new ArrayList<>();
        fkNames = new HashMap<>();
        fKeys = new ArrayList<>();
    }

    private IKey getIxfield(List<IKey> iKeys, String indexName, int seq) {
        for (IKey fk : iKeys) {
            if (fk.INDEX_NAME.equals(indexName) && fk.ORDINAL_POSITION == seq) {
                return fk;
            }
        }
        return null;
    }

    private FKey getFkfield(List<FKey> fKeys, String pkTableName, int seq) {
        for (FKey fk : fKeys) {
            if (fk.FK_NAME.equals(pkTableName) && fk.KEY_SEQ == seq) {
                return fk;
            }
        }
        return null;
    }

    private FKey getFkfieldName(List<FKey> fKeys, String name) {
        for (FKey fk : fKeys) {
            if (fk.FKCOLUMN_NAME.equals(name)) {
                return fk;
            }
        }
        return null;
    }

    private boolean getTableName(List<TableDefinition> tableList, String name) {
        for (TableDefinition table : tableList) {
            if (table.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String getTableAlias(List<TableDefinition> tableList, String name) {
        for (TableDefinition table : tableList) {
            if (table.getName().equals(name)) {
                return table.getAlias();
            }
        }
        return name;
    }

    private TableColumn getTableField(String fieldName) {
        for (TableColumn col : tableColumns) {
            if (col.COLUMN_NAME.equals(fieldName)) {
                return col;
            }
        }
        return null;
    }

    private void print(String p) {
        if (p == null) {
            return;
        }
        mainLines.add(p);
//        System.out.println(p);
    }

    private void printHeader(String p) {
        if (p == null) {
            return;
        }
        if (!p.isEmpty() || !p.equals("\n")) {
            if (!beginLines.contains(p)) {
                beginLines.add(p);
            }
        }
//        System.out.println(p);
    }

    private void printAll(String fileName, String PK) {

//        beginLines.forEach(l -> System.out.println(l));
//        System.out.println("\n");
//        mainLines.forEach(l -> System.out.println(l));
        String filepath = sourcePath + "/" + toCamelCase(fileName) + PK + ".java";
        FileWriter fileWriter = null;
        try {
            new File(filepath).delete();
            fileWriter = new FileWriter(filepath);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            beginLines.forEach(l -> printWriter.println(l));
            printWriter.println("\n");
            mainLines.forEach(l -> printWriter.println(l));
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTablesList(String start) {
        DatabaseMetaData databaseMetaData = null;
        List<String> tablesList = new ArrayList<>();
        //Fetching Database Metadata from connection
        try {
            databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, App.config.getDatasource_schema(), null, new String[]{"TABLE"});
            while (resultSet.next()) {
                if (!resultSet.getString("TABLE_NAME").toLowerCase().startsWith(start)) {
                    continue;
                }

                tablesList.add(resultSet.getString("TABLE_NAME"));
            }
        } catch (SQLException ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        } catch (Exception ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        }

        return tablesList;
    }

    public void listAllTables() {
        DatabaseMetaData databaseMetaData = null;

        //Fetching Database Metadata from connection
        try {
            databaseMetaData = connection.getMetaData();


            //Print TABLE_TYPE "TABLE"
            ResultSet resultSet = databaseMetaData.getTables(null, App.config.getDatasource_schema(), null, new String[]{"TABLE"});
            System.out.println("Printing TABLE_TYPE \"TABLE\" ");
            System.out.println("----------------------------------");
            while (resultSet.next()) {
                //Print
                System.out.println(resultSet.getString("TABLE_NAME"));
                showTableMetaData(resultSet.getString("TABLE_NAME"));
            }

            //Print TABLE_TYPE "SYSTEM TABLE"
            /*resultSet = databaseMetaData.getTables(null, null, null, new String[]{"SYSTEM TABLE"});
            System.out.println("Printing TABLE_TYPE \"SYSTEM TABLE\" ");
            System.out.println("----------------------------------");
            while(resultSet.next())
            {
                //Print
                System.out.println(resultSet.getString("TABLE_NAME"));
            }*/

            //Print TABLE_TYPE "VIEW"
            resultSet = databaseMetaData.getTables(null, App.config.getDatasource_schema(), null, new String[]{"VIEW"});
            System.out.println("Printing TABLE_TYPE \"VIEW\" ");
            System.out.println("----------------------------------");
            while (resultSet.next()) {
                //Print
                System.out.println(resultSet.getString("TABLE_NAME"));
            }


        } catch (SQLException ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        } catch (Exception ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        }

    }

    public List<String> getAllTables() {
        DatabaseMetaData databaseMetaData = null;

        List<String> allTables = new ArrayList<>();
        //Fetching Database Metadata from connection
        try {
            databaseMetaData = connection.getMetaData();


            //Print TABLE_TYPE "TABLE"
            ResultSet resultSet = databaseMetaData.getTables(null, App.config.getDatasource_schema(), null, new String[]{"TABLE"});
            while (resultSet.next()) {
                //Print
                allTables.add(resultSet.getString("TABLE_NAME"));
            }

        } catch (Exception ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        }

        return allTables;
    }


    public void showTableMetaData(String tableName) {
        try {
            System.out.println("\n\n==================== " + tableName + " =================");

            DatabaseMetaData databaseMetaData = connection.getMetaData();

            //get columns
            ResultSet columns = databaseMetaData.getColumns(null, App.config.getDatasource_schema(), tableName, null);
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String datatype = columns.getString("TYPE_NAME");
                String columnsize = columns.getString("COLUMN_SIZE");
                String decimaldigits = columns.getString("DECIMAL_DIGITS");
                String isNullable = columns.getString("IS_NULLABLE");
                String is_autoIncrment = columns.getString("IS_AUTOINCREMENT");
                String defaultval = columns.getString("COLUMN_DEF");

                //Printing results
                System.out.println(columnName + "---" + datatype + "---" + columnsize + "---"
                        + decimaldigits + "---" + isNullable + "---" + is_autoIncrment + "---" + defaultval);

            }

            //GetPrimarykeys
            ResultSet PK = databaseMetaData.getPrimaryKeys(null, App.config.getDatasource_schema(), tableName);
            System.out.println("------------PRIMARY KEYS-------------");
            while (PK.next()) {
                System.out.println(PK.getString("COLUMN_NAME") + "===" + PK.getString("PK_NAME"));
            }

            //Get Foreign Keys
            ResultSet FK = databaseMetaData.getImportedKeys(null, App.config.getDatasource_schema(), tableName);
            System.out.println("------------FOREIGN KEYS-------------");
            while (FK.next()) {
                System.out.println(FK.getString("PKTABLE_NAME") + "---" + FK.getString("PKCOLUMN_NAME") + "===" + FK.getString("FKTABLE_NAME") + "---" + FK.getString("FKCOLUMN_NAME") + "---" + FK.getString("KEY_SEQ") + "---" + FK.getString("FK_NAME"));
            }

            //Get Exported Keys
            ResultSet EK = databaseMetaData.getExportedKeys(null, App.config.getDatasource_schema(), tableName);
            System.out.println("------------EXPORTED KEYS-------------");
            while (EK.next()) {
                System.out.println(EK.getString("PKTABLE_NAME") + "---" + EK.getString("PKCOLUMN_NAME") + "===" + EK.getString("FKTABLE_NAME") + "---" + EK.getString("FKCOLUMN_NAME"));
            }

            //Get Index Keys
            ResultSet IX = databaseMetaData.getIndexInfo(null, App.config.getDatasource_schema(), tableName, false, false);
            System.out.println("------------INDEX KEYS-------------");
            while (IX.next()) {
                if (IX.getString("INDEX_NAME") == null) {
                    continue;
                }
                System.out.println(IX.getString("INDEX_NAME") + "---" + IX.getString("COLUMN_NAME") + "===" + IX.getString("ORDINAL_POSITION") + "---" + IX.getString("NON_UNIQUE") + "---" + IX.getString("ASC_OR_DESC"));
            }

        } catch (SQLException ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        } catch (Exception ex) {
            System.out.println("Error while fetching metadata. Terminating program.. " + ex.getMessage());
            System.exit(-1);
        }
    }


}
