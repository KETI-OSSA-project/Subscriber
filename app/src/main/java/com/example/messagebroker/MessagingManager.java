package com.example.messagebroker;

/*
 * Copyright 2019 by Security and Safety Things GmbH
 *
 * All rights reserved,
 * also regarding any disposal, exploitation, reproduction,
 * editing, distribution, as well as in the event of
 * applications for industrial property rights.
 *
 * This software contains proprietary and confidential
 * information of Security and Safety Things GmbH.
 * The disclosure of confidential information without
 * express authorization is prohibited.
 *
 * This software may include Open Source Software.
 * As far as Open Source Software is included,
 * your rights for the respective Open Source Software
 * components are governed by the applicable
 * Open Source Software licenses.
 */

// import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import android.util.Log;

import java.util.Objects;
import java.util.UUID;

import static com.example.messagebroker.Constants.MESSAGE_BROKER_PACKAGE;
import static com.example.messagebroker.Constants.MESSAGING_SERVICE;
import static java.util.Objects.requireNonNull;

/**
 * Application interface for publishing and receiving messages from the message broker service.
 *
 * <p>All the requests to the message broker service are handled by the manager object.
 * The {@link #getMessagingManager(Context)} provides an instance of the manager object.
 * A connection listener object implementing the interface
 * {@link MessagingServiceConnectionListener} can be set using
 * {@link #setMessagingServiceConnectionListener(MessagingServiceConnectionListener, Handler)}
 * for getting callbacks about the connection status.
 *
 *
 * <h4>Usage:</h4>
 * <pre><code>
 * messagingManager = MessagingManager.getMessagingManager(context);
 * MessagingManager. MessagingServiceConnectionListener connectionListener = new MessagingManager
 * .MessagingServiceConnectionListener () {
 *     public void onConnected() {
 *        ...
 *     }
 *
 *     public void onDisconnected() {
 *        ...
 *     }
 * };
 * messagingManager.setMessagingServiceConnectionListener(connectionListener, null);
 * // After connectionListener.onConnected() ...
 * ...
 * // Cleanup resources
 * messagingManager.disconnect();
 * </code></pre>
 * Messaging Service supports two approaches for sending and receiving messages:
 * <ul>
 *     <li>Message Broker API</li>
 *     <li>Message Ports API</li>
 * </ul>
 * Note: There is no interoperability of messages between both the approaches.
 * <br/>
 * <br/>
 * <h2>Message Broker API</h2>
 *
 * <p>Following are the different types of requests that can be made to the messaging service
 * using the Message Broker API.
 *
 * <h3>Subscribing to messages</h3>
 *
 * <p>A client can subscribe to {@link Message} objects from the message broker service using the
 * method {@link #subscribe(MessageListener, Filter)}. Following this, the client will be able to
 * receive messages from the service if any {@link Message} object published to the service
 * satisfies the filter criteria encapsulated in the {@link Filter} object. The updates are
 * triggered on the callback {@link MessageListener#onMessage(Message)} of the client</p>
 *
 * <h4>Usage:</h4>
 * // Create a client for listening to updates from the message broker service
 * <pre><code>
 * MessagingManager.MessageListener client = new MessagingManager.MessageListener() {
 *      public void onMessage(Message message) {
 *             // Your code;
 *      }
 * };
 *
 * // Create a filter
 * String topic1 = // topic you wish to subscribe to
 * String publisherId1 = // fill if you want to get messages only from a specific publisher
 * Filter filter1 = new Filter.Builder().setTopic(topic1).setPublisherId(publisherId1).build();
 * // Subscribe for getting messages satisfying the above defined filter conditions.
 * boolean result = messagingManager.subscribe(client, filter1);
 * // result = true; on successful subscription.
 *
 * // Create another filter
 * String topic2 = // topic you wish to subscribe to
 * String publisherId2 = // fill if you want to get messages only from a specific publisher
 * Filter filter2 = new Filter.Builder().setTopic(topic2).setPublisherId(publisherId2).build();
 *
 * // Subscribe also to the second filter created above.
 * boolean result = messagingManager.subscribe(client, filter2);
 *
 * // The callback client.onMessage(Message) is triggered when any {@link Message} object
 * published to the message broker service satisfies either of the conditions in filter1 or in
 * filter2.
 *
 * // Subscribing filter1 again now returns false since the filter is already in the subscription
 * list of the client
 *
 * boolean result = messagingManager.subscribe(client, filter1); // returns false
 * </code></pre>
 *
 * <h3>Un-subscribing to messages</h3>
 * <p>A client can unsubscribe to an already subscribed {@link Filter} object using the method
 * {@link #unsubscribe(MessageListener, Filter)}. Following this, {@link Filter} will be removed
 * from the subscription. However, the client will continue to receive messages as per the
 * remaining filters in the subscription</p>
 *
 * <h4>Usage:</h4>
 * <pre><code>
 * // If already subscribed to filter1, filter2
 *
 * // Remove filter1 from subscription
 * // Works as: filter2 remains in the subscription list of the client.
 * boolean result = messagingManager.unsubscribe(client, filter1);
 * // returns true if filter1 is successfully removed from the subscription list of the client.
 *
 *
 * // Trying to unsubscribe a {@link Filter} object not in the subscription list returns false
 * boolean result = messagingManager.unsubscribe(client, filter1);
 * // Returns false since filter1 is no more in the subscription list of the client.
 *
 * // Unsubscribe with no filter argument to remove all the active subscriptions from the client
 * result = messagingManager.unsubscribe(client);
 * // returns true on successfully removing all the active subscriptions.
 * // false is returned if all the previously subscribed filters are already unsubscribed (no
 * // active subscriptions to unsubscribe).
 * </code></pre>
 *
 * <h3>Publishing messages</h3>
 * <p>A client can publish messages to the message broker service, using the function
 * {@link #publish(Message)}. The message broker service parses this data and then communicates
 * it to the interested clients</p>
 *
 * <p>Message broker service can handle asynchronous publish requests. The publish requests are
 * queued and processed one by one. The order of processing may not be the same as the order in
 * which the publish requests are received.
 *
 * <h4>Usage:</h4>
 * <pre><code>
 * // Create a new {@link Message} object
 * messagingManager.publish(message);
 * </code></pre>
 *
 * <h3>Publishing messages with response</h3>
 * <p>A client can publish messages with optional response topic and correlation data set. The
 * response topic represents the topic on which the responses from the receivers of the message
 * are expected. Both the actual topic of the initial publish and the response topic can have one or
 * more subscribers. It is a good practice for the publisher to subscribe on the contained response
 * topic before publishing the message.</p>
 *
 * <p>The publisher can use the correlation data for identifying which specific publish a
 * response that is received later relates. Response topics can be used without
 * correlation data. Using the correlation data makes it possible for the publisher to handle
 * asynchronous responses that can possibly be sent from multiple responders.</p>
 *
 * <h4>Usage:</h4>
 * <pre><code>
 * // Publisher subscribes to response topic
 * String responseTopic = // topic you wish to subscribe for responses
 * Filter filter = new Filter.Builder().setTopic(responseTopic).build();
 * MessagingManager.MessageListener publisherClient = new MessagingManager.MessageListener() {
 *      public void onMessage(Message message) {
 *          byte[] correlationData = message.getCorrelationData();
 *          // Use correlationData for identifying a response.
 *      }
 * };
 * messagingManager.subscribe(publisherClient, filter);
 *
 * // Publish a {@link Message} with response topic and correlation data attached
 * Message message = new Message.Builder().setTopic(publishTopic).setResponseTopic(responseTopic)
 * .setCorrelationData("someCorrelationData".getBytes()).build();
 * messagingManager.publish(message);
 *
 * // Responder that has subscribed to publishTopic will get a message and can reply to the
 * publisher by using the {@link Message#getResponseTopic()} and
 * {@link Message#getCorrelationData()}
 * MessagingManager.MessageListener responderClient = new MessagingManager.MessageListener() {
 *      public void onMessage(Message message) {
 *          String responseTopic = message.getResponseTopic();
 *          byte[] correlationData = message.getCorrelationData();
 *          Message responseMessage = new Message.Builder().setTopic(responseTopic)
 *          .setCorrelationData(correlationData).build();
 *          messagingManager.publish(responseMessage);
 *      }
 * };
 * </code></pre>
 *
 * <h2>Message Ports API</h2>
 *
 * <p>To start with, a {@code MessagingManifest.xml} needs to be written and added to the assets/
 * folder of the application. The content of the XML depends on the type of request to the
 * Messaging Service.
 * </p>
 *
 * <p>Following are the different types of requests that can be made to the messaging service
 * using the Message Ports API.</p>
 *
 * <h3>Send Message using {@link OutPort}</h3>
 *
 * <p>An application can send {@link com.securityandsafetythings.messagebroker.ports.Message}s
 * to other application(s) using an {@link OutPort}.
 * All the other applications using an {@link InPort} matching to the {@link OutPort} will
 * receive a copy of the {@link com.securityandsafetythings.messagebroker.ports.Message}.</p>
 *
 * <p>To create an instance of {@link OutPort}, the application needs to have a {@code
 * MessagingManifest.xml} in the assets/ folder having contents similar to the below given XML,
 * declaring configuration of the {@link OutPort}.</p>
 *
 * <p>Example {@code MessagingManifest.xml} for {@link OutPort} with arbitrary topicA.</p>
 * <pre><code>
 * &lt;?xml version="1.0" encoding="UTF-8" standalone="no"?&gt;
 * &lt;Application schemaVersion="1.0"&gt;
 *     &lt;!--PortConfiguration is the node to describe the port used in the application. The use
 *     of its attributes are:
 *     name attribute is used to assign a unique arbitrary name "DataOutPort" to the port.
 *     type attribute is used to declare the type of the port as "OutPort".
 *     topic attribute is used to assign an arbitrary MQTT Topic "topicA".--&gt;
 *     &lt;PortConfiguration name="DataOutPort" type="OutPort" topic="topicA"&gt;
 *     &lt;/PortConfiguration&gt;
 * &lt;/Application&gt;
 * </code></pre>
 *
 * <p>Create {@link PortConfiguration} instance reading the configuration from XML, and
 * instantiate an {@link OutPort}:</p>
 * <pre><code>
 * OutPort outPort;
 * // Use the unique name "DataOutPort" used in the XML for the port configuration.
 * PortConfiguration outPortConfiguration
 * = PortConfiguration.createPortConfigurationFromAssets(getApplicationContext(),
 * "DataOutPort");
 *
 * try {
 *      outPort = mMessagingManager.createOutPort(outPortConfiguration)
 * } catch (MessageBrokerConnectionException e) {
 *      // handle the exception
 * }
 *
 * </code></pre>
 *
 * <p>Send a {@link com.securityandsafetythings.messagebroker.ports.Message} through
 * the outPort:</p>
 * <pre><code>
 * // Create a Message instance
 * Message message = new Message.Builder()
 *                     .setPayload(payload.getBytes())
 *                     .build();
 *
 * // Send the message
 * try {
 *      if (outPort != null){
 *                 outPort.send(message);
 *      }
 * } catch (MessageBrokerConnectionException e) {
 *     // Could not send the message.
 * }
 * </code></pre>
 *
 * <p>Delete the {@link OutPort} after use:</p>
 * <pre><code>
 * try {
 *      outPort.delete();
 *      outPort = null;
 * } catch (MessageBrokerConnectionException e) {
 *      // handle the exception
 * }
 * </code></pre>
 *
 * <h3>Receive a Message using {@link InPort}</h3>
 *
 * <p>An application can receive {@link com.securityandsafetythings.messagebroker.ports.Message}s
 * from other application(s) using an {@link InPort}.
 * The {@link InPort} will receive a copy of
 * the{@link com.securityandsafetythings.messagebroker.ports.Message} from all the other
 * {@link OutPort}s matching to the {@link InPort}.
 *
 * <p>To create an instance of {@link InPort}, the application needs to have a {@code
 * MessagingManifest.xml} in the assets/ folder having contents
 * similar to the below given XML, declaring the configuration of the {@link InPort}.</p>
 *
 * <p>Example {@code MessagingManifest.xml} for {@link InPort} with arbitrary topicA.</p>
 * <pre><code>
 * &lt;?xml version="1.0" encoding="UTF-8" standalone="no"?&gt;
 * &lt;Application schemaVersion="1.0"&gt;
 *     &lt;!--PortConfiguration is the node to describe the port used in the application. The use
 *     of its attributes are:
 *     name attribute is used to assign a unique arbitrary name "DataInPort" to the port.
 *     type attribute is used to declare the type of the port as "InPort".
 *     topic attribute is used to assign an arbitrary MQTT Topic filter "topicA".--&gt;
 *     &lt;PortConfiguration name="DataInPort" type="InPort" topic="topicA"&gt;
 *     &lt;/PortConfiguration&gt;
 * &lt;/Application&gt;
 * </code></pre>
 *
 * <p>Create {@link PortConfiguration} instance reading the configuration from XML, and
 * instantiate an {@link InPort}:</p>
 * <pre><code>
 * InPort inPort;
 * // Use the unique name "DataInPort" used in the XML for the port configuration.
 * PortConfiguration inPortConfiguration
 * = PortConfiguration.createPortConfigurationFromAssets(getApplicationContext(),
 * "DataInPort");
 *
 * try {
 *      inPort = mMessagingManager.createInPort(inPortConfiguration)
 * } catch (MessageBrokerConnectionException e) {
 *      // handle the exception
 * }
 *
 * </code></pre>
 *
 * <p>Receive a {@link com.securityandsafetythings.messagebroker.ports.Message} through
 * the inPort:</p>
 * <pre><code>
 * try {
 *      if (inPort != null){
 *          inPort.addMessageListener(new InPort.MessageListener() {
 *              public void onMessage( message) {
 *                  // Your code;
 *              }
 *          });
 *      }
 * } catch (MessageBrokerConnectionException e) {
 *     // Could not add the message listener
 * }
 * </code></pre>
 *
 * <p>Delete the {@link InPort} after use:</p>
 * <pre><code>
 * try {
 *      inPort.delete();
 *      inPort = null;
 * } catch (MessageBrokerConnectionException e) {
 *      // handle the exception
 * }
 * </code></pre>
 */
