package org.neo4j.smack.serialization.strategy;

import org.neo4j.graphdb.Node;
import org.neo4j.smack.serialization.SerializationException;
import org.neo4j.smack.serialization.SerializationModifier;
import org.neo4j.smack.serialization.SerializationStrategy;
import org.neo4j.smack.serialization.Serializer;

/**
 * @author mh
 * @since 27.11.11
 */
public class NodeSerializationStrategy implements SerializationStrategy<Node> {

    @Override
    public void serialize(Node node, Serializer out, SerializationModifier modifier) throws SerializationException {
        out.putNode(node);
    }
}
