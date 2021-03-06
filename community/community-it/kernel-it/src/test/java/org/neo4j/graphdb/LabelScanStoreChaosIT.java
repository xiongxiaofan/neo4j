/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.graphdb;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.neo4j.internal.index.label.TokenScanStoreTest;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.EmbeddedDbmsRule;
import org.neo4j.test.rule.RandomRule;

import static org.junit.Assert.assertEquals;
import static org.neo4j.internal.helpers.collection.Iterators.asSet;

/**
 * Tests functionality around missing or corrupted label scan store index, and that
 * the database should repair (i.e. rebuild) that automatically and just work.
 */
public class LabelScanStoreChaosIT
{
    private final DbmsRule dbRule = new EmbeddedDbmsRule();
    private final RandomRule randomRule = new RandomRule();
    @Rule
    public final RuleChain ruleChain = RuleChain.outerRule( randomRule ).around( dbRule );

    @Test
    public void shouldRebuildDeletedLabelScanStoreOnStartup() throws Exception
    {
        // GIVEN
        Node node1 = createLabeledNode( Labels.First );
        Node node2 = createLabeledNode( Labels.First );
        Node node3 = createLabeledNode( Labels.First );
        deleteNode( node2 ); // just to create a hole in the store

        // WHEN
        dbRule.restartDatabase( deleteTheLabelScanStoreIndex() );

        // THEN
        assertEquals( asSet( node1, node3 ), getAllNodesWithLabel( Labels.First ) );
    }

    @Test
    public void rebuildCorruptedLabelScanStoreToStartup() throws Exception
    {
        Node node = createLabeledNode( Labels.First );

        dbRule.restartDatabase( corruptTheLabelScanStoreIndex() );

        assertEquals( asSet( node ), getAllNodesWithLabel( Labels.First ) );
    }

    private static File storeFile( DatabaseLayout databaseLayout )
    {
        return databaseLayout.labelScanStore().toFile();
    }

    private DbmsRule.RestartAction corruptTheLabelScanStoreIndex()
    {
        return ( fs, directory ) -> scrambleFile( storeFile( directory ) );
    }

    private static DbmsRule.RestartAction deleteTheLabelScanStoreIndex()
    {
        return ( fs, directory ) -> fs.deleteFile( storeFile( directory ) );
    }

    private Node createLabeledNode( Label... labels )
    {
        try ( Transaction tx = dbRule.getGraphDatabaseAPI().beginTx() )
        {
            Node node = tx.createNode( labels );
            tx.commit();
            return node;
        }
    }

    private Set<Node> getAllNodesWithLabel( Label label )
    {
        try ( Transaction tx = dbRule.getGraphDatabaseAPI().beginTx() )
        {
            return asSet( tx.findNodes( label ) );
        }
    }

    private void scrambleFile( File file ) throws IOException
    {
        TokenScanStoreTest.scrambleFile( randomRule.random(), file );
    }

    private void deleteNode( Node node )
    {
        try ( Transaction tx = dbRule.getGraphDatabaseAPI().beginTx() )
        {
            tx.getNodeById( node.getId() ).delete();
            tx.commit();
        }
    }

    private enum Labels implements Label
    {
        First, Second, Third
    }
}
