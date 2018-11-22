package org.jpareverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JDBCtypes {

    protected static final String NUM_TYPES =
            "short,Short,int,Integer,long,Long,smallint, bigint, bigint identity," +
                    "int2,int4,int8,bigserial";


    protected static HashMap<String, String> SqlJavaTypes = new HashMap<>();

    static {
        SqlJavaTypes.put("char", "String");
        SqlJavaTypes.put("char-null", "String");
        SqlJavaTypes.put("nchar", "String");
        SqlJavaTypes.put("nchar-null", "String");
        SqlJavaTypes.put("ntext", "String");
        SqlJavaTypes.put("ntext-null", "String");
        SqlJavaTypes.put("nvarchar", "String");
        SqlJavaTypes.put("nvarchar-null", "String");
        SqlJavaTypes.put("varchar", "String");
        SqlJavaTypes.put("varchar-null", "String");
        SqlJavaTypes.put("image", "String");
        SqlJavaTypes.put("image-null", "String");
        SqlJavaTypes.put("int", "int");
        SqlJavaTypes.put("int identity", "int");
        SqlJavaTypes.put("int-null", "Integer");
        SqlJavaTypes.put("int2", "short");
        SqlJavaTypes.put("int2-null", "Short");
        SqlJavaTypes.put("int4", "int");
        SqlJavaTypes.put("int4-null", "Integer");
        SqlJavaTypes.put("int8", "long");
        SqlJavaTypes.put("int8-null", "Long");
        SqlJavaTypes.put("serial", "long");
        SqlJavaTypes.put("bigserial", "long");
        SqlJavaTypes.put("bigint", "long");
        SqlJavaTypes.put("bigint identity", "long");
        SqlJavaTypes.put("bigint-null", "Long");
        SqlJavaTypes.put("float", "double");
        SqlJavaTypes.put("float-null", "Double");
        SqlJavaTypes.put("datetime", "Date");
        SqlJavaTypes.put("datetime-null", "Date");
        SqlJavaTypes.put("time", "Date");
        SqlJavaTypes.put("time-null", "Date");
        SqlJavaTypes.put("tinyint", "short");
        SqlJavaTypes.put("tinyint-null", "Short");
        SqlJavaTypes.put("decimal", "BigDecimal");
        SqlJavaTypes.put("decimal-null", "BigDecimal");
        SqlJavaTypes.put("numeric", "BigDecimal");
        SqlJavaTypes.put("numeric-null", "BigDecimal");
        SqlJavaTypes.put("date", "Date");
        SqlJavaTypes.put("date-null", "Date");
        SqlJavaTypes.put("smallint", "short");
        SqlJavaTypes.put("smallint-null", "Short");
        SqlJavaTypes.put("bit", "boolean");
        SqlJavaTypes.put("bit-null", "Boolean");
        SqlJavaTypes.put("uniqueidentifier-null", "String");
        SqlJavaTypes.put("timestamp", "Date");
        SqlJavaTypes.put("timestamp-null", "Date");
        SqlJavaTypes.put("timestamptz", "Date");
        SqlJavaTypes.put("timestamptz-null", "Date");
        SqlJavaTypes.put("jsonb", "Map<String,Object>");
        SqlJavaTypes.put("jsonb-null", "Map<String,Object>");

        SqlJavaTypes.put("NUMBER", "BigDecimal");
        SqlJavaTypes.put("NUMBER-null", "BigDecimal");
        SqlJavaTypes.put("VARCHAR2", "String");
        SqlJavaTypes.put("VARCHAR2-null", "String");
        SqlJavaTypes.put("DATE", "Date");
        SqlJavaTypes.put("DATE-null", "Date");

        SqlJavaTypes.put("float8", "double");
        SqlJavaTypes.put("bool", "boolean");
        SqlJavaTypes.put("bool-null", "Boolean");
        SqlJavaTypes.put("text", "String");
        SqlJavaTypes.put("text-null", "String");
    }

    protected static List<String> SqlSizeTypes = new ArrayList<>();

    static {
        SqlSizeTypes.add("String");
        SqlSizeTypes.add("");
        SqlSizeTypes.add("");
        SqlSizeTypes.add("");
        SqlSizeTypes.add("");
        SqlSizeTypes.add("");
        SqlSizeTypes.add("");
    }

    protected static List<String> SqlDecimalTypes = new ArrayList<>();

    static {
        SqlDecimalTypes.add("BigDecimal");
        SqlDecimalTypes.add("");
        SqlDecimalTypes.add("");
        SqlDecimalTypes.add("");
        SqlDecimalTypes.add("");
        SqlDecimalTypes.add("");
        SqlDecimalTypes.add("");
    }

    protected static HashMap<String, String> SqlTypesAnnotation = new HashMap<>();

    static {
        SqlTypesAnnotation.put("time", "@Temporal(TemporalType.TIME)");
        SqlTypesAnnotation.put("date", "@Temporal(TemporalType.DATE)");
        SqlTypesAnnotation.put("datetime", "@Temporal(TemporalType.TIMESTAMP)");
        SqlTypesAnnotation.put("timestamp", "@Temporal(TemporalType.TIMESTAMP)");
    }

    protected static HashMap<String, String> SqlTypesImport = new HashMap<>();

    static {
        SqlTypesImport.put("Date", "import java.util.Date;");
        SqlTypesImport.put("BigDecimal", "import java.math.BigDecimal;");
        SqlTypesImport.put("List", "import java.util.List;");
        SqlTypesImport.put("DbDefault", "import io.ebean.annotation.DbDefault;");
        SqlTypesImport.put("DbComment", "import io.ebean.annotation.DbComment;");
        SqlTypesImport.put("DbJsonB", "import io.ebean.annotation.DbJsonB;");
        SqlTypesImport.put("DbJson", "import io.ebean.annotation.DbJson;");
    }

    protected static List<String> javaKeyword = new ArrayList<>();

    static {
        javaKeyword.add("interface");
//        javaKeyword.add("user");
    }

    protected static List<String> sqlKeyword = new ArrayList<>();

    static {
        sqlKeyword.add("user");
    }

}