public class MessagingManager implements AutoCloseable {
    private static final String TAG = MessagingManager.class.getSimpleName();
    private static final ComponentName SERVICE =
            ComponentName.createRelative(MESSAGE_BROKER_PACKAGE,
                    MESSAGING_SERVICE);
    private final BoundServiceHelper<IMessagingService> serviceHelper;
    // Session ID is randomly generated for each MessagingManager instance to be used for the
    // message broker book-keeping logic
    private final UUID sessionId = UUID.randomUUID();
    private final String applicationId;
    private final String applicationPackageName;

    private MessagingManager(@NonNull Context context) {
        Objects.requireNonNull(context);
        serviceHelper = BoundServiceHelper.newInstance(context,
                SERVICE,
                IMessagingService.Stub::asInterface);

        // Bind to the service. -> onServiceConnected()가 호출되기 전까지 Proxy == null (비동기)
        serviceHelper.bindService();

        // Replace the '.' characters of the package name with the '/' characters. This is to
        // facilitate hierarchical filtering (specifically following specification described
        // in MQTT v.5.0 Sec 4.7) on the publisherId attribute of a Message Object.
        applicationId = context.getPackageName();
        applicationPackageName = applicationId.replace('.', '/');
    }

    /**
     * Creates an instance with callbacks.
     *
     * <p>The {@code listener} callbacks are executed on main thread of the app.
     *
     * <p>This method should only be called on the main thread! This is required to guarantee,
     * that the {@link MessagingManager} object is returned before any callback is received.
     * If the execution cannot be done on the main thread, use the
     * {@link #getMessagingManager(Context)} and
     * {@link #setMessagingServiceConnectionListener(MessagingServiceConnectionListener, Handler)}
     * method instead.
     *
     * @param context  of the caller
     * @param listener for service availability changes
     * @return instance of the manager
     * @throws RuntimeException on connection error (e.g. service does not exist)
     */
    @MainThread
//    @NonNull
//    public static MessagingManager getConnectedInstance(
//            @NonNull Context context, @NonNull MessagingServiceConnectionListener listener) {
//        Objects.requireNonNull(context);
//        Objects.requireNonNull(listener);
//        if (Looper.myLooper() != Looper.getMainLooper()) {
//            Log.w(TAG, "getConnectedInstance is not called on the main thread!" +
//                    " This may lead to inconsistent behavior.");
//        }
//        MessagingManager manager = new MessagingManager(context);
//        manager.setMessagingServiceConnectionListener(listener, null);
//        return manager;
//    }

