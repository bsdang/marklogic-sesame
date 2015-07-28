/*
 * Copyright 2015 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * A library that enables access to a MarkLogic-backed triple-store via the
 * Sesame API.
 */
package com.marklogic.semantics.sesame;

import com.marklogic.semantics.sesame.client.MarkLogicClient;
import com.marklogic.semantics.sesame.query.MarkLogicBooleanQuery;
import com.marklogic.semantics.sesame.query.MarkLogicGraphQuery;
import com.marklogic.semantics.sesame.query.MarkLogicTupleQuery;
import com.marklogic.semantics.sesame.query.MarkLogicUpdateQuery;
import info.aduna.iteration.*;
import org.openrdf.IsolationLevel;
import org.openrdf.IsolationLevels;
import org.openrdf.model.*;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.*;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.UnknownTransactionStateException;
import org.openrdf.repository.base.RepositoryConnectionBase;
import org.openrdf.repository.sparql.query.SPARQLQueryBindingSet;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import static org.openrdf.query.QueryLanguage.SPARQL;

/**
 *
 * @author James Fuller
 */
public class MarkLogicRepositoryConnection extends RepositoryConnectionBase implements RepositoryConnection {

    protected final Logger logger = LoggerFactory.getLogger(MarkLogicRepositoryConnection.class);

    private static final String EVERYTHING = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";

    private static final String EVERYTHING_WITH_GRAPH = "SELECT * WHERE {  ?s ?p ?o . OPTIONAL { GRAPH ?ctx { ?s ?p ?o } } }";

    private static final String SOMETHING = "ASK { ?s ?p ?o }";

    private static final String NAMEDGRAPHS = "SELECT DISTINCT ?_ WHERE { GRAPH ?_ { ?s ?p ?o } }";

    private final boolean quadMode;

    private MarkLogicClient client;

    //constructor
    public MarkLogicRepositoryConnection(MarkLogicRepository repository, MarkLogicClient client, boolean quadMode) {
        super(repository);
        this.client = client;
        this.quadMode = quadMode;
        client.setValueFactory(repository.getValueFactory());
    }

    // valuefactory
    @Override
    public ValueFactory getValueFactory() {
        return client.getValueFactory();
    }
    public void setValueFactory(ValueFactory f) {
        client.setValueFactory(f);
    }

    // prepareQuery entrypoint
    @Override
    public Query prepareQuery(QueryLanguage queryLanguage, String queryString) throws RepositoryException, MalformedQueryException {
        return prepareTupleQuery(queryLanguage, queryString, "");
    }
    @Override
    public Query prepareQuery(QueryLanguage queryLanguage, String queryString, String baseURI)
            throws RepositoryException, MalformedQueryException
    {
        if (SPARQL.equals(queryLanguage)) {
            String strippedQuery = QueryParserUtil.removeSPARQLQueryProlog(queryString).toUpperCase();
            if (strippedQuery.startsWith("SELECT")) {
                return prepareTupleQuery(queryLanguage, queryString, baseURI);
            }
            else if (strippedQuery.startsWith("ASK")) {
                return prepareBooleanQuery(queryLanguage, queryString, baseURI);
            }
            else {
                return prepareGraphQuery(queryLanguage, queryString, baseURI);
            }
        }
        throw new UnsupportedOperationException("Unsupported query language " + queryLanguage.getName());
    }

    // prepareTupleQuery
    public TupleQuery prepareTupleQuery(String queryString) throws RepositoryException, MalformedQueryException {
        return prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    }
    @Override
    public TupleQuery prepareTupleQuery(QueryLanguage queryLanguage, String queryString) throws RepositoryException, MalformedQueryException {
        return prepareTupleQuery(queryLanguage, queryString, "");
    }
    @Override
    public TupleQuery prepareTupleQuery(QueryLanguage queryLanguage, String queryString, String baseURI) throws RepositoryException, MalformedQueryException {
        if (QueryLanguage.SPARQL.equals(queryLanguage)) {
            return new MarkLogicTupleQuery(client, new SPARQLQueryBindingSet(), baseURI, queryString);
        }
        throw new UnsupportedQueryLanguageException("Unsupported query language " + queryLanguage.getName());
    }

    // prepareGraphQuery
    public GraphQuery prepareGraphQuery(String queryString) throws RepositoryException, MalformedQueryException {
        return prepareGraphQuery(QueryLanguage.SPARQL, queryString, "");
    }
    @Override
    public GraphQuery prepareGraphQuery(QueryLanguage queryLanguage, String queryString) throws RepositoryException, MalformedQueryException {
        return prepareGraphQuery(queryLanguage, queryString, "");
    }
    @Override
    public GraphQuery prepareGraphQuery(QueryLanguage queryLanguage, String queryString, String baseURI)
            throws RepositoryException, MalformedQueryException
    {
        if (QueryLanguage.SPARQL.equals(queryLanguage)) {
            return new MarkLogicGraphQuery(client, new SPARQLQueryBindingSet(), baseURI, queryString);
        }
        throw new UnsupportedQueryLanguageException("Unsupported query language " + queryLanguage.getName());
    }

