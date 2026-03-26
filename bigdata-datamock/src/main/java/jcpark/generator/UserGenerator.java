package jcpark.generator;

import jcpark.config.DbConfig;
import jcpark.util.RandomDataUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户信息表 Mock 数据生成器
 *
 * 国家分布（共100人）：
 *   USA(25) China(15) UK(10) Japan(8) South Korea(7)
 *   Germany(7) France(6) Canada(5) Australia(5)
 *   Brazil(4) India(4) Singapore(4)
 *
 * monitor_currency_config / monitor_transaction_config / user_preferences
 * 三个 JSON 字段由用户自行维护，此处留 NULL。
 */
public class UserGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 国家及对应人数分布 */
    private static final Object[][] COUNTRY_DIST = {
        {"USA",         25},
        {"China",       15},
        {"UK",          10},
        {"Japan",        8},
        {"South Korea",  7},
        {"Germany",      7},
        {"France",       6},
        {"Canada",       5},
        {"Australia",    5},
        {"Brazil",       4},
        {"India",        4},
        {"Singapore",    4},
    };

    /**
     * 检查 user 表，若已有数据则直接加载 user_id 列表；
     * 若为空则生成 100 名用户并插入 DB，返回所有 user_id。
     */
    public List<String> initUsers() throws SQLException {
        try (Connection conn = DbConfig.getConnection()) {
            long count = countUsers(conn);
            if (count > 0) {
                System.out.println("[UserGenerator] user 表已有 " + count + " 条数据，跳过生成，直接加载 user_id...");
                return loadUserIds(conn);
            }
        }
        System.out.println("[UserGenerator] user 表为空，开始生成 100 名随机用户...");
        return generateAndInsert();
    }

    private long countUsers(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `user`")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private List<String> loadUserIds(Connection conn) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT user_id FROM `user`")) {
            while (rs.next()) ids.add(rs.getString(1));
        }
        System.out.println("[UserGenerator] 加载到 " + ids.size() + " 个 user_id");
        return ids;
    }

    private List<String> generateAndInsert() throws SQLException {
        List<String> userIds = new ArrayList<>();
        String sql = "INSERT INTO `user` " +
                "(user_id, appsflyer_id, user_name, gender, age_range, language, " +
                " country, province, city, register_time, first_install_time, last_login_time, " +
                " monitor_currency_config, monitor_transaction_config, user_preferences) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NULL,NULL,NULL)";

        try (Connection conn = DbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Object[] entry : COUNTRY_DIST) {
                String country = (String) entry[0];
                int count = (int) entry[1];
                for (int i = 0; i < count; i++) {
                    String userId  = RandomDataUtil.randomUUID();
                    String afId    = "AF_" + userId.replace("-", "").substring(0, 16).toUpperCase();
                    String gender  = RandomDataUtil.randomGender();
                    String name    = RandomDataUtil.randomName(country, gender);
                    String[] loc   = RandomDataUtil.randomLocation(country);
                    String lang    = RandomDataUtil.countryToLanguage(country);
                    String ageRange = RandomDataUtil.randomAgeRange();

                    // 注册时间：过去 730 天内随机
                    LocalDateTime registerTime     = RandomDataUtil.randomPastTime(730);
                    // 首次安装时间：注册时间前 0~7 天（或同一天）
                    LocalDateTime firstInstallTime = registerTime.minusDays(RandomDataUtil.randInt(0, 7));
                    // 最后登录时间：过去 30 天内
                    LocalDateTime lastLoginTime    = RandomDataUtil.randomPastTime(30);

                    ps.setString(1,  userId);
                    ps.setString(2,  afId);
                    ps.setString(3,  name);
                    ps.setString(4,  gender);
                    ps.setString(5,  ageRange);
                    ps.setString(6,  lang);
                    ps.setString(7,  country);
                    ps.setString(8,  loc[0]);
                    ps.setString(9,  loc[1]);
                    ps.setString(10, registerTime.format(FMT));
                    ps.setString(11, firstInstallTime.format(FMT));
                    ps.setString(12, lastLoginTime.format(FMT));
                    ps.addBatch();
                    userIds.add(userId);
                }
            }
            int[] results = ps.executeBatch();
            System.out.printf("[UserGenerator] 成功插入 %d 名用户%n", results.length);
        }
        return userIds;
    }
}