    /**
     * Creates an instance.
     *
     * @param context of the caller.
     * @return instance of the manager
     * @throws RuntimeException on connection error (e.g. service does not exist)
     */
    @NonNull
    public static MessagingManager getMessagingManager(Context context) {
        return new MessagingManager(context);
    }

    /**
     * Register a callback for service availability state changes.
     *
     * <p>When you register the {@code listener} for the first time, the {@code onConnected}
     * callback is invoked at once, if the manager is already connected to the service.
     *
     * @param listener to received availability state changes, or {@code null} to unregister a
     *                 previously registered listener
     * @param handler  on which the callback will be invoked, or {@code null} to use the main thread
     *                 {@link Looper#getMainLooper()}. The value is ignored when {@code listener}
     *                 is {@code null}.
     */
    public void setMessagingServiceConnectionListener(
            @Nullable MessagingServiceConnectionListener listener, @Nullable Handler handler) {
        serviceHelper.setExternalConnectionListener(buildConnectionListener(listener), handler);
    }

    @Nullable
    private BoundServiceHelper.ConnectionListener buildConnectionListener(
            @Nullable MessagingServiceConnectionListener listener) {
        if (listener == null) {
            return null;
        }
        return new BoundServiceHelper.ConnectionListener() {
            @Override
            public void onConnected() {
                listener.onConnected();
            }

            @Override
            public void onDisconnected() {
                listener.onDisconnected();
            }
        };
    }

