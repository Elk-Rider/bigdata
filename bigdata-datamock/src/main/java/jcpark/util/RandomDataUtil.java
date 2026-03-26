package jcpark.util;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Mock 数据随机化工具类
 * 包含各国姓名库、地理位置、交易渠道、资产价格等静态数据
 */
public class RandomDataUtil {

    private static final Random RANDOM = new Random();

    // =========================================================
    // 姓名库（按国家/文化区分）
    // =========================================================

    private static final String[] US_FIRST_M = {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
        "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Mark",
        "Donald", "Steven", "Paul", "Andrew", "Joshua", "Kevin", "Brian", "George", "Edward", "Ronald"
    };
    private static final String[] US_FIRST_F = {
        "Mary", "Patricia", "Jennifer", "Linda", "Barbara", "Elizabeth", "Susan",
        "Jessica", "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Margaret", "Sandra",
        "Ashley", "Dorothy", "Kimberly", "Emily", "Donna", "Michelle", "Carol", "Amanda", "Melissa", "Deborah"
    };
    private static final String[] US_LAST = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Wilson", "Anderson", "Taylor", "Thomas", "Jackson", "White", "Harris",
        "Martin", "Thompson", "Moore", "Young", "Allen", "King", "Wright", "Scott", "Green", "Baker"
    };

    private static final String[] CN_FIRST = {
        "Wei", "Fang", "Jing", "Ming", "Lei", "Yang", "Xiao", "Hai", "Tao", "Hua",
        "Jun", "Ying", "Lan", "Kun", "Peng", "Zhi", "Hao", "Rui", "Yun", "Qian",
        "Xin", "Bo", "Cheng", "Fan", "Gang", "Jie", "Lin", "Nan", "Qi", "Shuai"
    };
    private static final String[] CN_LAST = {
        "Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao", "Wu", "Zhou",
        "Xu", "Sun", "Ma", "Zhu", "Hu", "Guo", "He", "Lin", "Gao", "Luo",
        "Zheng", "Liang", "Xie", "Tang", "Han", "Feng", "Dong", "Xiao", "Cheng", "Cao"
    };

    private static final String[] JP_FIRST_M = {
        "Kenji", "Takashi", "Hiroshi", "Kazuki", "Ryota", "Shota", "Yuta", "Sho",
        "Kenta", "Naoki", "Daisuke", "Taro", "Ichiro", "Jiro", "Haruto"
    };
    private static final String[] JP_FIRST_F = {
        "Yuki", "Sakura", "Aoi", "Rin", "Misaki", "Nana", "Haruka", "Miu",
        "Saki", "Hina", "Koharu", "Yui", "Mei", "Akari", "Honoka"
    };
    private static final String[] JP_LAST = {
        "Sato", "Suzuki", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura",
        "Kobayashi", "Kato", "Yoshida", "Yamada", "Sasaki", "Yamaguchi", "Matsumoto", "Inoue"
    };

    private static final String[] KR_FIRST = {
        "Min", "Seok", "Jun", "Ji", "Hyun", "Soo", "Young", "Jin", "Jae", "Woo",
        "Hee", "Na", "Kyung", "Eun", "Sang", "Dong", "Chan", "Tae", "Yeon", "Sung"
    };
    private static final String[] KR_LAST = {
        "Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Cho", "Yoon", "Jang", "Lim",
        "Han", "Oh", "Seo", "Shin", "Kwon", "Hwang", "Ahn", "Song", "Hong", "Yoo"
    };

    private static final String[] DE_FIRST_M = {
        "Hans", "Klaus", "Dieter", "Werner", "Wolfgang", "Heinz", "Peter", "Karl",
        "Thomas", "Stefan", "Andreas", "Michael", "Markus", "Lukas", "Felix"
    };
    private static final String[] DE_FIRST_F = {
        "Anna", "Maria", "Ursula", "Petra", "Monika", "Brigitte", "Heike", "Sabine",
        "Anja", "Nicole", "Andrea", "Susanne", "Claudia", "Stefanie", "Laura"
    };
    private static final String[] DE_LAST = {
        "Mueller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
        "Becker", "Schulz", "Hoffmann", "Koch", "Bauer", "Richter", "Klein", "Wolf"
    };

    private static final String[] FR_FIRST_M = {
        "Jean", "Pierre", "Michel", "Andre", "Philippe", "Louis", "Henri", "Nicolas",
        "Francois", "Julien", "Thomas", "Maxime", "Antoine", "Alexandre", "Clement"
    };
    private static final String[] FR_FIRST_F = {
        "Marie", "Isabelle", "Sophie", "Nathalie", "Catherine", "Celine", "Helene",
        "Sylvie", "Camille", "Emma", "Lea", "Manon", "Chloe", "Lucie", "Margot"
    };
    private static final String[] FR_LAST = {
        "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit",
        "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia"
    };

    private static final String[] BR_FIRST_M = {
        "Carlos", "Jose", "Antonio", "Francisco", "Paulo", "Pedro", "Lucas", "Mateus"
    };
    private static final String[] BR_FIRST_F = {
        "Ana", "Maria", "Mariana", "Amanda", "Fernanda", "Juliana", "Patricia", "Beatriz"
    };
    private static final String[] BR_LAST = {
        "Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Rodrigues"
    };

    private static final String[] IN_FIRST_M = {
        "Raj", "Amit", "Suresh", "Ramesh", "Vijay", "Arjun", "Sanjay", "Nikhil"
    };
    private static final String[] IN_FIRST_F = {
        "Priya", "Anjali", "Kavita", "Sunita", "Pooja", "Neha", "Deepa", "Shreya"
    };
    private static final String[] IN_LAST = {
        "Sharma", "Patel", "Singh", "Kumar", "Gupta", "Mehta", "Joshi", "Nair"
    };

    // =========================================================
    // 地理位置（国家 -> [省/州, 城市]）
    // =========================================================

    private static final String[][] US_LOCATIONS = {
        {"California", "Los Angeles"}, {"California", "San Francisco"}, {"California", "San Diego"},
        {"New York", "New York City"}, {"New York", "Buffalo"}, {"Texas", "Houston"},
        {"Texas", "Dallas"}, {"Texas", "Austin"}, {"Florida", "Miami"}, {"Florida", "Orlando"},
        {"Illinois", "Chicago"}, {"Washington", "Seattle"}, {"Massachusetts", "Boston"},
        {"Georgia", "Atlanta"}, {"Colorado", "Denver"}, {"Arizona", "Phoenix"},
        {"Nevada", "Las Vegas"}, {"Oregon", "Portland"}, {"Minnesota", "Minneapolis"},
        {"Michigan", "Detroit"}, {"Pennsylvania", "Philadelphia"}, {"Ohio", "Columbus"},
        {"North Carolina", "Charlotte"}, {"Tennessee", "Nashville"}, {"Virginia", "Richmond"}
    };

    private static final String[][] CN_LOCATIONS = {
        {"广东省", "深圳市"}, {"广东省", "广州市"}, {"广东省", "东莞市"},
        {"上海市", "上海市"}, {"北京市", "北京市"}, {"浙江省", "杭州市"},
        {"浙江省", "宁波市"}, {"江苏省", "南京市"}, {"江苏省", "苏州市"},
        {"四川省", "成都市"}, {"湖北省", "武汉市"}, {"山东省", "青岛市"},
        {"陕西省", "西安市"}, {"重庆市", "重庆市"}, {"湖南省", "长沙市"}
    };

    private static final String[][] UK_LOCATIONS = {
        {"England", "London"}, {"England", "Manchester"}, {"England", "Birmingham"},
        {"England", "Liverpool"}, {"England", "Leeds"}, {"Scotland", "Edinburgh"},
        {"Scotland", "Glasgow"}, {"Wales", "Cardiff"}, {"Northern Ireland", "Belfast"},
        {"England", "Bristol"}
    };

    private static final String[][] JP_LOCATIONS = {
        {"Tokyo", "Tokyo"}, {"Osaka", "Osaka"}, {"Aichi", "Nagoya"},
        {"Fukuoka", "Fukuoka"}, {"Hokkaido", "Sapporo"}, {"Kanagawa", "Yokohama"},
        {"Miyagi", "Sendai"}, {"Hiroshima", "Hiroshima"}, {"Kyoto", "Kyoto"},
        {"Okinawa", "Naha"}
    };

    private static final String[][] KR_LOCATIONS = {
        {"Seoul", "Seoul"}, {"Gyeonggi", "Incheon"}, {"Gyeonggi", "Suwon"},
        {"Busan", "Busan"}, {"Daegu", "Daegu"}, {"Daejeon", "Daejeon"},
        {"Gwangju", "Gwangju"}, {"Ulsan", "Ulsan"}
    };

    private static final String[][] DE_LOCATIONS = {
        {"Bavaria", "Munich"}, {"Bavaria", "Nuremberg"}, {"Berlin", "Berlin"},
        {"Hamburg", "Hamburg"}, {"North Rhine-Westphalia", "Cologne"},
        {"North Rhine-Westphalia", "Dusseldorf"}, {"Baden-Württemberg", "Stuttgart"},
        {"Hesse", "Frankfurt"}, {"Saxony", "Dresden"}, {"Lower Saxony", "Hanover"}
    };

    private static final String[][] FR_LOCATIONS = {
        {"Ile-de-France", "Paris"}, {"Auvergne-Rhone-Alpes", "Lyon"},
        {"Provence-Alpes-Cote d'Azur", "Marseille"}, {"Nouvelle-Aquitaine", "Bordeaux"},
        {"Occitanie", "Toulouse"}, {"Hauts-de-France", "Lille"},
        {"Grand Est", "Strasbourg"}, {"Pays de la Loire", "Nantes"}
    };

    private static final String[][] CA_LOCATIONS = {
        {"Ontario", "Toronto"}, {"Quebec", "Montreal"}, {"British Columbia", "Vancouver"},
        {"Alberta", "Calgary"}, {"Ontario", "Ottawa"}
    };

    private static final String[][] AU_LOCATIONS = {
        {"New South Wales", "Sydney"}, {"Victoria", "Melbourne"},
        {"Queensland", "Brisbane"}, {"Western Australia", "Perth"},
        {"South Australia", "Adelaide"}
    };

    private static final String[][] BR_LOCATIONS = {
        {"Sao Paulo", "Sao Paulo"}, {"Rio de Janeiro", "Rio de Janeiro"},
        {"Minas Gerais", "Belo Horizonte"}, {"Bahia", "Salvador"},
        {"Parana", "Curitiba"}
    };

    private static final String[][] IN_LOCATIONS = {
        {"Maharashtra", "Mumbai"}, {"Karnataka", "Bangalore"}, {"Delhi", "New Delhi"},
        {"Tamil Nadu", "Chennai"}, {"Telangana", "Hyderabad"}
    };

    private static final String[][] SG_LOCATIONS = {
        {"Central Region", "Singapore"}, {"West Region", "Singapore"},
        {"East Region", "Singapore"}
    };

    // =========================================================
    // 交易数据枚举
    // =========================================================

    public static final String[] TRADE_CHANNELS = {
        "META", "GOOGLE", "IAP", "STRIPE", "PAYPAL", "ALIPAY", "WECHAT_PAY"
    };

    public static final String[] ORDER_TRADE_TYPES = {"IAP", "Revenue", "Subscription"};

    /** 高客单价 IAP 价格档位 */
    public static final double[] IAP_PRICES = {
        9.99, 14.99, 19.99, 29.99, 49.99, 69.99, 99.99, 149.99, 199.99, 299.99, 499.99
    };

    /** 订阅价格档位 */
    public static final double[] SUBSCRIPTION_PRICES = {
        9.99, 14.99, 19.99, 29.99, 49.99
    };

    /** 广告变现金额范围 */
    public static final double REVENUE_MIN = 5.00;
    public static final double REVENUE_MAX = 80.00;

    /** 折扣比例（0~30%随机）*/
    public static final double DISCOUNT_RATE_MAX = 0.30;

    /** 货币分布（USD 占主导）*/
    private static final String[] CURRENCIES = {
        "USD", "USD", "USD", "USD", "USD", "EUR", "CNY", "GBP"
    };

    /** 年龄段及其权重 */
    private static final String[] AGE_RANGES = {"18-24", "25-34", "35-44", "45-54", "55+"};
    private static final int[] AGE_WEIGHTS = {20, 35, 25, 15, 5};

    // =========================================================
    // 通用工具方法
    // =========================================================

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static <T> T pick(T[] arr) {
        return arr[RANDOM.nextInt(arr.length)];
    }

    public static String[] pickLocation(String[][] arr) {
        return arr[RANDOM.nextInt(arr.length)];
    }

    public static int randInt(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static double randDouble(double min, double max) {
        return min + RANDOM.nextDouble() * (max - min);
    }

    /** 按权重随机选取年龄段 */
    public static String randomAgeRange() {
        int r = RANDOM.nextInt(100);
        int cumulative = 0;
        for (int i = 0; i < AGE_WEIGHTS.length; i++) {
            cumulative += AGE_WEIGHTS[i];
            if (r < cumulative) return AGE_RANGES[i];
        }
        return AGE_RANGES[AGE_RANGES.length - 1];
    }

    /** 随机性别（male 48% / female 48% / unknown 4%） */
    public static String randomGender() {
        int r = RANDOM.nextInt(100);
        if (r < 48) return "male";
        if (r < 96) return "female";
        return "unknown";
    }

    /** 根据国家和性别生成姓名 */
    public static String randomName(String country, String gender) {
        boolean isMale = "male".equals(gender);
        switch (country) {
            case "USA": case "Canada": case "Australia": case "UK":
                return (isMale ? pick(US_FIRST_M) : pick(US_FIRST_F)) + " " + pick(US_LAST);
            case "China":
                return pick(CN_LAST) + " " + pick(CN_FIRST);
            case "Japan":
                return pick(JP_LAST) + " " + (isMale ? pick(JP_FIRST_M) : pick(JP_FIRST_F));
            case "South Korea":
                return pick(KR_LAST) + " " + pick(KR_FIRST);
            case "Germany":
                return (isMale ? pick(DE_FIRST_M) : pick(DE_FIRST_F)) + " " + pick(DE_LAST);
            case "France":
                return (isMale ? pick(FR_FIRST_M) : pick(FR_FIRST_F)) + " " + pick(FR_LAST);
            case "Brazil":
                return (isMale ? pick(BR_FIRST_M) : pick(BR_FIRST_F)) + " " + pick(BR_LAST);
            case "India":
                return (isMale ? pick(IN_FIRST_M) : pick(IN_FIRST_F)) + " " + pick(IN_LAST);
            default:
                return pick(US_FIRST_M) + " " + pick(US_LAST);
        }
    }

    /** 根据国家返回语言 */
    public static String countryToLanguage(String country) {
        switch (country) {
            case "China":       return "zh";
            case "Japan":       return "ja";
            case "South Korea": return "ko";
            case "Germany":     return "de";
            case "France":      return "fr";
            case "Brazil":      return "pt";
            default:            return "en";
        }
    }

    /** 随机获取 [省, 市] */
    public static String[] randomLocation(String country) {
        switch (country) {
            case "USA":         return pickLocation(US_LOCATIONS);
            case "China":       return pickLocation(CN_LOCATIONS);
            case "UK":          return pickLocation(UK_LOCATIONS);
            case "Japan":       return pickLocation(JP_LOCATIONS);
            case "South Korea": return pickLocation(KR_LOCATIONS);
            case "Germany":     return pickLocation(DE_LOCATIONS);
            case "France":      return pickLocation(FR_LOCATIONS);
            case "Canada":      return pickLocation(CA_LOCATIONS);
            case "Australia":   return pickLocation(AU_LOCATIONS);
            case "Brazil":      return pickLocation(BR_LOCATIONS);
            case "India":       return pickLocation(IN_LOCATIONS);
            case "Singapore":   return pickLocation(SG_LOCATIONS);
            default:            return new String[]{"Unknown", "Unknown"};
        }
    }

    public static String randomCurrency() {
        return pick(CURRENCIES);
    }

    /**
     * 根据交易类型随机生成金额（高客单价分布）
     */
    public static double randomAmount(String tradeType) {
        switch (tradeType) {
            case "IAP":
                return IAP_PRICES[RANDOM.nextInt(IAP_PRICES.length)];
            case "Subscription":
                return SUBSCRIPTION_PRICES[RANDOM.nextInt(SUBSCRIPTION_PRICES.length)];
            case "Revenue":
            default:
                return Math.round(randDouble(REVENUE_MIN, REVENUE_MAX) * 100.0) / 100.0;
        }
    }

    /**
     * 生成过去 N 天到现在之间的随机时间
     */
    public static LocalDateTime randomPastTime(int maxDaysAgo) {
        long now = System.currentTimeMillis();
        long daysAgoMs = (long) randInt(0, maxDaysAgo) * 24 * 3600 * 1000L;
        long randomMs = (long) (RANDOM.nextDouble() * 24 * 3600 * 1000L);
        long ts = now - daysAgoMs - randomMs;
        return LocalDateTime.ofEpochSecond(ts / 1000, (int) ((ts % 1000) * 1_000_000),
                java.time.ZoneOffset.ofHours(8));
    }
}
