package org.acme;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Transacao {

    public Integer id;

    public Integer valor;

    public Integer cliente_id;

    public Character tipo;

    public String descricao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
    public LocalDateTime realizada_em;

    @JsonIgnore
    public int saldo;

    @JsonIgnore
    public int limite;

    public static Transacao of(TransacaoEntrada te) {
        Transacao t = new Transacao();
        t.tipo = te.tipo;
        t.cliente_id = te.cliente_id;
        t.descricao = te.descricao;
        t.realizada_em = LocalDateTime.now();
        t.valor = Integer.valueOf(te.valor);
        return t;
    }

}
