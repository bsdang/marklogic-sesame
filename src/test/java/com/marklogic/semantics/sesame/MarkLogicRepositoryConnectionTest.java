package com.marklogic.semantics.sesame;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;

public class MarkLogicRepositoryConnectionTest {

    @Test
    public void testSPARQLQuery()
            throws Exception
    {

        MarkLogicRepository mr = new MarkLogicRepository();

        mr.shutDown();
        mr.initialize();

        MarkLogicRepositoryConnection con = (MarkLogicRepositoryConnection) mr.getConnection();

        Assert.assertTrue( con != null );
        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 2 ";
        TupleQuery tupleQuery =  con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult results = tupleQuery.evaluate();

        try {
            results.hasNext();
            BindingSet bindingSet = results.next();

            Value sV = bindingSet.getValue("s");
            Value pV = bindingSet.getValue("p");
            Value oV = bindingSet.getValue("o");

            Assert.assertEquals("http://example.org/marklogic/people/Jack_Smith",sV.stringValue());
            Assert.assertEquals("http://example.org/marklogic/predicate/livesIn",pV.stringValue());
            Assert.assertEquals("Glasgow", oV.stringValue());

            results.hasNext();
            BindingSet bindingSet1 = results.next();

            Value sV1 = bindingSet1.getValue("s");
            Value pV1 = bindingSet1.getValue("p");
            Value oV1 = bindingSet1.getValue("o");

            Assert.assertEquals("http://example.org/marklogic/people/Jane_Smith",sV1.stringValue());
            Assert.assertEquals("http://example.org/marklogic/predicate/livesIn",pV1.stringValue());
            Assert.assertEquals("London", oV1.stringValue());
        }
        finally {
            results.close();
        }
        con.close();
        }

    @Ignore
    public void testContextIDs()
            throws Exception
    {

        MarkLogicRepository mr = new MarkLogicRepository();
        mr.initialize();
        MarkLogicRepositoryConnection con = (MarkLogicRepositoryConnection) mr.getConnection();
        RepositoryResult<Statement> result = con.getStatements(RDF.TYPE, RDF.TYPE, null, true);
        try {
            Assert.assertTrue("result should not be empty", result.hasNext());
        }
        finally {
            result.close();
        }

        result = con.getStatements(RDF.TYPE, RDF.TYPE, null, false);
        try {
            Assert.assertFalse("result should be empty", result.hasNext());
        }
        finally {
            result.close();
        }
    }
}
