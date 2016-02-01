/*
 * Copyright 2015-2016 MarkLogic Corporation
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
package com.marklogic.semantics.sesame.config;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * representation of Repository config
 *
 * @author James Fuller
 */
public class MarkLogicRepositoryConfig extends RepositoryImplConfigBase {

	protected final Logger logger = LoggerFactory.getLogger(MarkLogicRepositoryConfig.class);

    public static ValueFactory vf= new ValueFactoryImpl();

    public static final URI QUERY_ENDPOINT = new URIImpl(
            "http://www.marklogic.com/v1/graphs/sparql");

    public static final URI UPDATE_ENDPOINT = new URIImpl(
            "http://www.marklogic.com/v1/graphs");

	private String queryEndpointUrl;
	private String updateEndpointUrl;

	private String host;
	private int port;
	private String user;
	private String password;
	private String auth;

    /**
	 * base constructor
	 */
	public MarkLogicRepositoryConfig() {
		super(MarkLogicRepositoryFactory.REPOSITORY_TYPE);
	}

    /**
	 * constructor initing with all connection details
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param auth
	 */
	public MarkLogicRepositoryConfig(String host, int port, String user, String password, String auth) {
        this();
        setHost(host);
        setPort(port);
        setUser(user);
        setPassword(password);
        setAuth(auth);
        setQueryEndpointUrl("http://" + user + ":" + password + "@" + host + ":" + port + "/v1/graphs/sparql");
        setUpdateEndpointUrl("http://" + user + ":" + password + "@" + host + ":" + port + "/v1/graphs");
    }

	/**
	 * constructor initing with query endpoint
	 *
	 * @param queryEndpointUrl
	 */
	public MarkLogicRepositoryConfig(String queryEndpointUrl) {
        setQueryEndpointUrl(queryEndpointUrl);
	}

	/**
	 * constructor initing with both query and update endpoint
	 *
	 * @param queryEndpointUrl
	 * @param updateEndpointUrl
	 */
	public MarkLogicRepositoryConfig(String queryEndpointUrl, String updateEndpointUrl) {
		this(queryEndpointUrl);
		setUpdateEndpointUrl(updateEndpointUrl);
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for host
	 *
	 * @return
	 */
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for port
	 *
	 * @return
	 */
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for user
	 *
	 */
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for password
	 *
	 */
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for auth
	 *
	 */
	public String getAuth() {
		return auth;
	}
	public void setAuth(String auth) {
		this.auth = auth;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for connection string
	 *
	 */
	public String getQueryEndpointUrl() {
		return queryEndpointUrl;
	}
	public void setQueryEndpointUrl(String url) {
		this.queryEndpointUrl = url;
	}

	/**
	 * MarkLogicRepositoryConfig specific getter/setter for connection string
	 *
	 */
	public String getUpdateEndpointUrl() {
		return updateEndpointUrl;
	}
	public void setUpdateEndpointUrl(String url) {
		this.updateEndpointUrl = url;
	}
	
	@Override
	/**
	 * validate configuration
	 *
	 */
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (getQueryEndpointUrl() == null) {
			throw new RepositoryConfigException(
					"No endpoint URL specified for SPARQL repository");
		}
	}

	@Override
	/**
	 * export graph representation of config
     *
	 * @Note - Graph is deprecating soon (in Sesame) to be replaced by Model
	 */
	public Resource export(Graph graph) {
		Resource implNode = super.export(graph);

		ValueFactory vf = graph.getValueFactory();
		if (getQueryEndpointUrl() != null) {
			graph.add(implNode, QUERY_ENDPOINT, vf.createURI(getQueryEndpointUrl()));
		}
		if (getUpdateEndpointUrl() != null) {
			graph.add(implNode, UPDATE_ENDPOINT, vf.createURI(getUpdateEndpointUrl()));
		}

		return implNode;
	}

	@Override
	/**
	 * parse graph representation of config
	 *
     * @Note - Graph is deprecating soon (in Sesame) to be replaced by Model
	 */
	public void parse(Graph graph, Resource implNode)
			throws RepositoryConfigException {
		super.parse(graph, implNode);

		try {
			URI uri = GraphUtil.getOptionalObjectURI(graph, implNode, QUERY_ENDPOINT);
			if (uri != null) {
				setQueryEndpointUrl(uri.stringValue());
			}

			uri = GraphUtil.getOptionalObjectURI(graph, implNode, UPDATE_ENDPOINT);
			if (uri != null) {
				setUpdateEndpointUrl(uri.stringValue());
			}
		} catch (GraphUtilException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}