    /**
     * Register a callback for service availability state changes using AIDL interface.
     *
     * @param listener AIDL callback interface for connection state changes
     */
//    public void setMessagingServiceConnectionListener(
//            @Nullable IMessagingServiceConnectionListener listener) {
//        if (listener == null) {
//            // 기존 연결 리스너 제거
//            serviceHelper.setExternalConnectionListener(null, null);
//            return;
//        }
//
//        // AIDL 콜백을 BoundServiceHelper의 ConnectionListener로 변환
//        BoundServiceHelper.ConnectionListener connectionListener =
//            new BoundServiceHelper.ConnectionListener() {
//                @Override
//                public void onConnected() {
//                    try {
//                        listener.onConnected();
//                    } catch (android.os.RemoteException e) {
//                        Log.e(TAG, "Error calling onConnected callback", e);
//                    }
//                }
//
//                @Override
//                public void onDisconnected() {
//                    try {
//                        listener.onDisconnected();
//                    } catch (android.os.RemoteException e) {
//                        Log.e(TAG, "Error calling onDisconnected callback", e);
//                    }
//                }
//            };
//
//        serviceHelper.setExternalConnectionListener(connectionListener, null);
//    }

    /**
     * Same as {@link #close()}.
     */
//    public void disconnect() {
//        close();
//    }

