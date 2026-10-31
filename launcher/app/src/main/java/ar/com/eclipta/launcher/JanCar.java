package ar.com.eclipta.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Minimal hand-written Binder clients for the JanCar IVI service (com.jancar.services).
 *
 * Wire format copied from the decompiled JanCar SDK (com.jancar.services.media.IMedia,
 * IMediaInfoCallback, com.jancar.services.system.ISystem, IGpsCallback): interface
 * descriptors and TRANSACTION_ codes must match exactly. Only the calls v0.1 needs are
 * implemented. Everything is optional: failures are logged and ignored.
 */
final class JanCar {
    static final String PKG = "com.jancar.services";
    static final String ACTION_MEDIA = "com.jancar.services.action.media";
    static final String ACTION_SYSTEM = "com.jancar.services.action.system";
    static final String ACTION_ACC = "com.jancar.services.action.acc";
    static final String EXTRA_ACC = "acc";
    // AutoChips quick boot wake-up. On this unit ignition on/off is a quick boot cycle and
    // JanCar's ACC broadcast does not fire, so this is the reliable wake signal.
    static final String ACTION_QB_POWERON = "autochips.intent.action.QB_POWERON";

    // IVIMedia.Type
    static final int TYPE_MUSIC = 0;
    static final int TYPE_RADIO = 1;
    static final int TYPE_A2DP = 7;
    static final int TYPE_CARPLAY = 25;
    // play state used by JanCar music: 3 = playing, 2 = paused
    static final int STATE_PLAYING = 3;

    static final String ACTION_CAR = "com.jancar.services.action.car";
    private static final String DESC_CAR = "com.jancar.services.car.ICar";
    private static final int TX_GET_HEADLIGHT_STATUS = 14;

    private JanCar() {}

