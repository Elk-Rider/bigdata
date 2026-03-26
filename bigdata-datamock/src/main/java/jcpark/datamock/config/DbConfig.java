package jcpark.datamock.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConfig {

    private static final String url;
    private static final String username;
    private static final String password;

    static {
        try (InputStream is = DbConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                throw new RuntimeException("找不到 application.properties，请确保文件存在于 src/main/resources/");
            }
            Properties props = new Properties();
            props.load(is);
            url      = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DbConfig] 驱动加载成功，连接目标: " + url);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("数据库配置初始化失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
