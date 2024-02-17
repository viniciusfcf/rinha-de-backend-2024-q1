package org.acme;

import java.time.LocalDateTime;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("/clientes")
public class ClientesResource {

    private static final RestResponse<LimiteSaldo> NOT_FOUND = RestResponse.notFound();
    private static final RestResponse<LimiteSaldo> UNPROCESSABLE = RestResponse.status(422);
    @Inject
    Logger logger;

    void onStart(@Observes StartupEvent se) {
        logger.info("Apagando transacoes");
        QuarkusTransaction.begin();
        Panache.getEntityManager().createNativeQuery("update public.saldocliente set saldo = 0").executeUpdate();
        Panache.getEntityManager().createNativeQuery("delete from public.transacao").executeUpdate();
        logger.info("Banco OK");
        QuarkusTransaction.commit();
    }

    @POST
    @Path("/{id}/transacoes")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public RestResponse<LimiteSaldo> debitarCreditar(@PathParam("id") Integer id, TransacaoEntrada te) {
        
        if (!existeCliente(id)) {
            return NOT_FOUND;
        }
        if (!te.ehValida()) {
            return UNPROCESSABLE;
        }
        te.cliente_id = id;

        Transacao t = Transacao.of(te);
        int valor = Integer.parseInt(te.valor);
        
        QuarkusTransaction.begin();
        
        SaldoCliente saldoCliente = SaldoCliente.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (te.tipo.charValue() == 'c') {
            saldoCliente.saldo += valor;
        } else {
            if (saldoCliente.saldo - valor < -saldoCliente.limite) {
                QuarkusTransaction.rollback();
                return UNPROCESSABLE;
            }
            saldoCliente.saldo -= valor;
        }
        t.limite = saldoCliente.limite;
        t.saldo = saldoCliente.saldo;
        
        saldoCliente.persist();
        t.persist();
        QuarkusTransaction.commit();

        return RestResponse.ok(new LimiteSaldo(saldoCliente.saldo, saldoCliente.limite));
    }

    @GET
    @Path("/{id}/extrato")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public RestResponse<Extrato> extrato(@PathParam("id") Integer id) {
        if (!existeCliente(id)) {
            return RestResponse.notFound();
        }
        Extrato extrato = new Extrato();
        extrato.saldo = new Saldo();
        extrato.saldo.data_extrato = LocalDateTime.now();
        extrato.ultimas_transacoes = Transacao.find("cliente_id = ?1 order by id desc", id).page(0, 10).list();
        if (!extrato.ultimas_transacoes.isEmpty()) {
            Transacao ultimaTransacao = extrato.ultimas_transacoes.get(0);
            extrato.saldo.total = ultimaTransacao.saldo;
            extrato.saldo.limite = ultimaTransacao.limite;
        } else {
            SaldoCliente saldoCliente = SaldoCliente.findById(id);
            extrato.saldo.total = saldoCliente.saldo;
            extrato.saldo.limite = saldoCliente.limite;
        }
        return RestResponse.ok(extrato);
    }

    private boolean existeCliente(int id) {
        return id < 6 && id > 0;
        // return Cliente.findById(id) != null;
    }
}