    static boolean available(Context c) {
        try {
            c.getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    interface LightsListener {
        /** Called on a background thread. */
        void onHeadlights(boolean on);
    }

    /**
     * Read-only access to ICar.getHeadLightStatus. No callback is registered: the launcher reads
     * the value when it connects and once a minute while visible.
     */
    static final class CarLink implements ServiceConnection {
        private final LightsListener listener;
        private volatile IBinder remote;
        private boolean bound;

        CarLink(LightsListener listener) {
            this.listener = listener;
        }

        void bind(Context ctx) {
            if (bound) return;
            try {
                bound = ctx.bindService(new Intent(ACTION_CAR).setPackage(PKG), this, Context.BIND_AUTO_CREATE);
            } catch (Throwable t) {
                DebugLog.log("bind car error: " + t);
            }
        }

        void unbind(Context ctx) {
            remote = null;
            if (!bound) return;
            bound = false;
            try {
                ctx.unbindService(this);
            } catch (Throwable ignored) {
            }
        }

        void readHeadlights() {
            final IBinder r = remote;
            if (r == null) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Parcel in = Parcel.obtain(), out = Parcel.obtain();
                    try {
                        in.writeInterfaceToken(DESC_CAR);
                        r.transact(TX_GET_HEADLIGHT_STATUS, in, out, 0);
                        out.readException();
                        listener.onHeadlights(out.readInt() != 0);
                    } catch (Throwable t) {
                        DebugLog.log("headlights error: " + t);
                    } finally {
                        in.recycle();
                        out.recycle();
                    }
                }
            }, "launcher-car").start();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remote = service;
            readHeadlights();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
        }
    }

    interface MediaListener {
        /** Called on a binder thread. */
        void onMediaChange(int type, String name, String info, int artW, int artH, byte[] art,
                           int index, int total, boolean popup);
        /** Called on a binder thread. position/duration in ms. */
        void onPlayStateChange(int type, int state, int position, int duration);
    }

    interface GpsListener {
        /** Called on a binder thread. speed is km/h according to the JanCar launcher. */
        void onGps(String longitude, String latitude, float accuracy, double altitude, float speed);
    }

    /** Base: binds an action of com.jancar.services, registers a callback binder on connect. */
    abstract static class Link implements ServiceConnection {
        private final String action;
        private final String name;
        private IBinder remote;
        private boolean bound;

        Link(String action, String name) {
            this.action = action;
            this.name = name;
        }

        void bind(Context ctx) {
            if (bound) return;
            try {
                Intent i = new Intent(action).setPackage(PKG);
                bound = ctx.bindService(i, this, Context.BIND_AUTO_CREATE);
                DebugLog.log("bind " + name + " -> " + bound);
            } catch (Throwable t) {
                bound = false;
                DebugLog.log("bind " + name + " error: " + t);
            }
        }

        void unbind(Context ctx) {
            IBinder r = remote;
            remote = null;
            if (r != null) {
                try {
                    unregister(r);
                } catch (Throwable t) {
                    DebugLog.log("unregister " + name + " error: " + t);
                }
            }
            if (bound) {
                bound = false;
                try {
                    ctx.unbindService(this);
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName cn, IBinder service) {
            remote = service;
            try {
                register(service);
                DebugLog.log("connected " + name + " (" + cn.flattenToShortString() + ")");
            } catch (Throwable t) {
                DebugLog.log("register " + name + " error: " + t);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName cn) {
            remote = null;
            DebugLog.log("disconnected " + name);
        }

        abstract void register(IBinder service) throws RemoteException;

        abstract void unregister(IBinder service) throws RemoteException;

        static void call(IBinder remote, String descriptor, int code, IBinder arg, boolean hasArg)
                throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(descriptor);
                if (hasArg) data.writeStrongBinder(arg);
                remote.transact(code, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }

    /** IMedia: registerMediaInfoCallback = 5, unRegisterMediaInfoCallback = 6, requestMediaInfoAndStateEvent = 22. */
    static final class MediaLink extends Link {
        private static final String DESC = "com.jancar.services.media.IMedia";
        private final Binder callback;

        MediaLink(final MediaListener listener) {
            super(ACTION_MEDIA, "media");
            callback = new Binder() {
                private static final String CB = "com.jancar.services.media.IMediaInfoCallback";

                {
                    attachInterface(null, CB);
                }

                @Override
                protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                        throws RemoteException {
                    switch (code) {
                        case 1: // onMediaChange
                            data.enforceInterface(CB);
                            listener.onMediaChange(data.readInt(), data.readString(), data.readString(),
                                    data.readInt(), data.readInt(), data.createByteArray(),
                                    data.readInt(), data.readInt(), data.readInt() != 0);
                            if (reply != null) reply.writeNoException();
                            return true;
                        case 2: // onPlayStateChange
                            data.enforceInterface(CB);
                            listener.onPlayStateChange(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                            if (reply != null) reply.writeNoException();
                            return true;
                        case 3: // onMediaZoneChanged(int, int)
                        case 4: // onCurrentShownMediaTypeChanged(int)
                            data.enforceInterface(CB);
                            if (reply != null) reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                }
            };
        }

        @Override
        void register(IBinder service) throws RemoteException {
            call(service, DESC, 5, callback, true);
            call(service, DESC, 22, null, false);
        }

        @Override
        void unregister(IBinder service) throws RemoteException {
            call(service, DESC, 6, callback, true);
        }
    }

    /** ISystem: registerGpsLocationInfoListener = 22, unregisterGpsLocationInfoListener = 23. */
    static final class SystemLink extends Link {
        private static final String DESC = "com.jancar.services.system.ISystem";
        private final Binder callback;

        SystemLink(final GpsListener listener) {
            super(ACTION_SYSTEM, "system");
            callback = new Binder() {
                private static final String CB = "com.jancar.services.system.IGpsCallback";

                {
                    attachInterface(null, CB);
                }

                @Override
                protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                        throws RemoteException {
                    switch (code) {
                        case 1: // onGpsLocationInfoChanged(String lon, String lat, float acc, double alt, float speed)
                            data.enforceInterface(CB);
                            listener.onGps(data.readString(), data.readString(), data.readFloat(),
                                    data.readDouble(), data.readFloat());
                            if (reply != null) reply.writeNoException();
                            return true;
                        case 2: // onGpsCountChanged(int, int, int, int)
                            data.enforceInterface(CB);
                            if (reply != null) reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                }
            };
        }

        @Override
        void register(IBinder service) throws RemoteException {
            call(service, DESC, 22, callback, true);
        }

        @Override
        void unregister(IBinder service) throws RemoteException {
            call(service, DESC, 23, callback, true);
        }
    }
}
