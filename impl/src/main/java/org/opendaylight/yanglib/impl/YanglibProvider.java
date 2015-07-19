/*
 * IETF Hackathon and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yanglib.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.Modules;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.ModulesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.OptionalRevision;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.module.Module;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.module.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev150703.module.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceListener;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YanglibProvider implements BindingAwareProvider, AutoCloseable, SchemaSourceListener {

    private static final Logger LOG = LoggerFactory.getLogger(YanglibProvider.class);
    private static final Predicate<PotentialSchemaSource<?>> YANG_SCHEMA_SOURCE = new Predicate<PotentialSchemaSource<?>>() {
        @Override
        public boolean apply(final PotentialSchemaSource<?> input) {
            // filter out non yang sources
            return YangTextSchemaSource.class.isAssignableFrom(input.getRepresentation());
        }
    };
    private DataBroker dataBroker;
    private SharedSchemaRepository repository;
    private String bindingAddr;
    private Long bindingPort;

    public YanglibProvider(final SharedSchemaRepository repository, final String bindingAddr, final Long bindingPort) {
        this.repository = repository;
        this.bindingAddr = bindingAddr;
        this.bindingPort = bindingPort;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("YanglibProvider Session Initiated");
        this.dataBroker = session.getSALService(DataBroker.class);
        repository.registerSchemaSourceListener(this);
        YanglibService.getInstance().setRepo(repository);
    }

    @Override
    public void close() throws Exception {
        LOG.info("YanglibProvider Closed");
        dataBroker = null;
    }

    @Override
    public void schemaSourceEncountered(final SchemaSourceRepresentation schemaSourceRepresentation) {
        LOG.debug("Source {} encountered", schemaSourceRepresentation.getIdentifier());
        // TODO now we might be able to provide additional information to the modules list
    }

    @Override
    public void schemaSourceRegistered(final Iterable<PotentialSchemaSource<?>> iterable) {
        final List<Module> newModules = new ArrayList<>();

        for (PotentialSchemaSource<?> potentialYangSource : Iterables.filter(iterable, YANG_SCHEMA_SOURCE)) {
            final YangIdentifier identifier = new YangIdentifier(potentialYangSource.getSourceIdentifier().getName());

            final String revision =
                    potentialYangSource.getSourceIdentifier().getRevision().equals(SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION) ? "" :
                            potentialYangSource.getSourceIdentifier().getRevision();

            final OptionalRevision optionalRevision = new OptionalRevision(revision);

            final Module newModule = new ModuleBuilder()
                    .setName(identifier)
                    .setRevision(optionalRevision)
                    .setSchema(getUrlForModule(potentialYangSource.getSourceIdentifier()))
                    .setKey(new ModuleKey(identifier, optionalRevision))
                    .build();
            newModules.add(newModule);
        }

        if(newModules.isEmpty()) {
            // If no new yang modules then do nothing
            return;
        }

        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Modules.class),
                new ModulesBuilder().setModuleSetId(getCurrentId()).setModule(newModules).build());

        writeTransaction.submit();
    }

    private Uri getUrlForModule(final SourceIdentifier sourceIdentifier) {
        final String revision = sourceIdentifier.getRevision().equals(SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION) ? "" : sourceIdentifier.getRevision();
        return new Uri("http://" + bindingAddr + ":" + bindingPort + "/yanglib/schemas/" + sourceIdentifier.getName() + "/" + revision);
    }

    private String getCurrentId() {
        return Long.toString(System.nanoTime());
    }

    @Override
    public void schemaSourceUnregistered(final PotentialSchemaSource<?> potentialSchemaSource) {
        LOG.debug("Source {} unregistered", potentialSchemaSource.getSourceIdentifier());
        // FIXME impl
    }

    // TODO add a listener to modules subtree in order to update the timestamp

    // TODO allow submitting of new sources into the cache using an RPC or something

    // TODO consider putting all remote sources added by users into the repository/schema .. but would we change the url then ?
}
