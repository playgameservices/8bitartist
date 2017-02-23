/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.example.eightbitartist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * A wrapper for a GoogleApiClient that communicates with the Nearby Connections API. The
 * NearbyClient can be in one of two configurations:
 * 1) Host - this is the coordinating hub. The host will maintain a single connection with
 * each client in the match. The clients do not maintain any direct connections to each other.
 * The host can 'broadcast' a message from one client to all others to give the appearance that
 * the network is densely connected. The Host tracks all participants that enter and leave
 * the match and sends the appropriate messages to the Clients.
 * <p>
 * 2) Client - a client is connected only to the Host.  When the client wants to communicate
 * with another Client, it asks the Host to broadcast a message.
 */
public class NearbyClient implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Connections.MessageListener {

    private Connections.ConnectionRequestListener myConnectionRequestListener =
            new Connections.ConnectionRequestListener() {
                @Override
                public void onConnectionRequest(String remoteEndpointId, String
                        remoteEndpointName, byte[] bytes) {
                    NearbyClient.this.onConnectionRequest(remoteEndpointId,
                            remoteEndpointName, bytes);
                }
            };
    private Connections.EndpointDiscoveryListener myEndpointDiscoveryListener =
            new Connections.EndpointDiscoveryListener() {
                @Override
                public void onEndpointFound(String endpointId,
                                            String serviceId,
                                            String name) {
                    NearbyClient.this.onEndpointFound(endpointId, serviceId,
                            name);
                }

                @Override
                public void onEndpointLost(String remoteEndpointId) {
                    NearbyClient.this.onEndpointLost(remoteEndpointId);
                }
            };

    interface NearbyClientListener {

        /**
         * GoogleApiClient is connected, safe to start using the Nearby API through the client.
         */
        void onServiceConnected();

        /**
         * Connected to a remote endpoint.
         *
         * @param endpointId   the endpoint id of the remote endpoint.
         * @param endpointName the name of the remote endpoint.
         */
        void onConnectedToEndpoint(String endpointId, String
                endpointName);

        /**
         * Disconnected from a remote endpoint.
         *
         * @param endpointId the id of the remote endpoint.
         * @param deviceId   the device id of the remote endpoint.
         */
        void onDisconnectedFromEndpoint(String endpointId, String deviceId);

        /**
         * Received a message from a remote endpoint.
         *
         * @param remoteEndpointId the id of the remote endpoint.
         * @param payload          the message contents, as an array of bytes,
         */
        void onMessageReceived(String remoteEndpointId, byte[] payload);
    }

    private static final String TAG = NearbyClient.class.getSimpleName();

    public static final int STATE_IDLE = 8000;
    public static final int STATE_DISCOVERING = 8001;
    public static final int STATE_ADVERTISING = 8002;

    // The Google API Client for connecting to the Nearby Connections API
    private GoogleApiClient mGoogleApiClient;

    // The Context for displaying Toasts and Dialogs
    private Context mContext;

    // True if this is a Host, false if this is a Client.
    private boolean mIsHost;

    // A listener to receive events such as messages and connect/disconnect events.
    private NearbyClientListener mListener;

    // A set of all connected clients, used by the Host.
    private HashMap<String, DrawingParticipant> mConnectedClients = new HashMap<>();

    // The id of the Host, used by the client.
    private String mHostId;

    // The state of the NearbyClient (one of STATE_IDLE, STATE_DISCOVERING, or STATE_ADVERTISING)
    private int mState;

    // List dialog to display available endpoints
    private MyListDialog mListDialog;

