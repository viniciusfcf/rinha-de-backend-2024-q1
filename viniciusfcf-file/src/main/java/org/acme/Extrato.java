package org.acme;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Extrato {

    public Saldo saldo;

    public List<Transacao> ultimas_transacoes;
    
}