package com.sajitar.backend.util;

public interface Viewer {

    /**
     * Visibilidade Pública: qualquer usuário pode ler.
     */
    interface Public {}

    /**
     * Visibilidade Protegida: um número restrito de usuários podem ler.
     */
    interface Protected extends Public {}

    /**
     * Visibilidade Privada: nenhum usuário pode ler.
     */
    interface Private extends Protected {}

}
