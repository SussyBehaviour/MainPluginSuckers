package Thisiscool.StuffForUs;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.server.ServerControl.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.TerminalBuilder;

import arc.func.Cons;
import arc.util.Log;
import reactor.util.annotation.NonNull;

public class Console {

    public static void load() {
        try {
            var terminal = TerminalBuilder.builder()
                    .jna(true)
                    .build();

            var completer = new StringsCompleter(instance.handler.getCommandList()
                    .map(command -> command.text));

            var reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();

            terminal.enterRawMode();

            instance.serverInput = null;
            System.setOut(new BlockingPrintStream(reader::printAbove));

            handleInput(reader);
            Log.info("JLine console loaded.");
        } catch (Exception e) {
            Log.err("Failed to load JLine console", e);
        }
    }

    public static void handleInput(LineReader reader) {
        read(reader, "> ").thenAccept(text -> {
            if (!text.isBlank() && !text.startsWith("#"))
                app.post(() -> instance.handleCommandString(text));

            handleInput(reader);
        });
    }

    public static CompletableFuture<String> read(LineReader reader, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reader.readLine(prompt);
            } catch (Exception e) {
                System.exit(0);
                return null;
            }
        }, mainExecutor);
    }

    public static class BlockingPrintStream extends PrintStream {
        public final Cons<String> cons;
        public int last = -1;

        public BlockingPrintStream(Cons<String> cons) {
            super(new ByteArrayOutputStream());
            this.cons = cons;
        }

        @Override
        public void write(int sign) {
            if (last == '\r' && sign == '\n') {
                last = -1;
                return;
            }

            last = sign;

            if (sign == '\n' || sign == '\r')
                flush();
            else
                super.write(sign);
        }

        @Override
        public void write(@NonNull byte[] array, int off, int len) {
            for (int i = 0; i < len; i++)
                write(array[off + i]);
        }

        @Override
        public void flush() {
            cons.get(out.toString());
            ((ByteArrayOutputStream) out).reset();
        }
    }
}