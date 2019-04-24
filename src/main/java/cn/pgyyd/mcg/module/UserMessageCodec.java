package cn.pgyyd.mcg.module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.pgyyd.mcg.module.MysqlMessage.CompositeMessage;
import cn.pgyyd.mcg.module.MysqlMessage.ExecuteMessage;
import cn.pgyyd.mcg.module.MysqlMessage.QueryMessage;
import cn.pgyyd.mcg.module.MysqlMessage.UpdateMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class UserMessageCodec{
    /**
     * 有点冗余，暂时先这么干
     * @author memetao
     *
     */
    static public class MysqlQuery implements MessageCodec<QueryMessage, QueryMessage> {
        /**
         * 将消息实体封装到Buffer用于传输
         * 
         * 实现方式：
         *   使用对象流从对象中获取Byte数组然后追加到Buffer
         */
        @Override
        public void encodeToWire(Buffer buffer, QueryMessage s) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o;
            try {
                o = new ObjectOutputStream(b);
                o.writeObject(s);
                o.close();
                buffer.appendBytes(b.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 从buffer中获取传输的消息实体
         */
        @Override
        public QueryMessage decodeFromWire(int pos, Buffer buffer) {
             final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
             ObjectInputStream o = null;
             QueryMessage msg = null;
             try {
                o = new ObjectInputStream(b);
                msg = (QueryMessage) o.readObject();
             } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
             }
             return msg;
        }
     
        /**
         * 如果是本地消息则直接返回
         */
        @Override
        public QueryMessage transform(QueryMessage s) {
            return s;
        }
        /**
         * 编解码器的名称：
         *     必须唯一，用于发送消息时识别编解码器，以及取消编解码器
         */
        @Override
        public String name() {
            return this.getClass().getName();
        }
        /**
         * 用于识别是否是用户编码器
         * 自定义编解码器通常使用-1
         */
        @Override
        public byte systemCodecID() {
            return -1;
        }
        
    }
    
    static public class MysqlUpdate implements MessageCodec<UpdateMessage, UpdateMessage> {
        /**
         * 将消息实体封装到Buffer用于传输
         * 
         * 实现方式：
         *   使用对象流从对象中获取Byte数组然后追加到Buffer
         */
        @Override
        public void encodeToWire(Buffer buffer, UpdateMessage s) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o;
            try {
                o = new ObjectOutputStream(b);
                o.writeObject(s);
                o.close();
                buffer.appendBytes(b.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 从buffer中获取传输的消息实体
         */
        @Override
        public UpdateMessage decodeFromWire(int pos, Buffer buffer) {
             final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
             ObjectInputStream o = null;
             UpdateMessage msg = null;
             try {
                o = new ObjectInputStream(b);
                msg = (UpdateMessage) o.readObject();
             } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
             }
             return msg;
        }
     
        /**
         * 如果是本地消息则直接返回
         */
        @Override
        public UpdateMessage transform(UpdateMessage s) {
            return s;
        }
        /**
         * 编解码器的名称：
         *     必须唯一，用于发送消息时识别编解码器，以及取消编解码器
         */
        @Override
        public String name() {
            return this.getClass().getName();
        }
        /**
         * 用于识别是否是用户编码器
         * 自定义编解码器通常使用-1
         */
        @Override
        public byte systemCodecID() {
            return -1;
        }
        
    }
    static public class MysqlExecute implements MessageCodec<ExecuteMessage, ExecuteMessage> {
        /**
         * 将消息实体封装到Buffer用于传输
         * 
         * 实现方式：
         *   使用对象流从对象中获取Byte数组然后追加到Buffer
         */
        @Override
        public void encodeToWire(Buffer buffer, ExecuteMessage s) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o;
            try {
                o = new ObjectOutputStream(b);
                o.writeObject(s);
                o.close();
                buffer.appendBytes(b.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 从buffer中获取传输的消息实体
         */
        @Override
        public ExecuteMessage decodeFromWire(int pos, Buffer buffer) {
             final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
             ObjectInputStream o = null;
             ExecuteMessage msg = null;
             try {
                o = new ObjectInputStream(b);
                msg = (ExecuteMessage) o.readObject();
             } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
             }
             return msg;
        }
     
        /**
         * 如果是本地消息则直接返回
         */
        @Override
        public ExecuteMessage transform(ExecuteMessage s) {
            return s;
        }
        /**
         * 编解码器的名称：
         *     必须唯一，用于发送消息时识别编解码器，以及取消编解码器
         */
        @Override
        public String name() {
            return this.getClass().getName();
        }
        /**
         * 用于识别是否是用户编码器
         * 自定义编解码器通常使用-1
         */
        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
    
    static public class MysqlComposite implements MessageCodec<CompositeMessage, CompositeMessage> {
        /**
         * 将消息实体封装到Buffer用于传输
         * 
         * 实现方式：
         *   使用对象流从对象中获取Byte数组然后追加到Buffer
         */
        @Override
        public void encodeToWire(Buffer buffer, CompositeMessage s) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o;
            try {
                o = new ObjectOutputStream(b);
                o.writeObject(s);
                o.close();
                buffer.appendBytes(b.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 从buffer中获取传输的消息实体
         */
        @Override
        public CompositeMessage decodeFromWire(int pos, Buffer buffer) {
             final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
             ObjectInputStream o = null;
             CompositeMessage msg = null;
             try {
                o = new ObjectInputStream(b);
                msg = (CompositeMessage) o.readObject();
             } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
             }
             return msg;
        }
     
        /**
         * 如果是本地消息则直接返回
         */
        @Override
        public CompositeMessage transform(CompositeMessage s) {
            return s;
        }
        /**
         * 编解码器的名称：
         *     必须唯一，用于发送消息时识别编解码器，以及取消编解码器
         */
        @Override
        public String name() {
            return this.getClass().getName();
        }
        /**
         * 用于识别是否是用户编码器
         * 自定义编解码器通常使用-1
         */
        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
}
