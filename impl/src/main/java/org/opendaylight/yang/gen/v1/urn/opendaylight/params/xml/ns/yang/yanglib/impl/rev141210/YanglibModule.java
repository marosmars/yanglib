/*
 * IETF Hackathon and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yanglib.impl.rev141210;

import java.io.File;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.yanglib.impl.YanglibProvider;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YanglibModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yanglib.impl.rev141210.AbstractYanglibModule {

    private static final Logger LOG = LoggerFactory.getLogger(YanglibModule.class);

    public YanglibModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public YanglibModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yanglib.impl.rev141210.YanglibModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getCacheFolder(), cacheFolderJmxAttribute);
        final File file = new File(getCacheFolder());
        JmxAttributeValidationException.checkCondition(file.exists(), "Non existing cache file", cacheFolderJmxAttribute);
        JmxAttributeValidationException.checkCondition(file.isDirectory(), "Non directory cache file", cacheFolderJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO add dependency on other repositories that should be aggregated into this one (using one library as aggregator) .. this would be alternative to the filesystem folder for cache
        // TODO make cache folder optional
        // Start cache and Text to AST transformer
        final SharedSchemaRepository repository = new SharedSchemaRepository("yang-library");
        YanglibProvider provider = new YanglibProvider(repository, getBindingAddr(), getBindingPort());

        final FilesystemSchemaSourceCache<YangTextSchemaSource> cache =
                new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File(getCacheFolder()));
        repository.registerSchemaSourceListener(cache);
        LOG.info("Starting yang library with sources from {}", getCacheFolder());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