    // prepareBooleanQuery
    public BooleanQuery prepareBooleanQuery(String queryString) throws RepositoryException, MalformedQueryException {
        return prepareBooleanQuery(QueryLanguage.SPARQL,queryString, "");
    }
    @Override
    public BooleanQuery prepareBooleanQuery(QueryLanguage queryLanguage, String queryString) throws RepositoryException, MalformedQueryException {
        return prepareBooleanQuery(queryLanguage, queryString, "");
    }
    @Override
    public BooleanQuery prepareBooleanQuery(QueryLanguage queryLanguage, String queryString, String baseURI) throws RepositoryException, MalformedQueryException {
        if (QueryLanguage.SPARQL.equals(queryLanguage)) {
            return new MarkLogicBooleanQuery(client, new SPARQLQueryBindingSet(), baseURI, queryString);
        }
        throw new UnsupportedQueryLanguageException("Unsupported query language " + queryLanguage.getName());
    }

    // prepareUpdate
    @Override
    public Update prepareUpdate(QueryLanguage queryLanguage, String queryString) throws RepositoryException, MalformedQueryException {
       return prepareUpdate(queryLanguage,queryString,"");
    }
    @Override
    public Update prepareUpdate(QueryLanguage queryLanguage, String queryString, String baseURI) throws RepositoryException, MalformedQueryException {
        if (QueryLanguage.SPARQL.equals(queryLanguage)) {
            return (Update) new MarkLogicUpdateQuery(client, new SPARQLQueryBindingSet(), baseURI, queryString);
        }
        throw new UnsupportedQueryLanguageException("Unsupported query language " + queryLanguage.getName());
    }

