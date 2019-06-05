package cn.pgyyd.mcg.module;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
/*TODO:弄成单例
 * */
@Slf4j
public class DBSelector {
    private static  Boolean is_initialized = false;
    
    private static TreeMap<String/*hash key*/,JsonObject/*数据库信息，账号\密码等*/> db_configs;
    
    private long hash_ring_length = 0;
    
    private TreeMap<Long,String> indexs;
    
    public DBSelector() {
        ;
    }
    /**
     * FIXME: 存在多线程竞争的问题
     * @param vertx
     */
    public void init(Vertx vertx) {
        if(is_initialized) {
            return;
        }
        log.info("DBSelector initializing...");
        ConfigStoreOptions storeOptions = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"));
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions().addStore(storeOptions);
        ConfigRetriever retriever =  ConfigRetriever.create(vertx, retrieverOptions);
        retriever.getConfig(ret -> {
            if(ret.succeeded()) {
                int db_number = ret.result().getInteger("db_number");  
                
                allocate_hashkeys(db_number);
                bind_hash_and_mysql(ret.result());
                exactMainDbInfo(get_mysql_json_config(ret.result(),"m0"));
                
                is_initialized = true;
            } else {
                log.error("get config file failed");
                System.exit(-1);
            }
        });
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        while(!is_initialized);
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
        student_id = student_id.substring(student_id.length() - 8);
        student_id = student_id.substring(0,3);  
        return 0;
    }
    
    public String hash_from_student_id(final String student) {
        int key = get_key_from_student_id(student);
        long index = key % hash_ring_length;
        return indexs.get(index);
    }
    
    public String hash_from_course_code(final String code) {
        //按照计划，每个课程id的后3位是学院id
        int key  =  Integer.parseInt(code.substring(code.length() - 3, code.length() - 1));
        long index = key % hash_ring_length;
        return indexs.get(index);
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
        String hash0 = "mcg-" + "database-selector-seat";
        hash_ring_length = db_num ;
        for(int i = 0 ;i < db_num ; i++) {
            long index = i;
            indexs.put(index,hash0 + index);
        }
    }
    
    private void bind_hash_and_mysql(JsonObject configs){
        for(Entry<Long,String> it : indexs.entrySet()) {
            JsonObject config = null;
            if(it.getKey() == 0) {
                config = get_mysql_json_config(configs,"m0");
                if(config == null) {
                    System.exit(-1);
                }
                db_configs.put(it.getValue(), config);
                continue;
            }
            config = get_mysql_json_config(configs,"s" + it.getKey());
            if(config == null) {
                System.exit(-1);
            }
            db_configs.put(it.getValue(),config );
        }
    }
    
    private JsonObject get_mysql_json_config(JsonObject configs, String name) {
        JsonObject config = configs.getJsonObject(name);  
        if(config == null) {
            log.error("get mysql configuration failed,name:" + name);
            return config;
        }
        JsonObject mysql = new JsonObject().
                put("host", config.getString("host")).
                put("port",config.getInteger("port")).
                put("username",config.getString("user")).
                put("password",config.getString("passwd")).
                put("maxPoolSize",config.getInteger("connections")).
                put("database",config.getString("name")).
                put("queryTimeout",1000);
        return mysql;
    }
}
