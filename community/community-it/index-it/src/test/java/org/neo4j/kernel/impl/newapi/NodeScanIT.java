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
package org.neo4j.kernel.impl.newapi;

import org.junit.jupiter.api.Test;

import org.neo4j.graphdb.Label;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.Kernel;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.extension.DbmsExtension;
import org.neo4j.test.extension.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.kernel.api.KernelTransaction.Type.IMPLICIT;
import static org.neo4j.kernel.api.security.AnonymousContext.read;

@DbmsExtension
class NodeScanIT
{
    @Inject
    private GraphDatabaseAPI db;

    @Test
    void trackPageCacheAccessOnNodeLabelScan() throws TransactionFailureException
    {
        Kernel kernel = db.getDependencyResolver().resolveDependency( Kernel.class );
        var testLabel = Label.label( "testLabel" );
        try ( KernelTransaction tx = kernel.beginTransaction( IMPLICIT, read() ) )
        {
            var cursorTracer = tx.pageCursorTracer();
            assertThat( cursorTracer.pins() ).isZero();

            var labelId = tx.tokenRead().nodeLabel( testLabel.name() );
            tx.dataRead().nodeLabelScan( labelId );

            assertThat( cursorTracer.pins() ).isNotZero();
        }
    }
}