    //
    @Override
    public RepositoryResult<Resource> getContextIDs() throws RepositoryException {

        try{
            String queryString = "SELECT DISTINCT ?_ WHERE { GRAPH ?_ { ?s ?p ?o } }";
            TupleQuery tupleQuery = prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = tupleQuery.evaluate();
            return
                    new RepositoryResult<Resource>(
                            new ExceptionConvertingIteration<Resource, RepositoryException>(
                                    new ConvertingIteration<BindingSet, Resource, QueryEvaluationException>(result) {

                                        @Override
                                        protected Resource convert(BindingSet bindings)
                                                throws QueryEvaluationException {
                                            return (Resource) bindings.getValue("_");
                                        }
                                    }) {

                                @Override
                                protected RepositoryException convert(Exception e) {
                                    return new RepositoryException(e);
                                }
                            });

        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    // statements
    @Override
    public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
        try {
            if (isQuadMode()) {
                TupleQuery tupleQuery = prepareTupleQuery(SPARQL, EVERYTHING_WITH_GRAPH);
                setBindings(tupleQuery, subj, pred, obj, contexts);
                tupleQuery.setIncludeInferred(includeInferred);
                TupleQueryResult qRes = tupleQuery.evaluate();
                return new RepositoryResult<Statement>(
                        new ExceptionConvertingIteration<Statement, RepositoryException>(
                                toStatementIteration(qRes, subj, pred, obj)) {
                            @Override
                            protected RepositoryException convert(Exception e) {
                                return new RepositoryException(e);
                            }
                        });
            }
            if (subj != null && pred != null && obj != null) {
                if (hasStatement(subj, pred, obj, includeInferred, contexts)) {
                    Statement st = new StatementImpl(subj, pred, obj);
                    CloseableIteration<Statement, RepositoryException> cursor;
                    cursor = new SingletonIteration<Statement, RepositoryException>(st);
                    return new RepositoryResult<Statement>(cursor);
                } else {
                    return new RepositoryResult<Statement>(new EmptyIteration<Statement, RepositoryException>());
                }
            }

            GraphQuery query = prepareGraphQuery(SPARQL, EVERYTHING, "");
            setBindings(query, subj, pred, obj, contexts);
            GraphQueryResult result = query.evaluate();
            return new RepositoryResult<Statement>(
                    new ExceptionConvertingIteration<Statement, RepositoryException>(result) {

                        @Override
                        protected RepositoryException convert(Exception e) {
                            return new RepositoryException(e);
                        }
                    });
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }
    @Override
    public boolean hasStatement(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
        try {
            BooleanQuery query = prepareBooleanQuery(SPARQL, SOMETHING, "");
            setBindings(query, subj, pred, obj, contexts);
            return query.evaluate();
        }
        catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        }
        catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }
    @Override
    public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts) throws RepositoryException {
        return false;
    }
    @Override
    public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred, RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
    }

    //
    @Override
    public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
    }

    //
    @Override
    public long size(Resource... contexts) throws RepositoryException {
        RepositoryResult<Statement> stmts = getStatements(null, null, null, true, contexts);
        try {
            long i = 0;
            while (stmts.hasNext()) {
                stmts.next();
                i++;
            }
            return i;
        }
        finally {
            stmts.close();
        }
    }

    @Override
    public boolean isEmpty() throws RepositoryException {
        return size() == 0;
    }

    @Override
    public boolean isAutoCommit() throws RepositoryException {
        return client.isActiveTransaction() == false;
    }
    @Override
    public void setAutoCommit(boolean autoCommit) throws RepositoryException {
        client.setAutoCommit();
    }
    @Override
    public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
        return client.isActiveTransaction();
    }
    @Override
    public IsolationLevel getIsolationLevel() {
        return IsolationLevels.SNAPSHOT;
    }
    @Override
    public void setIsolationLevel(IsolationLevel level) throws IllegalStateException {
        if(level != IsolationLevels.SNAPSHOT){
         throw new IllegalStateException();
        }
    }
    @Override
    public void begin() throws RepositoryException {
        client.openTransaction();
    }
    @Override
    public void begin(IsolationLevel level) throws RepositoryException {
        setIsolationLevel(level);
        begin();
    }
    @Override
    public void commit() throws RepositoryException {
        client.commitTransaction();
    }
    @Override
    public void rollback() throws RepositoryException {
        client.rollbackTransaction();
    }

    // add
    @Override
    public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
        client.sendAdd(in,baseURI,dataFormat,contexts);
    }
    @Override
    public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
        client.sendAdd(file,baseURI,dataFormat,contexts);
    }
    @Override
    public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {

    }
    @Override
    public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {

    }
    @Override
    public void add(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
        client.sendAdd(subject,predicate,object,contexts);
    }
    @Override
    public void add(Statement st, Resource... contexts) throws RepositoryException {

    }
    @Override
    public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {

    }
    @Override
    public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts) throws RepositoryException, E {

    }

    // remove
    @Override
    public void remove(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
        client.sendRemove(subject,predicate,object,contexts);
    }
    @Override
    public void remove(Statement st, Resource... contexts) throws RepositoryException {

    }
    @Override
    public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {

    }
    @Override
    public <E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts) throws RepositoryException, E {

    }

    //
    @Override
    public void clear(Resource... contexts) throws RepositoryException {
        client.sendClear(contexts);
    }

    //
    @Override
    protected void addWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
    }
    @Override
    protected void removeWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
    }

    // TBD - not in scope for 1.0.0
    @Override
    public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
        return null;
    }

    // TBD - not in scope for 1.0.0
    @Override
    public String getNamespace(String prefix) throws RepositoryException {
        return null;
    }
    // TBD - not in scope for 1.0.0
    @Override
    public void setNamespace(String prefix, String name) throws RepositoryException {
    }
    // TBD - not in scope for 1.0.0
    @Override
    public void removeNamespace(String prefix) throws RepositoryException {
    }
    // TBD - not in scope for 1.0.0
    @Override
    public void clearNamespaces() throws RepositoryException {
    }

    //
    private void setBindings(Query query, Resource subj, URI pred, Value obj, Resource... contexts)
            throws RepositoryException {
        if (subj != null) {
            query.setBinding("s", subj);
        }
        if (pred != null) {
            query.setBinding("p", pred);
        }
        if (obj != null) {
            query.setBinding("o", obj);
        }
        if (contexts != null && contexts.length > 0) {
            DatasetImpl dataset = new DatasetImpl();
            for (Resource ctx : contexts) {
                if (ctx == null || ctx instanceof URI) {
                    dataset.addDefaultGraph((URI) ctx);
                } else {
                    throw new RepositoryException("Contexts must be URIs");
                }
            }
            query.setDataset(dataset);
        }
    }

    //
    protected boolean isQuadMode() {
        return quadMode;
    }

    //
    protected Iteration<Statement, QueryEvaluationException> toStatementIteration(TupleQueryResult iter, final Resource subj, final URI pred, final Value obj) {

        return new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(iter) {

            @Override
            protected Statement convert(BindingSet b) throws QueryEvaluationException {

                Resource s = subj == null ? (Resource) b.getValue("s") : subj;
                URI p = pred == null ? getValueFactory().createURI(b.getValue("o").stringValue()) : pred;
                Value o = obj == null ? b.getValue("o") : obj;
                Resource ctx = (Resource) b.getValue("ctx");

                return getValueFactory().createStatement(s, p, o, ctx);
            }

        };
    }
}