    /**
     * Create a new NearbyClient.
     *
     * @param context  the creating context, generally an Activity or Fragment.
     * @param isHost   true if this client should act as host, false otherwise.
     * @param listener a NearbyClientListener to be notified of events.
     */
    public NearbyClient(Context context, boolean isHost, NearbyClientListener listener) {
        mContext = context;
        mIsHost = isHost;
        mListener = listener;
        mState = STATE_IDLE;

        mGoogleApiClient = new GoogleApiClient.Builder(mContext, this, this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Begin advertising for Nearby Connections.
     */
    public void startAdvertising() {
        long NO_TIMEOUT = 0L;

        Nearby.Connections.startAdvertising(mGoogleApiClient, null, null,
                NO_TIMEOUT, myConnectionRequestListener)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(Connections.StartAdvertisingResult result) {
                        if (result.getStatus().isSuccess()) {
                            Log.d(TAG, "Advertising as " +
                                    result.getLocalEndpointName());
                            mState = STATE_ADVERTISING;
                        } else {
                            Log.w(TAG, "Failed to start advertising: " +
                                    result.getStatus());
                            mState = STATE_IDLE;
                        }
                    }
                });
    }

    /**
     * Stop advertising for Nearby connections.
     */
    public void stopAdvertising() {
        mState = STATE_IDLE;
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
    }

