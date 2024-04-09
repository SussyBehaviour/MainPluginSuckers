package Thisiscool.Cancer;

import static arc.Core.*;

import arc.net.NetListener;
import arc.net.NetListener.QueuedListener;

public class MainThreadListener extends QueuedListener {

    public MainThreadListener(NetListener listener) {
        super(listener);
    }

    @Override
    protected void queue(Runnable runnable) {
        app.post(runnable);
    }
}