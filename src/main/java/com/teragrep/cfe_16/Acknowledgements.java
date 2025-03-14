/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2021-2025 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.cfe_16;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.bo.Ack;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.exceptionhandling.InternalServerErrorException;
import com.teragrep.cfe_16.exceptionhandling.ServerIsBusyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * Handles the acknowledgement statuses of the sent events (acks).
 * A background thread is used to clean up NRU ACK objects.
 *
 * This class is thread safe.
 *
 */
@Component
public final class Acknowledgements implements Runnable, LifeCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Acknowledgements.class);

    /**
     * A class that encapsulates state of individual channels regarding ACKs.
     */
    private class State {

        private int currentAckValue;
        private Ack ackToCompare;
        private Map<Integer, Ack> ackMap;

        public State() {
            this.currentAckValue = 0;
            this.ackToCompare = new Ack();
            this.ackMap = new HashMap<Integer, Ack>();
        }

        public int getCurrentAckValue() {
            return this.currentAckValue;
        }

        public void setCurrentAckValue(int currentAckValue) {
            this.currentAckValue = currentAckValue;
        }

        public Ack getAckToCompare() {
            return this.ackToCompare;
        }

        public void setAckToCompare(Ack ackToCompare) {
            this.ackToCompare = ackToCompare;
        }

        public Map<Integer, Ack> getAckMap() {
            return this.ackMap;
        }

        @Override
        public String toString() {
            return "State [currentAckValue=" + this.currentAckValue + ", ackToCompare=" + this.ackToCompare
                    + ", ackMap=" + this.ackMap + "]";
        }
    }

    /**
     * Does the JSON <-> Java conversions.
     */
    private final ObjectMapper objectMapper;

    /**
     * A hash for mapping channels to ACK status objects.
     */
    private final Map<String, State> ackStates;

    /**
     * The background thread for cleaning up ACKs.
     */
    private Thread cleanerThread;

    @Autowired
    private Configuration configuration;

    /**
     * An empty constructor for Spring @Autowired annotation.
     */
    public Acknowledgements() {
        this.objectMapper = new ObjectMapper();
        this.ackStates = Collections.synchronizedMap(new HashMap<String, State>());
    }

    @Override
    @PostConstruct
    public void start() {
        this.cleanerThread = new Thread(this);
        this.cleanerThread.start();
    }

    /**
     * Let's interrupt the cleaner thread from its eternal run().
     */
    @Override
    public void stop() {
        this.cleanerThread.interrupt();
    }

    /**
     * A private Accessor for the State object indexed by the given auth token and channel. If no State object is found,
     * a new object is created and added to the map.
     *
     * @param authToken
     * @param channel
     * @return
     */
    private State getOrCreateState(String authToken, String channel) {
        LOGGER.debug("Getting or creating state for channel <{}>", channel);
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            state = new State();
            this.ackStates.put(key, state);
        }
        LOGGER.debug("Created state <{}> for channel <{}>", state, channel);
        return state;
    }

    /**
     * This method has to be called first before calling any other Ack related methods.
     *
     * @param authToken
     * @param channel
     */
    public void initializeContext(String authToken, String channel) {
        LOGGER.debug("Initializing context for channel <{}>", channel);
        String key = authToken + channel;
        if (!this.ackStates.containsKey(key)) {
            LOGGER.debug("Adding new state to channel <{}>", channel);
            State state = new State();
            this.ackStates.put(key, state);
        }
    }

    /*
     * Assignes an Ack value for the event. Checks it there are acks still available
     * for the channel If there are no Acks available, throws ServerIsBusyException.
     * Uses the ackToCompare variable to check if Ack with a current value is in the
     * Ack list and increases the Ack value until a suitable Ack value is found and
     * when it is found, a new Ack with the current Ack value as an id is created.
     */
    public boolean incrementAckValue(String authToken, String channel) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            return false;
        }

        if (!acksAvailable(state)) {
            throw new ServerIsBusyException();
        }

        int currentAckValue;
        synchronized (state) {
            currentAckValue = state.getCurrentAckValue();
            Ack ackToCompare = state.getAckToCompare();
            ackToCompare.setId(currentAckValue);
            state.setAckToCompare(ackToCompare);
            Map<Integer, Ack> ackMap = state.getAckMap();
            while (ackMap.containsKey(ackToCompare.getId())) {
                currentAckValue++;
                ackToCompare = state.getAckToCompare();
                ackToCompare.setId(currentAckValue);
                if (currentAckValue > this.configuration.getMaxAckValue()) {
                    currentAckValue = 0;
                }
                state.setAckToCompare(ackToCompare);
            }

            currentAckValue++;
            if (currentAckValue > this.configuration.getMaxAckValue()) {
                currentAckValue = 0;
            }
            state.setCurrentAckValue(currentAckValue);
        }
        return true;
    }

    /*
     * Uses the help Ack object to find the Ack object from the Ack set and sets the
     * acknowledgement status of the Ack as true.
     */
    public boolean acknowledge(String authToken, String channel, int ackId) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        LOGGER.debug("Acknowledging ackId <{}> on channel <{}>", ackId, channel);
        if (state == null) {
            throw new IllegalStateException("An Ack cannot be acknowledge before it is added to the Ack list.");
        }
        synchronized (state) {
            Map<Integer, Ack> ackMap = state.getAckMap();
            Ack ack = ackMap.get(ackId);
            if (ack == null) {
                throw new InternalServerErrorException("Couldn't set the acknowledge status for Ack ID " + ackId);
            }
            ack.acknowledge();
            return true;
        }
    }

    /**
     * Adds a new Ack object for given channel. If this is the first time a channel is assigned a new Ack, a new State
     * object is created.
     *
     * @param channel
     * @param ack
     */
    public boolean addAck(String authToken, String channel, Ack ack) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            throw new InternalServerErrorException("No State for key " + key);
        }
        synchronized (state) {
            state.ackMap.put(ack.getId(), ack);
            return true;
        }
    }

    /**
     * Checks if the Ack with a given id is acknowledged.
     */
    public boolean isAckAcknowledged(String authToken, String channel, int ackId) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            throw new InternalServerErrorException("No State for key " + key);
        }
        synchronized (state) {
            Map<Integer, Ack> ackMap = state.getAckMap();
            Ack ack = ackMap.get(ackId);
            if (ack == null) {
                return false;
            }
            ack.acknowledge();
            return true;
        }
    }

    /**
     * Returns the Ack statuses of requested Ack id:s as a JSON node. JSON node with the id:s is given as a parameter.
     * Example: {"acks": [1,3,4]}
     */
    public JsonNode getRequestedAckStatuses(String authToken, String channel, JsonNode requestedAcksInJson) {
        JsonNode jsonNode = this.objectMapper.createObjectNode();
        if (requestedAcksInJson == null) {
            return jsonNode;
        }

        /*
         * Checks that the JSON parameter is given, and that there is an Ack node which
         * is an array. Saves the requested ack ids in an int array.
         */
        int[] requestedAckIds = null;
        if (requestedAcksInJson.get("acks") != null && requestedAcksInJson.get("acks").isArray()) {
            requestedAckIds = this.objectMapper.convertValue(requestedAcksInJson.get("acks"), int[].class);
        }

        /*
         * Goes through the requested Ack id:s, checks if they have been assigned to an
         * Ack object in the Ack list. If an Ack is found, the Ack id and the
         * acknowledgement status of that Ack is saved in "ackStatuses" JSON node. If
         * Ack is not found in the list, the Ack id and acknowledgement status "false"
         * is added to the JSON node.
         */

        Map<Integer, Boolean> ackStatuses = new HashMap<Integer, Boolean>();
        if (requestedAckIds != null) {
            String key = authToken + channel;
            State state = this.ackStates.get(key);
            if (state == null) {
                return null;
            }
            synchronized (state) {
                Map<Integer, Ack> ackMap = state.getAckMap();
                for (int i = 0; i < requestedAckIds.length; i++) {
                    int ackId = requestedAckIds[i];
                    Ack ack = ackMap.get(ackId);
                    if (ack == null) {
                        ackStatuses.put(ackId, false);
                    }
                    else {
                        ackStatuses.put(ackId, ack.isAcknowledged());
                        ackMap.remove(ackId);
                    }
                }
            }
        }
        jsonNode = this.objectMapper.convertValue(ackStatuses, JsonNode.class);
        return jsonNode;
    }

    /*
     * Checks if there is room in the Ack list for a new entry
     *
     * Not thread safe, needs external synchronization.
     */
    private boolean acksAvailable(State state) {
        Map<Integer, Ack> ackMap = state.getAckMap();
        int ackMapSize = ackMap.size();
        int maxAckValue = this.configuration.getMaxAckValue();
        if (ackMapSize > maxAckValue) {
            return false;
        }
        else {
            return true;
        }
    }

    /*
     * Deletes a given Ack from the Ack list.
     *
     * This is an O(n) operation..
     */
    public boolean deleteAckFromList(String authToken, String channel, Ack ack) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            throw new InternalServerErrorException("No State for key " + key);
        }
        synchronized (state) {
            state.getAckMap().remove(ack.getId());
            return true;
        }
    }

    public void run() {

        while (true) {
            try {
                LOGGER.debug("Sleeping for <{}> while waiting for polls", this.configuration.getPollTime());
                Thread.sleep(this.configuration.getPollTime());
            }
            catch (InterruptedException e) {
                break;
            }

            for (String key : this.ackStates.keySet()) {
                State state = this.ackStates.get(key);

                synchronized (state) {
                    Map<Integer, Ack> ackMap = state.getAckMap();
                    Iterator<Ack> iterator = ackMap.values().iterator();
                    while (iterator.hasNext()) {
                        Ack ack = iterator.next();
                        long thresholdInLong = ack.getLastUsedTimestamp() + this.configuration.getMaxAckAge();

                        /**
                         * If the Ack object is too old we'll remove it from the Ack set.
                         */
                        long now = System.currentTimeMillis();
                        if (now >= thresholdInLong) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the size of the Ack set of given channel.
     *
     * @param channel
     * @return
     */
    public int getAckListSize(String authToken, String channel) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            throw new InternalServerErrorException("No State for key " + key);
        }
        synchronized (state) {
            return state.getAckMap().size();
        }
    }

    /**
     * Returns the unmodifiable set of Acks for a channel.
     *
     * @param channel
     * @return
     */
    public Map<Integer, Ack> getAckList(String authToken, String channel) {
        String key = authToken + channel;
        State state = this.ackStates.get(key);
        if (state == null) {
            throw new InternalServerErrorException("No State for key " + key);
        }
        synchronized (state) {
            return state.getAckMap();
        }
    }

    /**
     * Returns the current Ack value for given token and channel. A new State is created, so this method must be called
     * first before other Ack manipulating methods are called.
     *
     * @param authToken
     * @param channel
     * @return
     */
    public int getCurrentAckValue(String authToken, String channel) {
        State state = this.getOrCreateState(authToken, channel);
        synchronized (state) {
            return state.getCurrentAckValue();
        }
    }
}
