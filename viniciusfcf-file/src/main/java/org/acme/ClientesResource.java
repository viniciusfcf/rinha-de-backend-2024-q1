package org.acme;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.JsonArray;
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
    public RestResponse<LimiteSaldo> debitarCreditar(@PathParam("id") Integer id, TransacaoEntrada te)
            throws IOException {

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
        try (RandomAccessFile writer = new RandomAccessFile(file, "rw")) {
            FileChannel fileChannel = writer.getChannel();

            var buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
            String str = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.println("s: " + str);
            JsonObject jsonObject = new JsonObject(str);
            JsonObject jsonSaldo = jsonObject.getJsonObject("saldo");
            
            JsonArray ultimasTransacoes = jsonObject.getJsonArray("ultimas_transacoes");
            
            Integer saldo = jsonSaldo.getInteger("total");
            Integer limite = jsonSaldo.getInteger("limite");
            if (te.tipo.charValue() == 'c') {
                saldo += valor;
            } else {
                if (saldo - valor < -limite) {
                    return UNPROCESSABLE;
                }
                saldo -= valor;
            }
            jsonSaldo.put("total", saldo);
            jsonSaldo.put("limite", limite);

            if (ultimasTransacoes.size() == 10) {
                ultimasTransacoes.remove(ultimasTransacoes.size() - 1);
            }
            ultimasTransacoes.add(0, t);
            ByteBuffer writeBuffer = ByteBuffer.wrap(jsonObject.toString().getBytes(StandardCharsets.UTF_8));

            fileChannel.write(writeBuffer);
            return RestResponse.ok(new LimiteSaldo(saldo, limite));
        }

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
