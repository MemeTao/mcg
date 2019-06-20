package cn.pgyyd.mcg.ds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.pgyyd.mcg.ds.SelectCourseMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class UserMessageCodec{
    /**
     * 有点冗余，暂时先这么干
     * @author memetao
     *
     */

    static public class SelectCourseMessageCodec implements MessageCodec<SelectCourseMessage, SelectCourseMessage> {
        /**
         * 将消息实体封装到Buffer用于传输
         *
         * 实现方式：
         *   使用对象流从对象中获取Byte数组然后追加到Buffer
         */
        @Override
        public void encodeToWire(Buffer buffer, SelectCourseMessage selectCourseMessage) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o;
            try {
                o = new ObjectOutputStream(b);
                o.writeObject(selectCourseMessage);
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
        public SelectCourseMessage decodeFromWire(int pos, Buffer buffer) {
            final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
            SelectCourseMessage msg = null;
            try {
                ObjectInputStream o = new ObjectInputStream(b);
                msg = (SelectCourseMessage) o.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return  msg;
        }

        /**
         * 如果是本地消息则直接返回
         */
        @Override
        public SelectCourseMessage transform(SelectCourseMessage selectCourseMessage) {
            return selectCourseMessage;
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
