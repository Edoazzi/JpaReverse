package org.jpareverse;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
import io.ebeaninternal.dbmigration.DefaultDbMigration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 */
public class App {
    static JDBCReverse jdbcUtil;
    static Config config;

    public static void main(String[] args) throws IOException {
        System.out.println("JpaReverse:");

        String fileconf = args.length == 0 ? "reverse.yaml" : args[0];
        String msg = loadConfiguration(fileconf);
        if (!msg.isEmpty()) {
            System.out.printf("Error: %s", msg);
            return;
        }

        int task = 1;
        if (task == 1) {
            gera();
        } else {
            migra();
        }

    }


    public static String loadConfiguration(String filename) throws FileNotFoundException {
        try {

            InputStream input = new FileInputStream(new File(
                    filename));

            Constructor constructor = new Constructor(Config.class);
            Yaml yaml = new Yaml(constructor);
            config = yaml.load(input);
            if (config.getFlat_mode() == null) {
                config.setFlat_mode("N");
            }
            if (config.getEbean_annotations() == null) {
                config.setEbean_annotations("Y");
            }
            if (config.getLombok() == null) {
                config.setLombok("N");
            }
            if (config.getSize_constants() == null) {
                config.setSize_constants("N");
            }
            if (config.getUse_model() == null) {
                config.setUse_model("T");
            }
            if (config.getVersion_field() == null) {
                config.setVersion_field("");
            }
            for (TableDefinition tab : config.getTables()) {
                if (config.getDestination_db().equals("postgres")) {
                    tab.setName(tab.getName().toLowerCase());
                }
                if (tab.getAlias() == null) {
                    tab.setAlias(tab.getName());
                }
            }
        /*List<BaseModel> bases = new ArrayList<>();
        BaseModel b1 = new BaseModel();
        b1.file = "BaseModel.java";
        b1.fields = "id,version, cod_lancamento, whencreated, whenmodified,whocreated,whomodified";
        BaseModel b2 = new BaseModel();
        b2.file = "BaseModelSys.java";
        b2.fields = "id,version, whencreated, whenmodified,whocreated,whomodified";
        bases.add(b1);
        bases.add(b2);
        config.setBaseModels(bases);
        System.out.println(config);
        System.out.println(yaml.dump(config));*/

            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }


    public static void gera() {


        Path pathToBeDeleted = Paths.get(config.getDestination_path());

        try {
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {

        }


        inicDB(config.getDatasource_databaseDriver(),
                config.getDatasource_databaseUrl(), config.getDatasource_username(), config.getDatasource_password());
        if (config.getDestination_db().contains("postgres")) {
            jdbcUtil.setDestinationPostgres();
        }
        jdbcUtil.setSourcePath(config.getDestination_path());

        List<String> tablesMade = new ArrayList<>();

        jdbcUtil.setTableList(config.getTables());
        for (int i = 0; i < config.getTables().size(); i++) {
            if (tablesMade.contains(config.getTables().get(i).getName())) {
                // does not repeat table
                continue;
            }

            System.out.println(i + ". " + config.getTables().get(i).getAlias());
            jdbcUtil.generateTableJpa(config.getTables(), i);
            tablesMade.add(config.getTables().get(i).getName());
        }

        System.out.printf("\n\n");
        List<String> allTables = jdbcUtil.getAllTables();
        for (String table : allTables) {
            if (!tablesMade.contains(table)) {
                System.out.printf("->>Table not defined %s\n", table);
            }
        }
    }


    private static void migra() throws IOException {
        System.setProperty("ddl.migration.name", "initial mig");
        DbMigration dbMigration = new DefaultDbMigration();
        dbMigration.setPlatform(Platform.SQLSERVER17);
        dbMigration.setStrictMode(false);

        // generate the migration ddl and xml
        // ... with EbeanServer in "offline" mode
        dbMigration.generateMigration();

    }

    private static void inicDB(String className, String url, String userName, String password) {

        jdbcUtil = new JDBCReverse(className, url, userName, new String(password));
        jdbcUtil.getConnection();

    }

    private static void inicSqlServer(String className, String hostname, String dbName, String userName, String password) {
        StringBuffer URL = new StringBuffer();
        int port = 1434;
        URL.append("jdbc:sqlserver://");
        URL.append(hostname + ":" + port + ";databaseName=" + dbName);

        jdbcUtil = new JDBCReverse(className, URL.toString(), userName, new String(password));
        jdbcUtil.getConnection();

    }
}
