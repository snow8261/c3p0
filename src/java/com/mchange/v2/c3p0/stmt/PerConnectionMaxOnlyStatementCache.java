/*
 * Distributed as part of c3p0 v.0.9.2
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.c3p0.stmt;

import java.sql.*;
import com.mchange.v2.async.AsynchronousRunner;

public final class PerConnectionMaxOnlyStatementCache extends GooGooStatementCache
{
    //MT: protected by this' lock
    int max_statements_per_connection;
    DeathmarchConnectionStatementManager dcsm;

    public PerConnectionMaxOnlyStatementCache(AsynchronousRunner blockingTaskAsyncRunner, AsynchronousRunner deferredStatementDestroyer, int max_statements_per_connection)
    {
	super( blockingTaskAsyncRunner, deferredStatementDestroyer );
	this.max_statements_per_connection = max_statements_per_connection;
    }

    //called only in parent's constructor
    protected ConnectionStatementManager createConnectionStatementManager()
    { return (this.dcsm = new DeathmarchConnectionStatementManager()); }

    //called by parent only with this' lock
    void addStatementToDeathmarches( Object pstmt, Connection physicalConnection )
    { dcsm.getDeathmarch( physicalConnection ).deathmarchStatement( pstmt ); }

    void removeStatementFromDeathmarches( Object pstmt, Connection physicalConnection )
    { dcsm.getDeathmarch( physicalConnection ).undeathmarchStatement( pstmt ); }

    boolean prepareAssimilateNewStatement(Connection pcon)
    {
	int cxn_stmt_count = dcsm.getNumStatementsForConnection( pcon );
	return ( cxn_stmt_count < max_statements_per_connection || (cxn_stmt_count == max_statements_per_connection && dcsm.getDeathmarch( pcon ).cullNext()) );
    }
}
