/*
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.jersey.tests;

import org.atmosphere.annotation.Broadcast;
import org.atmosphere.annotation.Resume;
import org.atmosphere.annotation.Schedule;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.jersey.Broadcastable;
import org.atmosphere.jersey.JerseyBroadcaster;
import org.atmosphere.util.StringFilterAggregator;
import org.atmosphere.util.XSSHtmlFilter;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Simple PubSubTest resource that demonstrate many functionality supported by
 * Atmosphere.
 *
 * @author Jeanfrancois Arcand
 */
@Path("/{topic}")
@Produces("text/plain;charset=ISO-8859-1")
public class PubSubTest {

    /**
     * Inject a {@link org.atmosphere.cpr.Broadcaster} based on @Path
     */
    private
    @PathParam("topic")
    Broadcaster broadcaster;
    private final static int count = 0;

    @GET
    @Path("scope")
    @Suspend(period = 5000, outputComments = false, scope = Suspend.SCOPE.REQUEST, resumeOnBroadcast = true)
    public Broadcastable suspendScopeRequest(@PathParam("topic") Broadcaster b) throws ExecutionException, InterruptedException {
        b.broadcast("foo").get();
        return new Broadcastable("bar", b);
    }

    @GET
    @Suspend(period = 5000, outputComments = false)
    public Broadcastable subscribe() {
        return new Broadcastable("resume", broadcaster);
    }

    @GET
    @Path("withComments")
    @Suspend(period = 5000, outputComments = true)
    public Broadcastable subscribeWithComments() {
        return new Broadcastable(broadcaster);
    }

    @GET
    @Path("forever")
    @Suspend(outputComments = true)
    public Broadcastable suspendForever() {
        return new Broadcastable(broadcaster);
    }

    @GET
    @Path("foreverWithoutComments")
    @Suspend(outputComments = false)
    public Broadcastable suspendForeverWithoutComments() {
        return new Broadcastable(broadcaster);
    }

    /**
     * Suspend the response, and register a {@link org.atmosphere.cpr.AtmosphereResourceEventListener}
     * that get notified when events occurs like client disconnection, broadcast
     * or when the response get resumed.
     *
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @GET
    @Path("subscribeAndUsingExternalThread")
    @Suspend(resumeOnBroadcast = true)
    public String subscribeAndResumeUsingExternalThread(final @PathParam("topic") String topic) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                BroadcasterFactory.getDefault().lookup(JerseyBroadcaster.class, topic).broadcast("Echo: " + topic);
            }
        });
        return "foo";
    }

    @GET
    @Path("suspendAndResume")
    @Suspend(outputComments = false)
    public String suspend() {
        return "suspend";
    }

    /**
     * Suspend the response, and tell the framework to resume the response
     * when the first @Broadcast operation occurs.
     *
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @GET
    @Suspend(resumeOnBroadcast = true, outputComments = false)
    @Path("subscribeAndResume")
    public Broadcastable subscribeAndResume() {
        return new Broadcastable(broadcaster);
    }

    @GET
    @Resume
    @Path("suspendAndResume/{uuid}")
    public String resume() throws ExecutionException, InterruptedException {
        broadcaster.broadcast("resume").get();
        return "resumed";
    }

    /**
     * Broadcast message to this server and also to other server using JGroups
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @POST
    @Broadcast
    public Broadcastable publish(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Broadcast message to this server and also to other server using JGroups
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @POST
    @Path("publishAndResume")
    @Broadcast(resumeOnBroadcast = true)
    public Broadcastable publishAndResume(@FormParam("message") String message) {
        return broadcast(message);
    }

    @POST
    @Path("filter")
    @Broadcast(resumeOnBroadcast = true, filters = {XSSHtmlFilter.class})
    public Broadcastable filter(@FormParam("message") String message) {
        return broadcast(message);
    }

    @POST
    @Path("aggregate")
    @Broadcast(resumeOnBroadcast = true, filters = {StringFilterAggregator.class})
    public Broadcastable aggregate(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Execute periodic {@link org.atmosphere.cpr.Broadcaster#broadcast(Object)} operation and
     * resume the suspended connection after the first broadcast operation.
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @Schedule(period = 5, resumeOnBroadcast = true, waitFor = 5)
    @POST
    @Path("scheduleAndResume")
    public Broadcastable scheduleAndResume(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Wait 5 seconds and then execute periodic {@link org.atmosphere.cpr.Broadcaster#broadcast(Object)}
     * operations.
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @Schedule(period = 10, waitFor = 5)
    @POST
    @Path("delaySchedule")
    public Broadcastable delaySchedule(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Execute periodic {@link org.atmosphere.cpr.Broadcaster#broadcast(Object)} operation.
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @Schedule(period = 5)
    @POST
    @Path("schedule")
    public Broadcastable schedule(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Delay until the next broadcast
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @Broadcast(delay = 0)
    @POST
    @Path("delay")
    public Broadcastable delayPublish(@FormParam("message") String message) {
        return broadcast(message);
    }

    @Broadcast(delay = 5, resumeOnBroadcast = true)
    @POST
    @Path("delayAndResume")
    public Broadcastable delayPublishAndResume(@FormParam("message") String message) {
        return broadcast(message);
    }

    /**
     * Use the {@link org.atmosphere.cpr.Broadcaster#delayBroadcast(Object)} directly
     * instead of using the annotation.
     *
     * @param message A String from an HTML form
     * @return A {@link org.atmosphere.jersey.Broadcastable} used to broadcast events.
     */
    @POST
    @Path("programmaticDelayBroadcast")
    public String manualDelayBroadcast(@FormParam("message") String message) {
        broadcaster.delayBroadcast(message);
        return message;
    }

    /**
     * Create a new {@link org.atmosphere.jersey.Broadcastable}.
     *
     * @param m
     * @return
     */
    Broadcastable broadcast(String m) {
        return new Broadcastable(m + "\n", broadcaster);
    }

}