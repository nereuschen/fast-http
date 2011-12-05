package org.neo4j.smack.test.performance;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource.Builder;
import org.neo4j.smack.Database;
import org.neo4j.smack.SmackServer;
import org.neo4j.smack.event.Invocation;
import org.neo4j.smack.event.Result;
import org.neo4j.smack.routing.RoutingDefinition;
import org.neo4j.test.ImpermanentGraphDatabase;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.Date;

/**
 * This is meant as a source of feedback for experimenting
 * with improving network latency.
 * 
 * Suggested things to try:
 *   - Make sure we re use TCP connections
 *   - Drop the Jersey HTTP client, use a Netty based client instead
 *   - Look into adjusting TCP packet ACK rate from Java
 */
public class NetworkLatency {
    
    private static final String NO_SERIALIZATION_AND_NO_DESERIALIZATION = "/noserialnodeserial";
    
    public static class MockRoutes extends RoutingDefinition {{
        addRoute("", new Object() {
            @GET
            @Path(NO_SERIALIZATION_AND_NO_DESERIALIZATION)
            public void noSerializationAndNoDeserialization(Invocation req, Result res) {
                res.setOk();
            }
        });
    }}

    private SmackServer server;
    
    public static void main(String [] args) {
        NetworkLatency latency = new NetworkLatency();
        System.out.println("Running tests.. (this may take a while)");
        double avgLatency = latency.test();
        System.out.println("Average latency: " + avgLatency + "ms");
    }

    private double test() {
        try {
            
            int numRequests = 10000;
            
            startServer();
            
            Date start = new Date();
            sendXRequests("http://localhost:7473" + NO_SERIALIZATION_AND_NO_DESERIALIZATION, numRequests);
            Date end = new Date();
            
            long total = end.getTime() - start.getTime(); 
            return ((double)total)/numRequests;
            
        } finally {
            stopServer();
        }
    }
    
    private void sendXRequests(String uri, int numRequests) {
        Builder resource = Client.create().resource(uri).accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON);
        for(int i=0;i<numRequests;i++) {
            ClientResponse response = resource.get(ClientResponse.class);
        }
    }
    
    private void startServer() {
        server = new SmackServer("localhost", 7473, new Database(new ImpermanentGraphDatabase()));
        server.addRoute("",new MockRoutes());
        server.start();
    }
    
    private void stopServer() {
        server.stop();
    }
}
