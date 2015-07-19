package org.opendaylight.yanglib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;

@Path("/")
public class YanglibService {

    private static YanglibService INSTANCE = new YanglibService();

    private SharedSchemaRepository repo;

    public static YanglibService getInstance() {
        return INSTANCE;
    }

    @GET
    @Path("/schemas/{modelName}{p:/?}{revision:([0-9\\-]*)}")
    public String getSchema(@PathParam("modelName") String name, @PathParam("revision") String revision) {
        Preconditions.checkState(repo != null);
        final SourceIdentifier id = new SourceIdentifier(name, Optional.fromNullable(revision.equals("") ? null : revision));
        final CheckedFuture<YangTextSchemaSource, SchemaSourceException> schemaSource = repo.getSchemaSource(id, YangTextSchemaSource.class);
        try {
            final YangTextSchemaSource yangTextSchemaSource = schemaSource.checkedGet(1, TimeUnit.MINUTES);
//            return "YANG schema";
            return new String(ByteStreams.toByteArray(yangTextSchemaSource.openStream()));
        } catch (TimeoutException e) {
            throw new IllegalStateException("Unable to get schema in time " + id, e);
        } catch (SchemaSourceException e) {
            throw new IllegalStateException("Unable to get schema" + id, e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read schema" + id, e);
        }
    }

    public void setRepo(final SharedSchemaRepository repo) {
        this.repo = repo;
    }
}
