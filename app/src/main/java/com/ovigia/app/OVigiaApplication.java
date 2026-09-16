package com.ovigia.app;

import android.app.Application;
import android.os.StrictMode;

public class OVigiaApplication extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            // Pega I/O e vazamentos na main thread durante o desenvolvimento.
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().detectNetwork()
                    .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects().detectActivityLeaks()
                    .penaltyLog().build());
        }
        container = new AppContainer(this);
    }

    public AppContainer container() {
        return container;
    }
}
