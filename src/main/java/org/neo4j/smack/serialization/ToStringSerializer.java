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
package org.neo4j.smack.serialization;


import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public class ToStringSerializer implements Serializer {
    StringBuilder buffer = new StringBuilder();

    @Override
    public void putEnum(Enum en) {
        
    }

    @Override
    public void putString(String string) {
        buffer.append(string);
    }
    
    public String toString() {
        return buffer.toString();
    }

    @Override
    public void putMap(Map<String, Object> data) {
        buffer.append(data);
    }

    @Override
    public void putNode(Node node) throws SerializationException {
        putMap(GraphElementSerializer.toNodeMap(node));
    }


    @Override
    public void putRelationship(Relationship rel) throws SerializationException {
        putMap(GraphElementSerializer.toRelationshipMap(rel));
    }

    @Override
    public void putRaw(String value) {
        putString(value);
    }

    @Override
    public MediaType getContentType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }
}
