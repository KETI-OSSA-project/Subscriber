package com.example.messagebroker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class BoundServiceHelper<T> {
    // 앱 내부에서 서비스 연결 상태를 관리하기 위한 리스너입니다. (IMessagingServiceConnectionListener 와 착각하면 안됌)
    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
    }

    private final Context context;
    private final ComponentName serviceComponent;
    private final BinderConverter<T> binderConverter;
    private final AtomicReference<T> serviceRef = new AtomicReference<>();
    private boolean bound = false;
    private ConnectionListener externalListener;
    private Handler externalHandler;

    public interface BinderConverter<T> {
        T convert(IBinder binder);
    }

    // connection: ServiceConnection 클래스를 상속받는 익명 클래스
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
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

    // 생성자
    private BoundServiceHelper(@NonNull Context context,
                              @NonNull ComponentName serviceComponent,
                              @NonNull BinderConverter<T> binderConverter) {
        this.context = context.getApplicationContext();
        this.serviceComponent = serviceComponent;
        this.binderConverter = binderConverter;
        bindService();
    }

    // 팩토리 메서드: BoundServiceHelper 인스턴스를 생성합니다.
    public static <T> BoundServiceHelper<T> newInstance(@NonNull Context context,
                                                        @NonNull ComponentName serviceComponent,
                                                        @NonNull BinderConverter<T> binderConverter) {
        return new BoundServiceHelper<>(context, serviceComponent, binderConverter);
    }

    public void bindService() {
        Intent intent = new Intent();
        intent.setComponent(serviceComponent);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void close() {
        if (bound) {
            context.unbindService(connection);
            bound = false;
            serviceRef.set(null);
        }
    }

    @Nullable
    // PROXY: This method returns the service instance if bound, or null if not bound.
    public T get() {
        return serviceRef.get();
    }

    public void setExternalConnectionListener(@Nullable ConnectionListener listener, @Nullable Handler handler) {
        this.externalListener = listener;
        this.externalHandler = handler;
        // 연결되어 있다면 즉시 콜백 호출
        if (listener != null && bound) {
            notifyExternalConnected();
        }
    }

    private void notifyExternalConnected() {
        if (externalListener != null) {
            Handler handler = externalHandler != null ? externalHandler : new Handler(Looper.getMainLooper());
            handler.post(() -> externalListener.onConnected());
        }
    }

    private void notifyExternalDisconnected() {
        if (externalListener != null) {
            Handler handler = externalHandler != null ? externalHandler : new Handler(Looper.getMainLooper());
            handler.post(() -> externalListener.onDisconnected());
        }
    }
}
