package org.acme;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/clientes")
public class ClientesResource {

    private static final RestResponse<LimiteSaldo> NOT_FOUND = RestResponse.notFound();
    private static final RestResponse<LimiteSaldo> UNPROCESSABLE = RestResponse.status(422);

    @Inject
    Logger logger;

    void onStart(@Observes StartupEvent se) throws IOException {
        new File("1").createNewFile();
        new File("2").createNewFile();
        new File("3").createNewFile();
        new File("4").createNewFile();
        new File("5").createNewFile();
        logger.info("Arquivos OK");
    }

    @POST
    @Path("/{id}/transacoes")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public RestResponse<LimiteSaldo> debitarCreditar(@PathParam("id") Integer id, TransacaoEntrada te) throws IOException {

        if (!existeCliente(id)) {
            return NOT_FOUND;
        }
        if (!te.ehValida()) {
            return UNPROCESSABLE;
        }
        te.cliente_id = id;

        Transacao t = Transacao.of(te);
        int valor = Integer.parseInt(te.valor);

        String file = id.toString();
        try (var fileChannel = FileChannel.open(java.nio.file.Path.of(file), java.nio.file.StandardOpenOption.WRITE)) {

        }

        // QuarkusTransaction.begin();

        // SaldoCliente saldoCliente = SaldoCliente.findById(id,
        // LockModeType.PESSIMISTIC_WRITE);
        // if (te.tipo.charValue() == 'c') {
        // saldoCliente.saldo += valor;
        // } else {
        // if (saldoCliente.saldo - valor < -saldoCliente.limite) {
        // // QuarkusTransaction.rollback();
        // return UNPROCESSABLE;
        // }
        // saldoCliente.saldo -= valor;
        // }
        // t.limite = saldoCliente.limite;
        // t.saldo = saldoCliente.saldo;

        // saldoCliente.persist();
        // t.persist();
        // QuarkusTransaction.commit();

        // return RestResponse.ok(new LimiteSaldo(saldoCliente.saldo,
        // saldoCliente.limite));
        return RestResponse.ok(new LimiteSaldo(1, 2));
    }

    @GET
    @Path("/{id}/extrato")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public RestResponse<JsonObject> extrato(@PathParam("id") Integer id) throws IOException {
        if (!existeCliente(id)) {
            return RestResponse.notFound();
        }

        String file = id.toString();
        try (var fileChannel = FileChannel.open(java.nio.file.Path.of(file), java.nio.file.StandardOpenOption.READ)) {
            var buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            String str = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.println("s: " + str);
            JsonObject jsonObject = new JsonObject(str);
            jsonObject.getJsonObject("saldo").put("data_extrato", LocalDateTime.now());

            return RestResponse.ok(jsonObject);
        }

    }

    private boolean existeCliente(int id) {
        return id < 6 && id > 0;
    }
}