    /**
     * Begin discovering Nearby Connections.
     *
     * @param serviceId the ID of advertising services to discover.
     */
    public void startDiscovery(String serviceId) {
        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, 0L,
                myEndpointDiscoveryListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Started discovery.");
                            mState = STATE_DISCOVERING;
                        } else {
                            Log.w(TAG, "Failed to start discovery: " + status);
                            mState = STATE_IDLE;
                        }
                    }
                });
    }

    /**
     * Stop discovering Nearby Connections.
     */
    public void stopDiscovery(String serviceId) {
        mState = STATE_IDLE;
        Nearby.Connections.stopDiscovery(mGoogleApiClient, serviceId);
    }

    /**
     * Disconnect the Google API client, disconnect from all connected
     * endpoints and stop
     * discovery/advertising when applicable.
     */
    public void onStop() {
        Log.d(TAG, "onStop");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Stop advertising or discovering, disconnect from all endpoints.
            mGoogleApiClient.disconnect();
            mState = STATE_IDLE;
        }
    }

    /**
     * Send a reliable message to the Host from a Client.
     */
    public void sendMessageToHost(String message) {
        try {
            byte[] payload = message.getBytes("UTF-8");
            Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                    mHostId, payload);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Cannot encode " + message + " to UTF-8?");
        }
    }

    /**
     * Send a reliable message to all other participants by routing
     * through the Host.
     *
     * @param message - the message to send.
     */
    public void broadcastMessage(String message) {
        Log.d(TAG, "broadcastMessage: " + message);
        if (mIsHost) {
            sendMessageToAll(message, null);
        } else {
            sendMessageToHost(message);
        }
    }

    /**
     * Send a message from the Host to all Clients, with the option to
     * exclude one participant.
     *
     * @param message     string to send as payload.
     * @param excludingId the participant ID of the participant to exclude.
     *                    Null to send to all.
     */
    public void sendMessageToAll(String message, String excludingId) {
        for (DrawingParticipant participant : mConnectedClients.values()) {
            if (!participant.getMessagingId().equals(excludingId)) {
                sendMessageTo(participant.getMessagingId(), message);
            }
        }
    }

    /**
     * Send a message to a specific participant.
     *
     * @param endpointId the endpoint ID of the participant that will
     *                   receive the message.
     * @param message    String to send as payload.
     */
    public void sendMessageTo(String endpointId, String message) {

        try {
            byte[] payload = message.getBytes("UTF-8");
            Log.d(TAG, "Sending message: " + message);
            Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                    endpointId, payload);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Cannot encode " + message + " to UTF-8?");
        }
    }

    /**
     * Send a connection request to a remote endpoint. If the request is successful, notify the
     * listener and add the connection to the Set.  Otherwise, show an error Toast.
     *
     * @param endpointId   the endpointID to connect to.
     * @param endpointName the name of the endpoint to connect to.
     */
    private void connectTo(final String endpointId,
                           final String endpointName) {
        Log.d(TAG, "connectTo:" + endpointId);
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, null,
                endpointId, null,
                new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String remoteEndpointId, Status status,
                                                     byte[] payload) {
                        Log.d(TAG, "onConnectionResponse:" +
                                remoteEndpointId + ":" + status);
                        if (status.isSuccess()) {
                            // Connection successful, notify listener
                            Toast.makeText(mContext, "Connected to: " + endpointName,
                                    Toast.LENGTH_SHORT).show();

                            mHostId = remoteEndpointId;
                            mConnectedClients.put(remoteEndpointId,
                                    new DrawingParticipant(
                                    remoteEndpointId, endpointName));
                            mListener.onConnectedToEndpoint(mHostId,
                                    endpointName);
                        } else {
                            // Connection not successful, show error
                            Toast.makeText(mContext, "Error: failed to connect.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }, this);
    }

    public int getState() {
        return mState;
    }

    public String getHostEndpointId() {
        return mHostId;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        mListener.onServiceConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    public void onConnectionRequest(final String remoteEndpointId, final
    String remoteName,
                                    byte[] payload) {
        Log.d(TAG, "onConnectionRequest:" + remoteEndpointId + ":" + remoteName);

        if (mIsHost) {
            // The host accepts all connection requests it gets.
            Nearby.Connections.acceptConnectionRequest(mGoogleApiClient,
                    remoteEndpointId,
                    null, this).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    Log.d(TAG, "acceptConnectionRequest:" + status + ":" +
                            remoteEndpointId);
                    if (status.isSuccess()) {
                        Toast.makeText(mContext, "Connected to " + remoteName,
                                Toast.LENGTH_SHORT).show();

                        // Record connection
                        DrawingParticipant participant =
                                new DrawingParticipant(remoteEndpointId,
                                remoteName);
                        mConnectedClients.put(remoteEndpointId, participant);

                        // Notify listener
                        mListener.onConnectedToEndpoint(remoteEndpointId, remoteName);
                    } else {
                        Toast.makeText(mContext, "Failed to connect to: " + remoteName,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // Clients should not be advertising and will reject all connection requests.
            Log.w(TAG, "Connection Request to Non-Host Device - Rejecting");
            Nearby.Connections.rejectConnectionRequest(mGoogleApiClient, remoteEndpointId);
        }
    }

    @Override
    public void onMessageReceived(String remoteEndpointId, byte[] payload, boolean isReliable) {
        Log.d(TAG, "onMessageReceived:" + remoteEndpointId);
        mListener.onMessageReceived(remoteEndpointId, payload);
    }

    @Override
    public void onDisconnected(String remoteEndpointId) {
        Log.d(TAG, "onDisconnected:" + remoteEndpointId);
        DrawingParticipant removed = mConnectedClients.remove(remoteEndpointId);
        if (removed != null) {
            mListener.onDisconnectedFromEndpoint(removed.getMessagingId(),
                    removed.getPersistentId());
        }
    }

    public void onEndpointFound(final String endpointId, final String serviceId,
                                String endpointName) {
        Log.d(TAG, "onEndpointFound:" + endpointId + ":" + serviceId + ":" +
                endpointName);

        // Ask the user if they would like to connect
        if (mListDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setTitle(mContext.getString(R.string.endpoint_found))
                    .setIcon(R.drawable.ic_launcher)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            mListDialog = new MyListDialog(mContext, builder, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String clickedEndpointName = mListDialog.getItemKey(which);
                    String clickedEndpointId = mListDialog.getItemValue(which);
                    connectTo(clickedEndpointId, clickedEndpointName);
                }
            });
        }

        mListDialog.addItem(endpointName, endpointId);
        mListDialog.show();
    }

    public void onEndpointLost(String remoteEndpointId) {
        Log.d(TAG, "onEndpointLost:" + remoteEndpointId);
        mListDialog.removeItemByValue(remoteEndpointId);
    }
}
