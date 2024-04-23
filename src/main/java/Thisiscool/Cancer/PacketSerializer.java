package Thisiscool.Cancer;

import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;

import Thisiscool.Cancer.EventBus.Response;
import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.DiscoverHost;
import arc.net.FrameworkMessage.KeepAlive;
import arc.net.FrameworkMessage.Ping;
import arc.net.FrameworkMessage.RegisterTCP;
import arc.net.FrameworkMessage.RegisterUDP;
import arc.net.NetSerializer;
import arc.util.Log;
import lombok.Getter;

@Getter
public class PacketSerializer implements NetSerializer {
    protected final Legend Legend;
    public PacketSerializer(Legend Legend) {
        this.Legend = Legend;
    }
    @Override
    public void write(ByteBuffer buffer, Object value) {
        if (value instanceof FrameworkMessage message) {
            buffer.put((byte) -2);
            writeFramework(buffer, message);
        } else {
            buffer.put((byte) -1);
            writeObject(buffer, value);
        }
    }

    @Override
    public Object read(ByteBuffer buffer) {
        Object result = switch (buffer.get()) {
            case -2 -> readFramework(buffer);
            case -1 -> readObject(buffer);
            default -> throw new IllegalStateException("Unexpected object type!");
        };
        return result;
    }

    private void writeString(ByteBuffer buffer, String string) {
        buffer.putInt(string.length());
        buffer.put(string.getBytes());
    }

    private String readString(ByteBuffer buffer) {
        var bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new String(bytes);
    }

    private void writeFramework(ByteBuffer buffer, FrameworkMessage message) {
        if (message instanceof Ping ping) {
            buffer.put((byte) 0);
            buffer.putInt(ping.id);
            buffer.put((byte) (ping.isReply ? 1 : 0));
        } else if (message instanceof KeepAlive) {
            buffer.put((byte) 1);
        } else if (message instanceof DiscoverHost) {
            buffer.put((byte) 2);
        } else if (message instanceof RegisterTCP tcp) {
            buffer.put((byte) 3);
            buffer.putInt(tcp.connectionID);
        } else if (message instanceof RegisterUDP udp) {
            buffer.put((byte) 4);
            buffer.putInt(udp.connectionID);
        } else if (message instanceof LegendName name) {
            buffer.put((byte) 5);
            writeString(buffer, name.name());
        }
    }

    private FrameworkMessage readFramework(ByteBuffer buffer) {
        return switch (buffer.get()) {
            case 0 -> new Ping() {{
                this.id = buffer.getInt();
                this.isReply = buffer.get() == 1;
            }};
            case 1 -> FrameworkMessage.keepAlive;
            case 2 -> FrameworkMessage.discoverHost;
            case 3 -> new RegisterTCP() {{
                this.connectionID = buffer.getInt();
            }};
            case 4 -> new RegisterUDP() {{
                this.connectionID = buffer.getInt();
            }};
            case 5 -> new LegendName(readString(buffer));
            default -> throw new IllegalStateException("Unexpected framework message!");
        };
    }

    private Object readObject(ByteBuffer buffer) {
        try {
            var type = Class.forName(readString(buffer));
            if (!(Response.class.isAssignableFrom(type) || Legend.getBus().contains(type))) {
                skipRemaining(buffer);
                return null;
            }

            var bytes = new byte[buffer.getInt()];
            buffer.get(bytes);

            return JSON.parseObject(bytes, type);
        } catch (ClassNotFoundException e) {
            Log.err("Class not found during deserialization: " + e.getMessage());
            skipRemaining(buffer);
            return null;
        }
    }

    private void writeObject(ByteBuffer buffer, Object value) {
        writeString(buffer, value.getClass().getName());

        var json = JSON.toJSONBytes(value);
        buffer.putInt(json.length);
        buffer.put(json);
    }

    private void skipRemaining(ByteBuffer buffer) {
        buffer.position(buffer.position() + buffer.remaining());
    }
}