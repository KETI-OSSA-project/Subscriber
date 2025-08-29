package com.example.broker;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import android.util.Log;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class MessagingManager implements AutoCloseable {
    private final BoundServiceHelper<IMessagingService> serviceHelper;
    private final String applicationId;
    private final String applicationPackageName;

    // MessageListener -> Stub 캐싱용 맵 추가
    private final ConcurrentHashMap<MessageListener, IMessageListener.Stub> stubMap = new ConcurrentHashMap<>();

    private MessagingManager(@NonNull Context context) {
        Objects.requireNonNull(context);
        // serviceHelper = BoundServiceHelper 객체
        serviceHelper = BoundServiceHelper.newInstanceWithAction(context,
                Constants.MESSAGING_SERVICE_ACTION,
                IMessagingService.Stub::asInterface);
        applicationId = context.getPackageName();
        applicationPackageName = applicationId.replace('.', '/');
    }

    @NonNull
    public static MessagingManager getMessagingManager(Context context) {
        return new MessagingManager(context);
    }

    public boolean isConnected() {
        IMessagingService service = serviceHelper.get();
        return service != null;
    }

    public void setMessagingServiceConnectionListener(
            @Nullable MessagingServiceConnectionListener listener, @Nullable Handler handler) {
        serviceHelper.setExternalConnectionListener(buildConnectionListener(listener), handler);
    }

    @Nullable
    private BoundServiceHelper.ConnectionListener buildConnectionListener(
            @Nullable MessagingServiceConnectionListener listener) {
        if (listener == null) return null;
        // 서비스: onServiceConnected -> 클라이언트: listener.onConnected
        return new BoundServiceHelper.ConnectionListener() {
            @Override
            public void onConnected() { listener.onConnected(); }
            @Override
            public void onDisconnected() { listener.onDisconnected(); }
        };
    }

    @Override
    public void close() {
        serviceHelper.close();
    }

    @SuppressWarnings("checkstyle:javadocmethod")
    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
    public boolean subscribe(@NonNull MessageListener client, @NonNull Filter filter) {
        return subscribe(client, filter, null);
    }

    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
    public boolean subscribe(@NonNull MessageListener client, @NonNull Filter filter, @Nullable Handler handler) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(filter);
        IMessagingService service = serviceHelper.get();
        if (service == null) {
            throw new RuntimeException("MessagingService가 아직 바인딩되지 않았습니다. 연결을 기다려주세요.");
        }
        try {
            IMessageListener.Stub messageListener = getOrCreateStub(client, handler);
            return service.subscribe(messageListener, filter);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private IMessageListener.Stub getOrCreateStub(MessageListener messageListener, Handler handler) {
        IMessageListener.Stub stub = stubMap.get(messageListener);
        if (stub == null) {
            stub = createMessageListenerProxy(messageListener, handler);
            stubMap.put(messageListener, stub);
        }
        return stub;
    }

    private IMessageListener.Stub createMessageListenerProxy(MessageListener messageListener, Handler handler) {
        return new IMessageListener.Stub() {
            @Override
            public void onMessage(Message message) throws RemoteException {
                if (handler != null) handler.post(() -> messageListener.onMessage(message));
                else messageListener.onMessage(message);
            }
        };
    }

    @SuppressWarnings("checkstyle:javadocmethod")
    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
    public boolean unsubscribe(@NonNull MessageListener client, @NonNull Filter filter) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(filter);
        IMessagingService service = serviceHelper.get();
        if (service == null) {
            throw new RuntimeException("MessagingService가 아직 바인딩되지 않았습니다. 연결을 기다려주세요.");
        }
        try {
            IMessageListener.Stub messageListener = getOrCreateStub(client, null);
            return service.unsubscribe(messageListener, filter);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public interface MessagingServiceConnectionListener {
        void onConnected();
        void onDisconnected();
    }

    public interface MessageListener {
        void onMessage(Message message);
    }

    // 바인딩 주소 정리한 곳
    public static class Constants {
        public static final String MESSAGE_BROKER_PACKAGE = "com.example.broker";
        public static final String MESSAGING_SERVICE = "com.example.broker.MessagingService";
        public static final String MESSAGING_SERVICE_ACTION = "com.example.broker.MessagingService";
        public static final String MESSAGE_SUBSCRIBE_PERMISSION = "com.example.permission.MESSAGE_SUBSCRIBE";
    }

    // BoundServiceHelper 통합
    public static class BoundServiceHelper<T> {
        private static final String TAG = "BoundServiceHelper";
        public interface ConnectionListener {
            void onConnected();
            void onDisconnected();
        }
        public interface BinderConverter<T> {
            T convert(android.os.IBinder binder);
        }
        private final Context context;
        private final ComponentName serviceComponent;
        private final BinderConverter<T> binderConverter;
        private final java.util.concurrent.atomic.AtomicReference<T> serviceRef = new java.util.concurrent.atomic.AtomicReference<>();
        private boolean bound = false;
        private ConnectionListener externalListener;
        private Handler externalHandler;
        private final android.content.ServiceConnection connection = new android.content.ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
                T service = binderConverter.convert(binder);
                serviceRef.set(service);
                bound = true;
                notifyExternalConnected();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
                serviceRef.set(null);
                notifyExternalDisconnected();
            }
        };

        // 바인딩 관리자
        private BoundServiceHelper(@NonNull Context context, @Nullable ComponentName serviceComponent, @Nullable String action, @NonNull BinderConverter<T> binderConverter) {
            this.context = context.getApplicationContext();
            this.serviceComponent = serviceComponent;
            this.binderConverter = binderConverter;
            if (action != null) {
                bindServiceWithAction(action); // binding with action
            } else {
                bindService();
            }
        }
        public static <T> BoundServiceHelper<T> newInstanceWithAction(@NonNull Context context, @NonNull String action, @NonNull BinderConverter<T> binderConverter) {
            return new BoundServiceHelper<>(context, null, action, binderConverter);
        }
        public void bindService() {
            try {
                android.content.Intent intent = new android.content.Intent();
                intent.setComponent(serviceComponent);
                boolean success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!success) {
                    Log.w(TAG, "Failed to bind to service: " + serviceComponent);
                    notifyExternalDisconnected();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while binding to service", e);
                notifyExternalDisconnected();
            }
        }
        // binding
        public void bindServiceWithAction(String action) {
            try {
                android.content.Intent intent = new android.content.Intent();
                intent.setComponent(new ComponentName("com.example.broker", "com.example.broker.MessagingService"));
                boolean success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!success) {
                    Log.w(TAG, "Failed to bind to service with action: " + action);
                    notifyExternalDisconnected();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while binding to service with action", e);
                notifyExternalDisconnected();
            }
        }
        public void setExternalConnectionListener(ConnectionListener listener, Handler handler) {
            this.externalListener = listener;
            this.externalHandler = handler;
        }
        private void notifyExternalConnected() {
            if (externalListener != null) {
                if (externalHandler != null) {
                    externalHandler.post(externalListener::onConnected);
                } else {
                    externalListener.onConnected();
                }
            }
        }
        private void notifyExternalDisconnected() {
            if (externalListener != null) {
                if (externalHandler != null) {
                    externalHandler.post(externalListener::onDisconnected);
                } else {
                    externalListener.onDisconnected();
                }
            }
        }
        public T get() {
            return serviceRef.get();
        }
        public void close() {
            if (bound) {
                context.unbindService(connection);
                bound = false;
            }
        }
    }
}
