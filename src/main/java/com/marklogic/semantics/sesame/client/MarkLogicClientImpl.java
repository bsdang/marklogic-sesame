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
package com.marklogic.semantics.sesame.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.Transaction;
import com.marklogic.client.impl.SPARQLBindingsImpl;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.semantics.GraphManager;
import com.marklogic.client.semantics.SPARQLBindings;
import com.marklogic.client.semantics.SPARQLQueryDefinition;
import com.marklogic.client.semantics.SPARQLQueryManager;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.repository.sparql.query.SPARQLQueryBindingSet;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 *
 * @author James Fuller
 */
public class MarkLogicClientImpl {

    protected final Logger logger = LoggerFactory.getLogger(MarkLogicClientImpl.class);

    private String host;

    private int port;

    private String user;

    private String password;

    private String auth;

    static public SPARQLQueryManager sparqlManager;

    protected static DatabaseClientFactory.Authentication authType = DatabaseClientFactory.Authentication.valueOf(
            "DIGEST"
    );

    protected DatabaseClient databaseClient;

    // constructor
    public MarkLogicClientImpl(String host, int port, String user, String password, String auth) {
        this.databaseClient = DatabaseClientFactory.newClient(host, port, user, password, DatabaseClientFactory.Authentication.valueOf(auth));
    }



    // host
    public String getHost() {
        return this.host;
    }
    public void setHost(String host) {
        this.host = host;
    }

    // port
    public int getPort() {
        return this.port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    // user
    public String getUser() {
        return this.user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    // password
    public String getPassword() {
        return password;
    }

    public void setPassword() {
        this.password = password;
    }

    // auth
    public String getAuth() {
        return auth;
    }
    public void setAuth(String auth) {
        this.auth = auth;
        this.authType = DatabaseClientFactory.Authentication.valueOf(
                auth
        );
    }

    // auth type
    public void setAuthType(DatabaseClientFactory.Authentication authType) {
        MarkLogicClientImpl.authType = authType;
    }
    public DatabaseClientFactory.Authentication getAuthType() {
        return authType;
    }

    //
    public DatabaseClient getDatabaseClient() {
        return databaseClient;
    }

    // performSPARQLQuery
    public InputStream performSPARQLQuery(String queryString, SPARQLQueryBindingSet bindings, long start, long pageLength, Transaction tx, boolean includeInferred) throws JsonProcessingException {
        return performSPARQLQuery(queryString, bindings, new InputStreamHandle(), start, pageLength, tx,includeInferred);
    }
    public InputStream performSPARQLQuery(String queryString, SPARQLQueryBindingSet bindings, InputStreamHandle handle, long start, long pageLength, Transaction tx, boolean includeInferred) throws JsonProcessingException {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();
        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(queryString);
        qdef.setIncludeDefaultRulesets(includeInferred);

        qdef.setBindings(getSPARQLBindings(bindings));
        sparqlManager.executeSelect(qdef, handle, start, pageLength, tx);
        return handle.get();
    }

    // performGraphQuery
    public InputStream performGraphQuery(String queryString, SPARQLQueryBindingSet bindings, Transaction tx, boolean includeInferred) throws JsonProcessingException {
        return performGraphQuery(queryString, bindings, new InputStreamHandle(), tx, includeInferred);
    }
    public InputStream performGraphQuery(String queryString, SPARQLQueryBindingSet bindings, InputStreamHandle handle, Transaction tx, boolean includeInferred) throws JsonProcessingException {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();

        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(queryString);
        qdef.setIncludeDefaultRulesets(includeInferred);

        qdef.setBindings(getSPARQLBindings(bindings));
        sparqlManager.executeDescribe(qdef, handle, tx);
        return handle.get();
    }

    // performBooleanQuery
    public boolean performBooleanQuery(String queryString, SPARQLQueryBindingSet bindings, Transaction tx, boolean includeInferred) {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();
        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(queryString);
        qdef.setIncludeDefaultRulesets(includeInferred);

        qdef.setBindings(getSPARQLBindings(bindings));
        return sparqlManager.executeAsk(qdef, tx);
    }

    // performUpdateQuery
    public void performUpdateQuery(String queryString, SPARQLQueryBindingSet bindings, Transaction tx, boolean includeInferred) {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();
        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(queryString);
        qdef.setIncludeDefaultRulesets(includeInferred);
        qdef.setBindings(getSPARQLBindings(bindings));
        sparqlManager.executeUpdate(qdef, tx);
    }

    // performAdd
    public void performAdd(File file, String baseURI, RDFFormat dataFormat,Transaction tx,Resource... contexts){
        GraphManager gmgr = getDatabaseClient().newGraphManager();
        gmgr.setDefaultMimetype(dataFormat.getDefaultMIMEType());
        //TBD- must be more efficient method to deal with this
        for(Resource context: contexts) {
            gmgr.write(context.toString(), new FileHandle(file),tx);
        }
    }
    public void performAdd(InputStream in, String baseURI, RDFFormat dataFormat,Transaction tx,Resource... contexts) {
        GraphManager gmgr = getDatabaseClient().newGraphManager();
        gmgr.setDefaultMimetype(dataFormat.getDefaultMIMEType());
        // TBD- must handle multiple contexts
        gmgr.write(contexts[0].stringValue(), new InputStreamHandle(in), tx);
    }
    public void performAdd(Resource subject,URI predicate, Value object,Transaction tx,Resource... contexts) {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();
        // TBD- must handle multiple contexts

        String query = "INSERT DATA { GRAPH <" + contexts[0].stringValue() + "> { ?s ?p ?o } }";

        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(query);
        qdef.withBinding("s", subject.stringValue());
        qdef.withBinding("p", predicate.stringValue());
        qdef.withBinding("o", object.stringValue());
        sparqlManager.executeUpdate(qdef,tx);
    }

    // performRemove
    public void performRemove(Resource subject,URI predicate, Value object,Transaction tx,Resource... contexts) {
        sparqlManager = getDatabaseClient().newSPARQLQueryManager();
        // TBD- must handle multiple contexts

        String query = "DELETE WHERE { GRAPH <" + contexts[0].stringValue() + "> { ?s ?p ?o } }";

        SPARQLQueryDefinition qdef = sparqlManager.newQueryDefinition(query);
        qdef.withBinding("s", subject.stringValue());
        qdef.withBinding("p", predicate.stringValue());
        qdef.withBinding("o", object.stringValue());
        sparqlManager.executeUpdate(qdef,tx);
    }

    // performClear
    public void performClear(Transaction tx, Resource... contexts){
        GraphManager gmgr = getDatabaseClient().newGraphManager();
        for(Resource context : contexts) {
            gmgr.delete(context.stringValue(), tx);
        }
    }
    public void performClearAll(Transaction tx){
        GraphManager gmgr = getDatabaseClient().newGraphManager();
        gmgr.deleteGraphs();
    }

    // getSPARQLBindings
    protected SPARQLBindings getSPARQLBindings(SPARQLQueryBindingSet bindings){
        SPARQLBindings sps = new SPARQLBindingsImpl();
        for (Binding binding : bindings) {
            sps.bind(binding.getName(), binding.getValue().stringValue());
            logger.debug("binding:" + binding.getName() + "=" + binding.getValue());
        }
        return sps;
    }
}