package org.neo4j.smack.test.integration;
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

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Version;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.smack.test.util.AbstractRestFunctionalTestBase;
import org.neo4j.smack.test.util.JaxRsResponse;
import org.neo4j.smack.test.util.JsonHelper;
import org.neo4j.smack.test.util.RestRequest;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.TestData;

import java.util.Map;

import static org.junit.Assert.*;

public class DataAPIRootIT extends AbstractRestFunctionalTestBase
{
    /**
     * The service root is your starting point to discover the REST API.
     * It contains the basic starting points for the databse, and some
     * version and extension information. The +reference_node+ entry will
     * only be present if there is a reference node set and exists in the database.
     */
    @Documented
    @Test
    @Ignore
    @Graph("I know you")
    @TestData.Title( "Get service root" )
    public void assert200OkFromGet() throws Exception
    {
        AbstractGraphDatabase db = (AbstractGraphDatabase)graphdb();
        Transaction tx = db.beginTx();
        db.getConfig().getGraphDbModule().setReferenceNodeId( data.get().get("I").getId() );
        tx.success();
        tx.finish();
        String body = gen.get().expectedStatus( 200 ).get( getDataUri() ).entity();
        Map<String, Object> map = JsonHelper.jsonToMap( body );
        assertEquals( getDataUri() + "node", map.get( "node" ) );
        assertNotNull( map.get( "reference_node" ) );
        assertNotNull( map.get( "node_index" ) );
        assertNotNull( map.get( "relationship_index" ) );
        assertNotNull( map.get( "extensions_info" ) );
        assertNotNull( map.get( "batch" ) );
        assertEquals( Version.getKernelRevision(), map.get( "neo4j_version" ) );

        // Make sure advertised urls work
            JaxRsResponse response = RestRequest.req().get( getDataUri() );
        if ( map.get( "reference_node" ) != null )
        {
            response = RestRequest.req().get(
                    (String) map.get( "reference_node" ) );
            assertEquals( 200, response.getStatus() );
            response.close();
        }
        response = RestRequest.req().get( (String) map.get( "node_index" ) );
        assertTrue( response.getStatus() == 200 || response.getStatus() == 204 );
        response.close();

        response = RestRequest.req().get(
                (String) map.get( "relationship_index" ) );
        assertTrue( response.getStatus() == 200 || response.getStatus() == 204 );
        response.close();

        response = RestRequest.req().get( (String) map.get( "extensions_info" ) );
        assertEquals( 200, response.getStatus() );
        response.close();

        response = RestRequest.req().post( (String) map.get( "batch" ), "[]" );
        assertEquals( 200, response.getStatus() );
        response.close();
    }
}
