package org.acme;

import java.time.LocalDateTime;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.common.annotation.RunOnVirtualThread;
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

    @POST
    @Path("/{id}/transacoes")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public LimiteSaldo debitarCreditar(@PathParam("id") Integer id, TransacaoEntrada te) {

        if (!existeCliente(id)) {
            throw new WebApplicationException(404);
        }
        if (!te.ehValida()) {
            throw new WebApplicationException(422);
        }
        te.cliente_id = id;

        Transacao t = Transacao.of(te);
        int valor = Integer.parseInt(te.valor);
        
        QuarkusTransaction.begin();
        
        SaldoCliente saldoCliente = SaldoCliente.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (te.tipo.equals("c")) {
            saldoCliente.saldo += valor;
        } else {
            if (saldoCliente.saldo - valor < -saldoCliente.limite) {
                QuarkusTransaction.rollback();
                throw new WebApplicationException(422);
            }
            saldoCliente.saldo -= valor;
        }
        t.limite = saldoCliente.limite;
        t.saldo = saldoCliente.saldo;
        
        saldoCliente.persist();
        t.persist();
        QuarkusTransaction.commit();

        return new LimiteSaldo(saldoCliente.saldo, saldoCliente.limite);
    }

    @GET
    @Path("/{id}/extrato")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Extrato extrato(@PathParam("id") Integer id) {
        if (!existeCliente(id)) {
            throw new WebApplicationException(404);
        }
        Extrato extrato = new Extrato();
        extrato.saldo.data_extrato = LocalDateTime.now();
        extrato.ultimas_transacoes = Transacao.find("cliente_id = ?1 order by id desc", id).page(0, 10).list();
        if (extrato.ultimas_transacoes.size() > 0) {
            Transacao ultimaTransacao = extrato.ultimas_transacoes.get(0);
            extrato.saldo.total = ultimaTransacao.saldo;
            extrato.saldo.limite = ultimaTransacao.limite;
        } else {
            SaldoCliente saldoCliente = SaldoCliente.findById(id);
            extrato.saldo.total = saldoCliente.saldo;
            extrato.saldo.limite = saldoCliente.limite;
        }
        return extrato;
    }

    private boolean existeCliente(int id) {
        return id > 0 && id < 6;
        // return Cliente.findById(id) != null;
    }
}
