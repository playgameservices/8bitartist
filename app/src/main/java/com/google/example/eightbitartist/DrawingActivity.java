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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.eightbitartist.messages.ClearMessage;
import com.google.example.eightbitartist.messages.EPointMessage;
import com.google.example.eightbitartist.messages.GuessMessage;
import com.google.example.eightbitartist.messages.Message;
import com.google.example.eightbitartist.messages.MessageAdapter;
import com.google.example.eightbitartist.messages.ParticipantMessage;
import com.google.example.eightbitartist.messages.TurnMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * The main Activity for all of 8BitArtist.  This Activity can show the UI for creating a match as
 * well as the UI for playing a match. The updateViewVisibility() function controls the visible
 * layout based on the state of the game.
 * <p>
 * There are two types of game play for 8BitArtist:
 * 1) Online Mode - this mode uses the Play Game Services RealTime Multiplayer APIs to connect
 * up to 4 players at any location to play a real-time match. Players may leave once the game
 * begins, but no new players may join. Playing in this mode requires the player to be signed
 * in with his or her Play Games profile.
 * <p>
 * 2) Party Mode - this mode uses the Play Game Services Nearby Connections API to connect
 * any number of players who are connected to the same WiFi network. In this mode one player
 * acts as the 'host' to begin the game while any other players nearby can join or leave the
 * game at any time. Playing in this mode is totally anonymous and does not require the player
 * to be signed in.
 */
