package cn.pgyyd.mcg.module;

import java.util.Map.Entry;
import java.util.TreeMap;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
/*TODO:弄成单例
 * */
@Slf4j
public class DBSelector {
    private static  Boolean is_initialized = false;
    
    private static TreeMap<String/*hash key*/,
            JsonObject/*数据库信息，账号\密码等*/> db_configs = new TreeMap<String,JsonObject>();
    
    private static long hash_ring_length = 0;
    
    private static TreeMap<Long,String> indexs = new TreeMap<Long,String>();
    
    public DBSelector() {
        ;
    }
    /**
     * FIXME: 存在多线程竞争的问题
     * @param vertx
     */
    public void init(JsonObject config) {
        if(is_initialized) {
            return;
        }
        log.info("DBSelector initializing...");
        Integer db_number =config.getInteger("db_number",1);  
        allocate_hashkeys(db_number);
        bind_hash_and_mysql(config);
        exactMainDbInfo(get_mysql_json_config(config,"m0"));
        is_initialized = true;
        log.info("DBSelector initialized");
    }

    private void exactMainDbInfo(final JsonObject mySQLClientConfig) {
        if(!is_initialized) {
            load_infos(mySQLClientConfig);
        }
    }
    
    static public TreeMap<String,JsonObject> hashkey_and_db_configs(){
        if(!is_initialized) {
            return null;
        }
        return db_configs;
    }
    
    private int get_key_from_student_id(String student_id) {
        //1400304211   003代表学院id?
        //FIXME:应该没有000吧
        try {
            student_id = student_id.substring(2,5);
        }catch (IndexOutOfBoundsException e) {
            log.error("student id :"+student_id + " is illegal!");
        }
        return 0;
    }
    
    public String hash_from_student_id(final String student) {
        int key = get_key_from_student_id(student);
        long index = key % hash_ring_length;
        String hash =  indexs.get(index);
        return hash;
    }
    
    public String hash_from_course_code(final String code) {
        //按照计划，每个课程id的后3位是学院id
        //cjlu-00002-001
        int key  =  Integer.parseInt(code.substring(code.length() - 3, code.length() - 1));
        long index = key % hash_ring_length;
        String hash = indexs.get(index);
        return hash;
    }
    
    public String main_db_hash()
    {
        long index = 0;
        return indexs.get(index);
    }
    
    private boolean load_infos(final JsonObject mySQLClientConfig) {
        return false;
    }
    
    private void allocate_hashkeys(int db_num) {
        log.info("allocating hash keys...");
        if(db_num == 0) {
            log.error("db number is must greater than zero!");
            System.exit(-1);
        }
        String hash0 = "mcg-" + "database-selector-seat";
        hash_ring_length = db_num ;
        for(int i = 0 ;i < db_num ; i++) {
            long index = i;
            indexs.put(index,hash0 + index);
        }
    }
    
    private void bind_hash_and_mysql(JsonObject configs){
        log.info("bind hash and mysql...");
        for(Entry<Long,String> it : indexs.entrySet()) {
            JsonObject config = null;
            if(it.getKey() == 0) {
                config = get_mysql_json_config(configs,"m0");
                if(config == null) {
                    log.error("load m0 configuration failed!");
                    System.exit(-1);
                }
                db_configs.put(it.getValue(), config);
                continue;
            }
            config = get_mysql_json_config(configs,"s" + it.getKey());
            if(config == null) {
                log.error("load s" + it.getKey() +" configuration failed!");
                System.exit(-1);
            }
            db_configs.put(it.getValue(),config );
        }
    }
    
    private JsonObject get_mysql_json_config(JsonObject configs, String name) {
        JsonObject config = configs.getJsonObject(name);  
        if(config == null) {
            return config;
        }
        JsonObject mysql = new JsonObject().
                put("host", config.getString("host","127.0.0.1")).
                put("port",config.getInteger("port",3306)).
                put("username",config.getString("user","memetao")).
                put("password",config.getString("passwd","123456")).
                put("maxPoolSize",config.getInteger("connections",4)).
                put("database",config.getString("name","mcg")).
                put("queryTimeout",1000);
        return mysql;
    }
}
