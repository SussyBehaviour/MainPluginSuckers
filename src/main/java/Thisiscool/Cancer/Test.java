package Thisiscool.Cancer;

import static arc.Core.*;

import Thisiscool.Cancer.EventBus.Request;
import Thisiscool.Cancer.EventBus.Response;
import arc.math.Mathf;
import arc.mock.MockApplication;
import arc.struct.Seq;
import arc.util.Log;
import lombok.AllArgsConstructor;

public class Test {
    public static ServerLegend server;
    public static ClientLegend client;

    public static void main(String... args) {
        app = new MockApplication();

        server = Legend.server(Mathf.random(9999));
        server.setName("Server");

        client = Legend.client(server.getPort());
        client.setName("Client");

        server.connect();
        client.connect();

        server.on(Test1.class, test1 -> {
            Log.info("Received?");
            server.respond(test1, new Test2("Name:"));
        });

        client.request(new Test1(Seq.with(new Field("Test:", "asd"))), test2 -> {
            Log.info("Received!");
        });

        while (true) {
        }
    }

    @AllArgsConstructor
    public static class Test1 extends Request<Test2> {
        public Seq<Field> fields;
    }

    @AllArgsConstructor
    public static class Field {
        public String name;
        public String text;
    }

    @AllArgsConstructor
    public static class Test2 extends Response {
        public String field;
    }
}