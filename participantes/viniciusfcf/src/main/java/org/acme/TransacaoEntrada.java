package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransacaoEntrada {

    public String valor;

    @JsonIgnore
    public Integer cliente_id;

    public Character tipo;

    public String descricao;

    public boolean ehValida() {
        return valor != null
                && tipo != null
                && descricao != null
                && valorEhInteger()
                && Integer.parseInt(valor) > 0
                && (tipo.charValue() == 'c' || tipo.charValue() == 'd')
                && descricao.length() > 0
                && descricao.length() <= 10;
    }

    private boolean valorEhInteger() {
        try {
            Integer.parseInt(valor);
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }

}
