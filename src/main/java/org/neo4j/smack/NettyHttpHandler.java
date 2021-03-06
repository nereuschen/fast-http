/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.smack;

import com.lmax.disruptor.RingBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.neo4j.smack.event.RequestEvent;
import org.neo4j.smack.routing.InvocationVerb;

import java.util.concurrent.atomic.AtomicLong;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpHandler extends SimpleChannelHandler {

    private RingBuffer<RequestEvent> workBuffer;

    public NettyHttpHandler(RingBuffer<RequestEvent> workBuffer) {
        this.workBuffer = workBuffer;
    }
    
    static AtomicLong i = new AtomicLong(0l);
    static AtomicLong connectionId = new AtomicLong(0l);
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        HttpRequest httpRequest = (HttpRequest) e.getMessage();
        
        long sequenceNo = workBuffer.next();
        RequestEvent event = workBuffer.get(sequenceNo);
        event.setId(i.incrementAndGet());
        addMethod(httpRequest, event);
        addParamsAndPath(httpRequest, event);
        // e.getRemoteAddress() // todo routing ?
        event.setIsPersistentConnection(isKeepAlive(httpRequest));
        event.setContent(httpRequest.getContent());
        event.setContext(ctx);

        workBuffer.publish(sequenceNo);
    }

    private void addMethod(HttpRequest httpRequest, RequestEvent event) {
        final String methodName = httpRequest.getMethod().getName();
        event.setVerb(InvocationVerb.valueOf(methodName.toUpperCase()));
    }

    private void addParamsAndPath(HttpRequest httpRequest, RequestEvent event) {
        final String uri = httpRequest.getUri();
        if (uri.contains("?")) {
            final QueryStringDecoder decoder = new QueryStringDecoder(uri);
            event.getPathVariables().add(decoder.getParameters());
            event.setPath(decoder.getPath());
        } else {
           event.setPath(uri);
        }
    }

    // todo use output buffer for exception handling
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        cause.printStackTrace();
        if (ch.isConnected()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        ctx.setAttachment(connectionId.incrementAndGet());
    }
    
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response)
                .addListener(ChannelFutureListener.CLOSE);
    }
    
}