    /**
     * Unbind from the service and cleanup all resources.
     */
    @Override
    public void close() {
        serviceHelper.close();
    }

    /**
     * Publish a {@link Message} object to the message broker service.
     * Requires <code>com.securityandsafetythings.permission.MESSAGE_PUBLISH</code> permission.
     *
     * @param message Message
     * @throws SecurityException Can be thrown if the caller has no android permission
     *                           <code>com.securityandsafetythings.permission
     *                           .MESSAGE_PUBLISH</code>.
     */
    @RequiresPermission(Constants.MESSAGE_PUBLISH_PERMISSION)
    // 동기 메서드이자 Wrapper 메서드
    public void publish(@NonNull Message message) {
        Objects.requireNonNull(message);
        // message.setPublisherId(applicationPackageName);

        // 바인딩이 안된 경우 (publish가 호출되기 전에 연결이 끊어진 경우)
        if (serviceHelper == null) {
            Log.w(TAG, "MessagingService가 아직 연결되지 않았습니다.");
            return;
        }

        IMessagingService service = serviceHelper.get();
        if (service == null) {
            Log.w(TAG, "MessagingService가 아직 바인딩되지 않았습니다. 연결을 기다려주세요.");
            return;
        }

        try {
            service.publish(message);
            Log.d(TAG, "메시지 발행 성공: " + message.getTopic());
        } catch (RemoteException e) {
            Log.e(TAG, "메시지 발행 중 RemoteException 발생", e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }

    /**
     * Subscribe for receiving messages from the message broker service satisfying rules set in the
     * {@link Filter} object.
     * For the first subscribe() call on "client" the subscription is created with the
     * {@link Filter} object, all subsequent subscribe() calls on "client" will append the
     * {@link Filter} to existing subscription.
     * Filters in the subscription are independent of each other and logically ORed for
     * subscription.
     * Requires <code>com.securityandsafetythings.permission.MESSAGE_SUBSCRIBE</code> permission.
     *
     * @param client MessageListener
     * @param filter The {@link Filter} object for subscription.
     * @return true if the {@link Filter} object is successfully subscribed, false otherwise.
     * @throws MessageListenerException For the types {@link MessageListenerException}
     * @throws SecurityException        Can be thrown if the caller has no android permission
     *                                  <code>com.securityandsafetythings.permission
     *                                  .MESSAGE_SUBSCRIBE</code>.
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
//    public boolean
//    subscribe(@NonNull MessageListener client, @NonNull Filter filter)
//            throws MessageListenerException {
//        return subscribe(client, filter, null);
//    }

    /**
     * Subscribe for receiving messages from the message broker service satisfying rules set in the
     * {@link Filter} object.
     * For the first subscribe() call on "client" the subscription is created with the
     * {@link Filter} object, all subsequent subscribe() calls on "client" will append the
     * {@link Filter} to existing subscription.
     * Filters in the subscription are independent of each other and logically ORed for
     * subscription.
     * Requires <code>com.securityandsafetythings.permission.MESSAGE_SUBSCRIBE</code> permission.
     *
     * @param client  MessageListener
     * @param filter  The {@link Filter} object for subscription.
     * @param handler The {@link Handler} the listener callback should be executed by. If handler is
     *                null, the callback will be executed on arbitrary binder thread.
     * @return true if the {@link Filter} object is successfully subscribed, false otherwise.
     * @throws MessageListenerException For the types {@link MessageListenerException}
     * @throws SecurityException        Can be thrown if the caller has no android permission
     *                                  <code>com.securityandsafetythings.permission
     *                                  .MESSAGE_SUBSCRIBE</code>.
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
//    public boolean
//    subscribe(@NonNull MessageListener client, @NonNull Filter filter, @Nullable Handler handler)
//            throws MessageListenerException {
//        Objects.requireNonNull(client);
//        Objects.requireNonNull(filter);
//
//        try {
//            IMessageListener.Stub messageListener = createMessageListenerProxy(client, handler);
//            return handleResult(serviceHelper.get().subscribe(messageListener,
//                    Objects.hash(client, sessionId), filter, applicationPackageName));
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }


//    private boolean handleResult(MessageBrokerResult result) throws MessageListenerException {
//        switch (result) {
//            case FILTER_NOT_ADDED:
//            case FILTER_NOT_REMOVED:
//                return false;
//            case FILTER_ADDED:
//            case FILTER_REMOVED:
//            case ALL_FILTERS_REMOVED:
//                return true;
//            case LISTENER_NOT_SUBSCRIBED:
//                throw new MessageListenerException(
//                        MessageListenerException.Type.LISTENER_NOT_SUBSCRIBED);
//            case NULL_FILTER:
//            case NULL_LISTENER:
//            case NULL_PACKAGE_NAME:
//            default:
//                throw new RuntimeException(
//                        "Internal Error: incorrect result returned for this request: " +
//                                result.name());
//        }
//    }
//    private IMessageListener.Stub createMessageListenerProxy(MessageListener messageListener,
//                                                             Handler handler) {
//        IMessageListener.Stub messageListenerProxy =
//                new IMessageListener.Stub() {
//                    @Override
//                    public void onMessage(Message message) throws RemoteException {
//                        if (handler != null) {
//                            handler.post(() -> messageListener.onMessage(message));
//                        } else {
//                            messageListener.onMessage(message);
//                        }
//                    }
//                };
//        return messageListenerProxy;
//    }

    /**
     * Unsubscribe to a {@link Filter} object already subscribed by a {@link MessageListener}.
     * Requires <code>com.securityandsafetythings.permission.MESSAGE_SUBSCRIBE</code> permission.
     *
     * @param client MessageListener on which the un-subscribe operation is being carried out.
     * @param filter The {@link Filter} object to be unsubscribed.
     * @return true if the {@link Filter} object is successfully un-subscribed, false otherwise.
     * @throws MessageListenerException A {@link MessageListenerException} is thrown if the
     *                                  client was not subscribed to the service before
     * @throws SecurityException        if caller has no permission to unsubscribe
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
//    public boolean
//    unsubscribe(@NonNull MessageListener client, @NonNull Filter filter)
//            throws MessageListenerException {
//
//        Objects.requireNonNull(client);
//        Objects.requireNonNull(filter);
//
//        try {
//            return handleResult(serviceHelper.get().unsubscribe(Objects.hash(client, sessionId),
//                    filter));
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * Unsubscribe to all the {@link Filter} objects subscribed by a {@link MessageListener}.
     * Requires <code>com.securityandsafetythings.permission.MESSAGE_SUBSCRIBE</code> permission.
     *
     * @param client MessageListener on which the un-subscribe operation is being carried out.
     * @return true if all the {@link Filter} objects subscribed before are successfully
     * un-subscribed, false if all the previously subscribed filters are already unsubscribed
     * (no active subscriptions to unsubscribe).
     * @throws MessageListenerException A {@link MessageListenerException} is thrown if the
     *                                  client was not subscribed to the service before
     * @throws SecurityException        if caller has no permission to unsubscribe
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.MESSAGE_SUBSCRIBE_PERMISSION)
//    public boolean
//    unsubscribe(@NonNull MessageListener client)
//            throws MessageListenerException {
//        Objects.requireNonNull(client);
//        try {
//            return handleResult(serviceHelper.get().unsubscribeAll(Objects.hash(client,
//                    sessionId)));
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * Create an InPort. The provided {@link PortConfiguration} is used to configure the InPort.
     *
     * @param inPortConfiguration Configuration used for creating the InPort.
     * @return Instance of {@link InPort} which was created.
     * @throws MessageBrokerConnectionException if there is an error creating the connection.
     * @throws SecurityException                if the caller doesn't have
     *                                          <code>com.securityandsafetythings.permission
     *                                          .USE_MESSAGE_PORTS</code>
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.USE_MESSAGE_PORTS_PERMISSION)
//    public InPort createInPort(
//            @NonNull PortConfiguration inPortConfiguration)
//            throws MessageBrokerConnectionException {
//        requireNonNull(inPortConfiguration);
//        try {
//            PortResponse response = serviceHelper.get().createInPort(
//                    inPortConfiguration,
//                    applicationId,
//                    createMessagingServiceConnectionListener());
//            MessageBrokerResult result = response.getResult();
//            if (result == MessageBrokerResult.OK) {
//                return response.getInPort();
//            } else {
//                handlePortResult(result);
//                return null;
//            }
//        } catch (RemoteException e) {
//            throw new RuntimeException();
//        }
//    }

    /**
     * Create a port Group. The provided {@link PortConfiguration} is used to configure the InPort.
     *
     * @param portGroupName Name of the port group.
     * @param groupUid Uid of the group. For the purpose of persistence the uid of group should
     *                be persistently stored and passed over restarts.
     * @return Instance of {@link com.securityandsafetythings.messagebroker.ports.PortGroup} which
     * was created.
     * @throws MessageBrokerConnectionException if there is an error creating the group.
     * @throws SecurityException                if the caller doesn't have
     *                                          <code>com.securityandsafetythings.permission
     *                                          .USE_MESSAGE_PORT_GROUPS</code>
     * @hide
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @SystemApi
//    @RequiresPermission(Constants.USE_MESSAGE_PORT_GROUPS_PERMISSION)
//    public PortGroup createPortGroup(@NonNull String portGroupName, @NonNull String groupUid)
//            throws MessageBrokerConnectionException {
//        requireNonNull(portGroupName);
//        requireNonNull(groupUid);
//        try {
//            PortGroupResponse response = serviceHelper.get().createPortGroup(
//                    portGroupName,
//                    applicationId,
//                    groupUid,
//                    createMessagingServiceConnectionListener());
//            return response.getPortGroup();
//        } catch (RemoteException e) {
//            throw new RuntimeException();
//        }
//    }

    /**
     * Create an OutPort. The provided {@link PortConfiguration} is used to configure the OutPort.
     *
     * @param outPortConfiguration Configuration used for creating the InPort.
     * @return Instance of {@link OutPort} which was created.
     * @throws MessageBrokerConnectionException if there is an error creating the connection.
     * @throws SecurityException                if the caller doesn't have
     *                                          <code>com.securityandsafetythings.permission
     *                                          .USE_MESSAGE_PORTS</code>
     */
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
//    @SuppressWarnings("checkstyle:javadocmethod")
//    @RequiresPermission(Constants.USE_MESSAGE_PORTS_PERMISSION)
//    public OutPort createOutPort(
//            @NonNull PortConfiguration outPortConfiguration)
//            throws MessageBrokerConnectionException {
//        requireNonNull(outPortConfiguration);
//        try {
//            outPortConfiguration.setPublisherId(applicationPackageName);
//            PortResponse response = serviceHelper.get().createOutPort(
//                    outPortConfiguration,
//                    applicationId,
//                    createMessagingServiceConnectionListener());
//            MessageBrokerResult result = response.getResult();
//            if (result == MessageBrokerResult.OK) {
//                return response.getOutPort();
//            } else {
//                handlePortResult(result);
//                return null;
//            }
//        } catch (RemoteException e) {
//            throw new RuntimeException();
//        }
//    }

//    private IMessagingServiceConnectionListener createMessagingServiceConnectionListener() {
//        return new IMessagingServiceConnectionListener.Stub() {
//            @Override
//            public void onConnected() throws RemoteException {
//                //TODO: extend and reveal if this is needed
//            }
//
//            @Override
//            public void onDisconnected() throws RemoteException {
//                //TODO: extend and reveal if this is needed
//            }
//        };
//    }

    /**
     * Listener to handle availability state changes of the service.
     *
     * <p>The callback {@code onConnected} is called, when the service is available and ready
     * to be used. The callback {@code onDisconnected} is called, when the service has died. Any
     * calls may throw a {@link android.os.DeadObjectException} exception. The manager will
     * reconnect automatically.
     **/
    public interface MessagingServiceConnectionListener {
        /**
         * Remote service is available and can be used.
         */
        void onConnected();

        /**
         * Remote service has died. Do not use it anymore.
         */
        void onDisconnected();
    }

    /**
     * Interface for listening to messages from the message broker service. The client object
     * implementing the interface can subscribe to the message broker service for listening to
     * the {@link Message} updates sent by other applications or by the message broker service.
     * The callback {@link #onMessage} is triggered by the message broker service when a
     * {@link Message} is published to the service
     * satisfying the
     * filter rules set by the client. A sample code for subscribing a client with
     * the message broker service can be found in the documentation of {@link MessagingManager}
     */
//    public interface MessageListener {
//        /**
//         * Called when a {@link Message} received by the message broker service satisfies
//         * one or more {@link Filter} rules the client subscribed for.
//         *
//         * @param message contains the new {@link Message}
//         */
//        void onMessage(Message message);
//    }

//    private static void handlePortResult(MessageBrokerResult result)
//            throws MessageBrokerConnectionException {
//        switch (result) {
//            case OK:
//                return;
//            case INVALID_PORT_CONFIGURATION:
//                throw new MessageBrokerConnectionException(
//                        MessageBrokerConnectionException.Type.INVALID_PORT_CONFIGURATION);
//            case NOT_OK:
//            default:
//                throw new MessageBrokerConnectionException(
//                        MessageBrokerConnectionException.Type.UNKNOWN_ERROR);
//        }
//    }
//
}