public class DrawingActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnInvitationReceivedListener,
        RoomUpdateListener,
        RealTimeMessageReceivedListener,
        RoomStatusUpdateListener,
        View.OnClickListener, DrawView.DrawViewListener {

    private static final String TAG = "DrawingActivity";

    // Intent codes used in startActivityForResult
    private final static int RC_SIGN_IN = 9001;
    private final static int RC_SELECT_PLAYERS = 10000;
    private final static int RC_ACHIEVEMENTS = 10002;
    private final static int RC_WAITING_ROOM = 10003;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Wrapper for a GoogleApiClient that interacts with the Nearby Connections API. This is
    // managed separately because sign-in is not required to connect to Nearby players.
    private NearbyClient mNearbyClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked ths sign-in button?
    private boolean mSignInClicked = false;

    // Should the sign-in flow be started automatically?
    private boolean mAutoStartSignInFlow = true;

    //  JSON Processing
    private Gson mMapper;

    // AlertDialog for showing messages to the user
    private AlertDialog mAlertDialog;

    // RealTime Multiplayer Room, null when not connected
    private Room mRoom;

    // Nearby Connections Data
    private boolean mIsHostingParty = false;
    private boolean mIsJoinedParty = false;
    private String mServiceId;

    // The player's participant id.  This maps to DrawingParticipant.persistentId
    private String mMyPersistentId;

    // It is the player's turn when (match turn number % num participants == my turn index)
    private int mMyTurnIndex;

    // The match turn number, monotonically increasing from 0
    private int mMatchTurnNumber = 0;

    // The eligible guess words for this turn
    private List<String> mTurnWords;

    // The index of the correct word
    private int mWordIndex = 0;

    // Set of participant IDs for players that have guessed this turn
    private HashSet<String> mGuessersThisTurn = new HashSet<>();

    // True if this player has already guessed this turn, false otherwise
    private boolean mHasGuessed = false;

    // Map of all participants in the match, with key being the participant ID for fast lookup
    private HashMap<String, DrawingParticipant> mParticipants = new HashMap<>();

    // Participants that were in this match at one time, but left
    private HashMap<String, DrawingParticipant> mOldParticipants = new HashMap<>();

    // Data to draw the DrawView
    private DrawView mDrawView;

    // All possible words for 8BitArtist
    private String[] mAllWords;

    // ProgressBar. TextView, and Handler used to show the time remaining to make a guess.
    private ProgressBar mGuessProgress;
    private TextView mGuessProgressText;
    private Handler mGuessProgressHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Set up guesser progress
        mGuessProgress = (ProgressBar) findViewById(R.id.guessProgress);
        mGuessProgressText = (TextView) findViewById(R.id.guessProgressText);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.startMatchButton).setOnClickListener(this);
        findViewById(R.id.partyModeHostButton).setOnClickListener(this);
        findViewById(R.id.partyModeJoinButton).setOnClickListener(this);
        findViewById(R.id.clearButton).setOnClickListener(this);
        findViewById(R.id.doneButton).setOnClickListener(this);

        // ListView item click listener for word guessing
        ((ListView) findViewById(R.id.listView))
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        makeGuess(position);
                    }
                });

        // Check the games configuration
        checkConfiguration(true);

        // Create the Google API Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // Service ID for Nearby Connections, set to App ID
        mServiceId = getString(R.string.app_id);

        // Create the object mapper
        mMapper = new GsonBuilder().registerTypeAdapter(Message.class,
                new MessageAdapter())
                .create();

        // Initialize DrawView and ColorChooser
        mDrawView = ((DrawView) findViewById(R.id.drawView));
        mDrawView.setListener(this);

        ((ColorChooser) findViewById(R.id.colorChooser))
                .setDrawView(((DrawView) findViewById(R.id.drawView)));

        // Create array of all words
        mAllWords = getResources().getString(R.string.words).split(",");
        mTurnWords = Arrays.asList(mAllWords);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: disconnecting GoogleApiClient(s)");
        if (isSignedIn()) {
            mGoogleApiClient.disconnect();
        }

        if (mNearbyClient != null) {
            mNearbyClient.onStop();
        }
    }

    @Override
    public void onBackPressed() {
        dismissSpinner();

        if (mNearbyClient != null && mNearbyClient.getState() == NearbyClient.STATE_DISCOVERING) {
            // Cancel discovery of Nearby Connections
            mNearbyClient.stopDiscovery(mServiceId);
        } else if (mRoom != null || mIsHostingParty || mIsJoinedParty) {
            // In a game, leave the game on back pressed
            leaveGame();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected: sign-in successful.");

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        // Get invitation from Bundle
        if (bundle != null) {
            Invitation invitation = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
            if (invitation != null) {
                onInvitationReceived(invitation);
            }
        }

        updateViewVisibility();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended: trying to reconnect.");
        mGoogleApiClient.connect();

        updateViewVisibility();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            Log.i(TAG, "onConnectionFailed: already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = false;

            Log.i(TAG, "onConnectionFailed: resolving connection failure.");
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.sign_in_failed))) {
                Log.i(TAG, "onConnectionFailed: could not resolve.");
                mResolvingConnectionFailure = false;
            }
        }

        updateViewVisibility();
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_achievements:
                Intent intent = Games.Achievements.getAchievementsIntent(mGoogleApiClient);
                startActivityForResult(intent, RC_ACHIEVEMENTS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Open the player selection UI for a RealTime Multiplayer game
     */
    private void onStartMatchClicked() {
        // Select between 1 and 3 players (not including the current one, so the game has 2-4 total)
        int minPlayers = 1;
        int maxPlayers = 3;
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient,
                minPlayers, maxPlayers, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    /**
     * Initialize a NearbyClient to advertise as a Nearby Connections host.
     */
    private void onHostPartyClicked() {
        // Show party mode layout
        mIsHostingParty = true;
        updateViewVisibility();

        if (mNearbyClient != null) {
            mNearbyClient.onStop();
        }

        // Start advertising for Nearby Connections
        mNearbyClient = new NearbyClient(this, true, new NearbyClient.NearbyClientListener() {

            @Override
            public void onServiceConnected() {
                Log.d(TAG, "Advertising for Nearby Connections");
                mNearbyClient.startAdvertising();
            }

            @Override
            public void onConnectedToEndpoint(String endpointId, String
                    endpointName) {

                mMyPersistentId = "";
                DrawingParticipant me = new DrawingParticipant("local", "Me");
                onParticipantConnected(me);

                // Add participant
                DrawingParticipant participant = new DrawingParticipant
                        (endpointId, endpointName);
                onParticipantConnected(participant);

                // Send all participants to all other participants
                for (DrawingParticipant dp : mParticipants.values()) {

                    ParticipantMessage msg = new ParticipantMessage(dp);
                    mNearbyClient.sendMessageToAll(mMapper.toJson(msg,
                            Message.class),
                            dp.getMessagingId());

                }

                if (mParticipants.size() <= 2) {
                    // Start the match if this is the first connection
                    startMatch();
                } else {
                    // Otherwise, send them the current game state
                    TurnMessage turnMsg = new TurnMessage(mMatchTurnNumber, mTurnWords, mWordIndex);
                    mNearbyClient.sendMessageTo(endpointId, mMapper.toJson
                            (turnMsg, Message.class));


                    beginMyTurn();
                }
                updateViewVisibility();
            }

            @Override
            public void onDisconnectedFromEndpoint(String endpointId, String deviceId) {
                // Tell other clients it was disconnected
                ParticipantMessage msg = new ParticipantMessage(mParticipants.get(deviceId));
                msg.setIsJoining(false);
                mNearbyClient.broadcastMessage(mMapper.toJson(msg));

                DrawingActivity.this.onParticipantDisconnected(endpointId, deviceId);
            }

            @Override
            public void onMessageReceived(String remoteEndpointId, byte[] payload) {
                // The host forwards most messages to all clients.
                mNearbyClient.sendMessageToAll(new String(payload),
                        remoteEndpointId);

                // Parse messages normally
                DrawingActivity.this.onMessageReceived(payload);
            }
        });
    }

    /**
     * Initialize a NearbyClient to discovery an advertising Nearby Connections host.
     */
    private void onJoinPartyClicked() {
        Log.d(TAG, "onJoinPartyClicked:" + mServiceId);
        showSpinner();

        if (mNearbyClient != null) {
            mNearbyClient.onStop();
        }

        // Start looking for Nearby Connections
        mNearbyClient = new NearbyClient(this, false, new NearbyClient.NearbyClientListener() {
            @Override
            public void onServiceConnected() {
                Log.d(TAG, "Trying to find party host.");
                mNearbyClient.startDiscovery(mServiceId);
            }

            @Override
            public void onConnectedToEndpoint(String hostId, String hostName) {
                Log.d(TAG, "onConnectedToEndpoint");
                dismissSpinner();

                mNearbyClient.stopDiscovery(mServiceId);
                mIsJoinedParty = true;

                // Add self to participants
                mMyPersistentId = "localhost";
                DrawingParticipant me = new DrawingParticipant("local", "Me");
                onParticipantConnected(me);

                // Add host to participants
                DrawingParticipant participant = new DrawingParticipant(hostId, hostName);
                onParticipantConnected(participant);

                // Start the appropriate turn
                beginMyTurn();
            }

            @Override
            public void onDisconnectedFromEndpoint(String endpointId, String deviceId) {
                DrawingActivity.this.onParticipantDisconnected(endpointId, deviceId);
            }

            @Override
            public void onMessageReceived(String remoteEndpointId, byte[] payload) {
                DrawingActivity.this.onMessageReceived(payload);
            }
        });
    }

    /**
     * Controls the UI for all game screens. Can show one of the following based on the game state:
     * 1) Signed out home screen UI
     * 2) Signed in home screen UI
     * 3) In-game UI for the Artist
     * 4) In-game UI for a Guesser
     */
    private void updateViewVisibility() {
        boolean inParty = mIsHostingParty || mIsJoinedParty;
        boolean inRoom = (mRoom != null);
        boolean inGame = inParty || inRoom;

        // Keep screen on while in game
        if (inGame) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (!isSignedIn() && !inGame) {
            // Show the home screen signed out
            ((TextView) findViewById(R.id.name_field)).setText("");
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
            findViewById(R.id.partyModeLayout).setVisibility(View.VISIBLE);
        } else if (isSignedIn() && !inGame) {
            // Show home screen signed in
            ((TextView) findViewById(R.id.name_field)).setText("You are: "
                    + Games.Players.getCurrentPlayer(mGoogleApiClient).getDisplayName());
            findViewById(R.id.login_layout).setVisibility(View.GONE);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
            findViewById(R.id.partyModeLayout).setVisibility(View.VISIBLE);
        } else if (inGame) {
            // Show the in-game layout
            findViewById(R.id.login_layout).setVisibility(View.GONE);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.partyModeLayout).setVisibility(View.GONE);

            // Set proper UI for either artist or guesser
            if (isMyTurn()) {
                setArtistUI();
            } else {
                setGuessingUI();
            }
        }
    }

    private void showSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
    }

    private void dismissSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.GONE);
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", null);

        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
    }


    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        Log.i(TAG, "onActivityResult: code = " + request + ", response = " + response);

        // Coming back from resolving a sign-in request
        if (request == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (response == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, request, response,
                        R.string.sign_in_failed);
            }
        }

        // Coming back from a RealTime Multiplayer waiting room
        if (request == RC_WAITING_ROOM) {
            dismissSpinner();

            Room room = data.getParcelableExtra(Multiplayer.EXTRA_ROOM);
            if (response == RESULT_OK) {
                Log.d(TAG, "Waiting Room: Success");
                mRoom = room;
                startMatch();
            } else if (response == RESULT_CANCELED) {
                Log.d(TAG, "Waiting Room: Canceled");
                leaveRoom();
            } else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                Log.d(TAG, "Waiting Room: Left Room");
                leaveRoom();
            } else if (response == GamesActivityResultCodes.RESULT_INVALID_ROOM) {
                Log.d(TAG, "Waiting Room: Invalid Room");
                leaveRoom();
            }
        }

        // We are coming back from the player selection UI, in preparation to start a match.
        if (request == RC_SELECT_PLAYERS) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                Log.d(TAG, "onActivityResult: user canceled player selection.");
                return;
            }

            // Create a basic room configuration
            RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this)
                    .setMessageReceivedListener(this)
                    .setRoomStatusUpdateListener(this);

            // Set the auto match criteria
            int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
            if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
                Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            }

            // Set the invitees
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
            if (invitees != null && invitees.size() > 0) {
                roomConfigBuilder.addPlayersToInvite(invitees);
            }

            // Build the room and start the match
            showSpinner();
            Games.RealTimeMultiplayer.create(mGoogleApiClient, roomConfigBuilder.build());
        }
    }

    /**
     * Begin a new match, send a message to all other participants with the initial turn data
     */
    private void startMatch() {
        if (isMyTurn()) {
            // Pick words randomly
            mTurnWords = getRandomWordSubset(10);
            mWordIndex = (new Random()).nextInt(mTurnWords.size());

            // Send turn message to others
            TurnMessage turnMessage = new TurnMessage(0, mTurnWords, mWordIndex);
            sendReliableMessageToOthers(turnMessage);
        }

        beginMyTurn();

        // Unlock the Achievement for starting a game
        if (isSignedIn() && checkConfiguration(false)) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_started_a_game));
        }
    }

    /**
     * Determines if the current player is drawing or guessing. Used to determine what UI to show
     * and what messages to send.
     *
     * @return true if the current player is the artist, false otherwise.
     */
    private boolean isMyTurn() {
        int numParticipants = mParticipants.size();
        if (numParticipants == 0) {
            Log.w(TAG, "isMyTurn: no participants - default to true.");
            return true;
        }
        int participantTurnIndex = mMatchTurnNumber % numParticipants;

        Log.d(TAG, String.format("isMyTurn: %d participants, turn #%d, my turn is #%d",
                numParticipants, mMatchTurnNumber, mMyTurnIndex));
        return (mMyTurnIndex == participantTurnIndex);
    }

    @Override
    public void onInvitationReceived(final Invitation invitation) {
        Log.d(TAG, "onInvitationReceived:" + invitation);
        final String inviterName = invitation.getInviter().getDisplayName();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Invitation")
                .setMessage("Would you like to play a new game with " + inviterName + "?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Games.RealTimeMultiplayer.declineInvitation(mGoogleApiClient,
                                invitation.getInvitationId());
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        acceptInvitation(invitation);
                    }
                });

        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        Log.d(TAG, "onInvitationRemoved:" + invitationId);

        // The invitation is no longer valid, so dismiss the dialog asking if they'd like to
        // accept and show a Toast.
        if (mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }

        Toast.makeText(this, "The invitation was removed.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Accept an invitation to join an RTMP game
     */
    private void acceptInvitation(Invitation invitation) {
        Log.d(TAG, "Got invitation: " + invitation);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this)
                .setInvitationIdToAccept(invitation.getInvitationId());

        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }

    @Override
    public void onDrawEvent(int gridX, int gridY, short colorIndex) {
        // Send realtime message to others
        EPointMessage msg = new EPointMessage(new EPoint(gridX, gridY), colorIndex);
        sendReliableMessageToOthers(msg);
    }

    /**
     * Clear the DrawView and send a message to all opponents to do the same
     */
    private void onClearClicked() {
        mDrawView.clear();

        ClearMessage msg = new ClearMessage();
        sendReliableMessageToOthers(msg);
    }

    /**
     * Create a Dialog with the result of the local player's guess.
     *
     * @param guessIndex   the index in the word list that the player clicked.
     * @param correctIndex the index in the word list of the correct answer.
     */
    private void createGuessDialog(int guessIndex, int correctIndex) {
        Log.d(TAG, "Guessed..." + mTurnWords.get(guessIndex));
        mHasGuessed = true;
        String guessedWord = mTurnWords.get(guessIndex);
        String correctWord = mTurnWords.get(correctIndex);

        if (guessIndex == correctIndex) {
            // The player guessed correctly
            showDialog("You got it!", guessedWord + " is correct!");

            // Reveal and unlock the correct guess achievement
            if (isSignedIn() && checkConfiguration(false)) {
                Games.Achievements.reveal(mGoogleApiClient,
                        getResources().getString(R.string.achievement_guessed_correctly));
                Games.Achievements.unlock(mGoogleApiClient,
                        getResources().getString(R.string.achievement_guessed_correctly));
            }
        } else {
            // The player guessed incorrectly
            showDialog("No!", guessedWord + " is wrong. The real answer was " + correctWord);

            // Unlock the wrong guess achievement
            if (isSignedIn() && checkConfiguration(false)) {
                Games.Achievements.unlock(mGoogleApiClient,
                        getResources().getString(R.string.achievement_got_one_wrong));
            }
        }
    }

    /**
     * Create a dialog with the result of another player's guess.
     *
     * @param guesserId the participant ID of the player that guessed.
     */
    private void createOpponentGuessDialog(String guesserId) {
        mGuessersThisTurn.add(guesserId);

        boolean allHaveGuessed = mGuessersThisTurn.size() >= mParticipants.size() - 1;
        if (isMyTurn() && allHaveGuessed) {
            // All guesses entered
            String message = "All other players have guessed.\n" +
                    "Press 'Done' to end your turn.";
            showDialog("All Guesses Entered", message);
        }
    }

    /**
     * Show or hide the word choice list for guessing.
     *
     * @param enable true if the list should be shown, false otherwise.
     */
    private void enableGuessing(boolean enable) {
        if (enable) {
            findViewById(R.id.listView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.listView).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Begin a turn where the player is guessing what the artist is drawing. Begins a countdown
     * from 30 to 1 which determines how many points the player will get if and when they make a
     * correct guess,
     */
    private void beginGuessingTurn() {
        mDrawView.clear();
        mHasGuessed = false;
        setGuessingUI();

        // Set up the progress dialog
        mGuessProgress.setProgress(30);
        mGuessProgressText.setText(String.valueOf(30));

        // Decrement from 30 to 1, once every second
        Runnable decrementProgress = new Runnable() {
            @Override
            public void run() {
                int oldProgress = mGuessProgress.getProgress();
                if (!mHasGuessed && oldProgress > 1) {
                    mGuessProgress.setProgress(oldProgress - 1);
                    mGuessProgressText.setText(
                            mNearbyClient.getState() + ": " +
                                    String.valueOf(oldProgress - 1));
                    mGuessProgressHandler.postDelayed(this, 1000L);

                }
            }
        };
        mGuessProgressHandler.removeCallbacksAndMessages(null);
        mGuessProgressHandler.postDelayed(decrementProgress, 1000L);

        updateViewVisibility();
    }


    /**
     * Begin a turn where the player is drawing. Clear the DrawView and show the drawing UI.
     */
    private void beginArtistTurn() {
        mDrawView.clear();
        setArtistUI();

        updateViewVisibility();
    }

    /**
     * Begin the player's turn, calling the correct beginTurn function based on role
     **/
    private void beginMyTurn() {
        Log.d(TAG, "beginMyTurn: " + isMyTurn());
        if (isMyTurn()) {
            beginArtistTurn();
        } else {
            beginGuessingTurn();
        }
    }

    /**
     * When the artist clicks done, all guessing is closed and the turn should be passed to the
     * next person to draw. The artist can do this at any point and the artist's turn is never over
     * until Done is clicked.
     */
    private void onDoneClicked() {
        // Increment turn number
        mMatchTurnNumber = mMatchTurnNumber + 1;

        // Choose random word subset and correct word
        mTurnWords = getRandomWordSubset(10);
        mWordIndex = (new Random()).nextInt(mTurnWords.size());

        // Send new turn data to others
        TurnMessage turnMessage = new TurnMessage(mMatchTurnNumber, mTurnWords, mWordIndex);
        sendReliableMessageToOthers(turnMessage);


        // Increment turn achievements
        if (isSignedIn() && checkConfiguration(false)) {
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_5_turns), 1);
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_10_turns), 1);
        }

        beginMyTurn();
        updateViewVisibility();
    }

    /**
     * Pick a random set of words from the master word list.
     *
     * @param numWords the number of words to choose.
     * @return a list of randomly chosen words.
     */
    private List<String> getRandomWordSubset(int numWords) {
        List<String> result = new ArrayList<>();

        Collections.addAll(result, mAllWords);
        Collections.shuffle(result);
        result = result.subList(0, numWords);

        return result;
    }

    /**
     * Record my guess, incrementing score if necessary and informing all other players.
     *
     * @param position the index in the word list of my guess.
     */
    private void makeGuess(int position) {
        // Decide how many points I could earn based on the current state of the countdown bar
        int potentialPoints = mGuessProgress.getProgress();

        // Send my guess to other players
        GuessMessage guessMessage = new GuessMessage(position, potentialPoints, mMyPersistentId);
        sendReliableMessageToOthers(guessMessage);

        // Disable guessing and show result
        enableGuessing(false);
        createGuessDialog(position, mWordIndex);

        // Increment my score if I got it right
        if (position == mWordIndex) {
            incrementPlayerScore(mMyPersistentId, potentialPoints);
        }
    }

    /**
     * Show the UI for a non-artist player
     */
    private void setGuessingUI() {
        findViewById(R.id.guesserUI).setVisibility(View.VISIBLE);
        findViewById(R.id.colorChooser).setVisibility(View.GONE);
        findViewById(R.id.clearDoneLayout).setVisibility(View.GONE);
        findViewById(R.id.guessWord).setVisibility(View.GONE);

        // Disable touch on drawview
        mDrawView.setTouchEnabled(false);
        enableGuessing(true);

        // Show player 'cards'
        setUpPlayerViews();

        // Set words, clear draw view
        resetWords(mTurnWords);
        mDrawView.clear();
    }

    /**
     * Show the UI for the player who is currently acting as the artist.
     */
    private void setArtistUI() {
        findViewById(R.id.artistUI).setVisibility(View.VISIBLE);
        findViewById(R.id.guesserUI).setVisibility(View.GONE);
        findViewById(R.id.colorChooser).setVisibility(View.VISIBLE);
        findViewById(R.id.clearDoneLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.guessWord).setVisibility(View.VISIBLE);

        mDrawView.setTouchEnabled(true);

        ((TextView) findViewById(R.id.guessWord)).setText(mTurnWords.get(mWordIndex));
        mDrawView.clear();
    }

    /**
     * Show a small view for each connected player with their picture, name, and score.
     */
    private void setUpPlayerViews() {
        // Remove all existing views (note that removing and re-adding all views is not very
        // efficient and you should not do it in your own application).
        LinearLayout playerViews = (LinearLayout) findViewById(R.id.playerViews);
        playerViews.removeAllViewsInLayout();

        // Sort the list to determine which player is the artist so we can highlight that view
        List<String> ids = new ArrayList<>();
        ids.addAll(mParticipants.keySet());
        Collections.sort(ids);
        int artistIndex = mMatchTurnNumber % ids.size();
        String artistId = ids.get(artistIndex);

        // Create a PlayerView for each participant
        for (DrawingParticipant participant : mParticipants.values()) {
            PlayerView playerView = new PlayerView(this);
            playerView.populateWithParticipant(participant);
            playerView.setIsArtist(participant.getPersistentId().equals(artistId));
            playerViews.addView(playerView);
        }
    }

    /**
     * Set the list of words to display for guessing.
     */
    private void resetWords(List<String> words) {
        ListView list = (ListView) findViewById(R.id.listView);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);

        for (String st : words) {
            adapter.add(st);
        }

        list.setAdapter(adapter);
    }

    /**
     * Disconnect from an RTMP room or a Nearby Connection. Clear all game data.
     */
    private void leaveGame() {
        if (mRoom != null) {
            leaveRoom();
        } else if (mIsJoinedParty || mIsHostingParty) {
            mNearbyClient.onStop();
            mIsJoinedParty = mIsHostingParty = false;
            updateViewVisibility();
        }

        mParticipants.clear();
        mOldParticipants.clear();
        mMyPersistentId = null;
        mHasGuessed = false;
    }

    /**
     * Leave an RTMP room.
     */
    private void leaveRoom() {
        Log.d(TAG, "leaveRoom:" + mRoom);
        if (mRoom != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoom.getRoomId());
            mRoom = null;
        }

        updateViewVisibility();
    }

    /**
     * Update the turn order so that each participant has a unique slot.
     */
    private void updateTurnIndices() {
        // Turn order is determined by sorting participant IDs, which are consistent across
        // devices (but not across sessions)
        ArrayList<String> ids = new ArrayList<>();
        ids.addAll(mParticipants.keySet());
        Collections.sort(ids);

        // Get your turn order
        mMyTurnIndex = ids.indexOf(mMyPersistentId);
        Log.d(TAG, "My turn index: " + mMyTurnIndex);
    }

    /**
     * Show the UI for an RTMP waiting room.
     */
    private void showWaitingRoom(Room room) {
        // Require all players to join before starting
        final int MIN_PLAYERS = Integer.MAX_VALUE;

        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
        startActivityForResult(i, RC_WAITING_ROOM);
    }


    private void sendReliableMessageToOthers(Message msg) {
        sendReliableMessageToOthers(mMapper.toJson(msg, Message.class));
    }

    /**
     * Send a reliable message to all other participants. If this is an RTMP game, send a reliable
     * message to each player directly. If this is a Nearby Connections game, send a message to the
     * host who will broadcast it to all connected players.
     *
     * @param message string to send in the message.
     */
    private void sendReliableMessageToOthers(String message) {

        byte[] data = null;

        DrawingParticipant me = mParticipants.get(mMyPersistentId);
        for (DrawingParticipant participant : mParticipants.values()) {
            if (!participant.equals(me) && !participant.getIsLocal()) {
                if (data == null) {
                    try {
                        data = message.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Could not encode " + message + " as UTF-8?");
                        return;
                    }
                }
                // The participant is RTMP and not sending message to myself
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null,
                        data, mRoom.getRoomId(), participant.getMessagingId());
            }
        }

        // Party mode, route messages through host
        if (mIsJoinedParty || mIsHostingParty) {
            Log.d(TAG, "Broadcasting message.");
            mNearbyClient.broadcastMessage(message);
        }
    }

    /**
     * Add points to a player's score, local copy only.
     *
     * @param id        the participant ID to update.
     * @param numPoints number of points to add to the score.
     */
    private void incrementPlayerScore(String id, int numPoints) {
        Integer currentScore = mParticipants.get(id).getScore();
        mParticipants.get(id).setScore(currentScore + numPoints);
    }

    /**
     * Remove a participant. If this is RTMP and you are now the only player in the room, leave the
     * room as well and end the game. If this is a Nearby Connections game and the host has
     * disconnected, leave the game and display an error.
     *
     * @param messagingId  the messaging ID of the player that disconnected.
     * @param persistentId the persistent ID of the player that disconnected.
     */
    private void onParticipantDisconnected(String messagingId, String persistentId) {
        Log.d(TAG, "onParticipantDisconnected:" + messagingId);
        DrawingParticipant dp = mParticipants.remove(persistentId);
        if (dp != null) {
            // Display disconnection toast
            Toast.makeText(this, dp.getDisplayName() + " disconnected.", Toast.LENGTH_SHORT).show();

            // Add the participant to the "old" list in case they reconnect
            mOldParticipants.put(dp.getPersistentId(), dp);

            if (mRoom != null && mParticipants.size() <= 1) {
                // Last player left in an RTMP game, leave
                leaveRoom();
            } else if (mIsJoinedParty && messagingId.equals(mNearbyClient.getHostEndpointId())) {
                // Host disconnected, leave the game
                Log.d(TAG, "onParticipantDisconnected: host");
                Toast.makeText(this, "Error: disconnected from host.", Toast.LENGTH_SHORT).show();
                leaveGame();
            } else {
                updateTurnIndices();
            }

            updateViewVisibility();
        }
    }

    /**
     * RTMP Participant joined, register the DrawingParticipant if the Participant is connected.
     *
     * @param p the Participant from the Real-Time Multiplayer match.
     */
    private void onParticipantConnected(Participant p) {
        if (p.isConnectedToRoom()) {
            onParticipantConnected(new DrawingParticipant(p));
        }
    }

    /**
     * Add a DrawingParticipant to the ongoing game and update turn order. If the
     * DrawingParticipant is a duplicate, this method does nothing.
     *
     * @param dp the DrawingParticipant to add.
     */
    private void onParticipantConnected(DrawingParticipant dp) {
        Log.d(TAG, "onParticipantConnected: " + dp.getPersistentId());
        if (!mParticipants.containsKey(dp.getPersistentId())) {
            mParticipants.put(dp.getPersistentId(), dp);
        }

        updateTurnIndices();
        updateViewVisibility();
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        Log.d(TAG, "onRealTimeMessageReceived");
        byte[] data = realTimeMessage.getMessageData();

        onMessageReceived(data);
    }

    /**
     * Message handler for RTMP and Nearby Connections messages. First decodes as a general Message
     * and then uses the type attribute to determine which class the message originally was. Then
     * deserializes to a specific message class and takes the appropriate action,
     *
     * @param bytes byte array of data to deserialize.
     */
    private void onMessageReceived(byte[] bytes) {
        String data = new String(bytes);
        Log.d(TAG, "Message: " + data);

        // Parse JSON message.
        Message message = mMapper.fromJson(data, Message.class);

        // Branch on message type
        if (message instanceof EPointMessage) {
            // EPointMessage - draw a point on the DrawView
            EPointMessage msg = (EPointMessage) message;
            mDrawView.setMacroPixel(msg.getPoint().x, msg.getPoint().y, (short) msg.getColor());
        } else if (message instanceof ClearMessage) {
            // ClearMessage - clear the DrawView
            mDrawView.clear();
        } else if (message instanceof TurnMessage) {
            // TurnMessage - set all turn-specific data
            TurnMessage msg = (TurnMessage) message;
            mMatchTurnNumber = msg.getTurnNumber();
            mTurnWords = msg.getWords();
            mWordIndex = msg.getCorrectWord();
            mGuessersThisTurn.clear();

            beginMyTurn();
        } else if (message instanceof GuessMessage) {
            // GuessMessage - record an opponent's guess
            GuessMessage msg = (GuessMessage) message;
            createOpponentGuessDialog(msg.getGuesserId());

            if (msg.getGuessIndex() == mWordIndex) {
                // The guess was correct, award a point
                incrementPlayerScore(msg.getGuesserId(), msg.getPotentialPoints());
            }
        } else if (message instanceof ParticipantMessage) {
            // ParticipantMessage - add or remove a participant
            ParticipantMessage msg = (ParticipantMessage) message;
            DrawingParticipant participant = msg.getDrawingParticipant();

            if (msg.getIsJoining()) {
                if (mOldParticipants.containsKey(participant.getPersistentId())) {
                    Log.d(TAG, "Participant rejoining: " + participant.getPersistentId());
                    // This participant was in the game before, add and recover
                    DrawingParticipant oldParticipant = mOldParticipants.remove(
                            participant.getPersistentId());
                    mParticipants.put(participant.getPersistentId(), oldParticipant);

                    // Tell everyone what their old score was
                    ParticipantMessage updateMsg = new ParticipantMessage(oldParticipant);
                    mNearbyClient.sendMessageToAll(mMapper.toJson
                            (updateMsg, Message.class), null);
                } else if (mParticipants.containsKey(participant.getPersistentId())) {
                    // Current participant, update the score
                    mParticipants.get(participant.getPersistentId()).setScore(
                            participant.getScore());
                    onParticipantConnected(participant);
                } else {
                    // Add new participant
                    onParticipantConnected(participant);
                }
            } else {
                onParticipantDisconnected(participant.getMessagingId(),
                        participant.getPersistentId());
            }

            updateTurnIndices();
            updateViewVisibility();
        }
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated: " + statusCode + ":" + room);
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.w(TAG, "Error in onRoomCreated: " + statusCode);
            Toast.makeText(this, "Error creating room.",
                    Toast.LENGTH_SHORT).show();
            dismissSpinner();
            return;
        }

        showWaitingRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom: " + statusCode + ":" + room);
    }

    @Override
    public void onLeftRoom(int statusCode, String s) {
        Log.d(TAG, "onLeftRoom: " + statusCode + ":" + s);
        mRoom = null;
        updateViewVisibility();
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected: " + statusCode + ":" + room);
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.w(TAG, "Error in onRoomConnected: " + statusCode);
            return;
        }

        mRoom = room;
        updateViewVisibility();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                mSignInClicked = true;
                mGoogleApiClient.connect();
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                break;
            case R.id.sign_out_button:
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                updateViewVisibility();
                break;
            case R.id.startMatchButton:
                onStartMatchClicked();
                break;
            case R.id.partyModeHostButton:
                onHostPartyClicked();
                break;
            case R.id.partyModeJoinButton:
                onJoinPartyClicked();
                break;
            case R.id.clearButton:
                onClearClicked();
                break;
            case R.id.doneButton:
                onDoneClicked();
                break;
        }
    }

    @Override
    public void onRoomConnecting(Room room) {
        Log.d(TAG, "onRoomConnecting: " + room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        Log.d(TAG, "onRoomAutoMatching: " + room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {
        Log.d(TAG, "onPeerInvitedToRoom: " + room + ":" + strings);
    }

    @Override
    public void onPeerDeclined(Room room, List<String> strings) {
        Log.d(TAG, "onPeerDeclined: " + room + ":" + strings);
    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {
        Log.d(TAG, "onPeerJoined: " + room + ":" + strings);
        mRoom = room;
        for (String pId : strings) {
            onParticipantConnected(mRoom.getParticipant(pId));
        }
    }

    @Override
    public void onPeerLeft(Room room, List<String> strings) {
        Log.d(TAG, "onPeerLeft: " + room + ":" + strings);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedRoRoom: " + room);
        mRoom = room;

        // Add self to participants
        mMyPersistentId = mRoom.getParticipantId(
                Games.Players.getCurrentPlayerId(mGoogleApiClient));
        Participant me = mRoom.getParticipant(mMyPersistentId);
        onParticipantConnected(me);

        updateTurnIndices();
        updateViewVisibility();
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        Log.d(TAG, "onDisconnectedFromRoom: " + room);
        leaveRoom();
    }

    @Override
    public void onPeersConnected(Room room, List<String> strings) {
        Log.d(TAG, "onPeersConnected:" + room + ":" + strings);
        mRoom = room;
        for (String pId : strings) {
            onParticipantConnected(mRoom.getParticipant(pId));
        }
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> strings) {
        Log.d(TAG, "onPeersDisconnected: " + room + ":" + strings);
        for (String pId : strings) {
            onParticipantDisconnected(pId, pId);
        }
    }

    @Override
    public void onP2PConnected(String s) {
        Log.d(TAG, "onP2PConnected: " + s);
    }

    @Override
    public void onP2PDisconnected(String s) {
        Log.d(TAG, "onP2PDisconnected: " + s);
    }

    private boolean checkConfiguration(boolean showDialog) {
        int[] ids = new int[]{
                R.string.app_id, R.string.achievement_5_turns, R.string.achievement_10_turns,
                R.string.achievement_got_one_wrong, R.string.achievement_guessed_correctly,
                R.string.achievement_started_a_game
        };

        // Check all of the resource IDs
        boolean correctlyConfigured = true;
        for (int id : ids) {
            if ("REPLACE_ME".equals(getString(id))) {
                correctlyConfigured = false;
            }
        }

        if (!correctlyConfigured) {
            Log.e(TAG, "Error: res/values/ids.xml contains invalid values.");
            if (showDialog) {
                // Show a dialog warning the developer
                AlertDialog ad = new AlertDialog.Builder(this)
                        .setTitle("Configuration Error")
                        .setMessage("One or more of your IDs is not configured correctly, " +
                                "please correctly configure res/values/ids.xml")
                        .setPositiveButton("OK", null)
                        .create();

                ad.show();
            }
        }

        return correctlyConfigured;
    }
}
