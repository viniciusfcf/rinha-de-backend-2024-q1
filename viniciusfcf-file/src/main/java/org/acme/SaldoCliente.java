package org.acme;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SaldoCliente {
    
    public Integer id;

    public int saldo;

    public int limite;
}
