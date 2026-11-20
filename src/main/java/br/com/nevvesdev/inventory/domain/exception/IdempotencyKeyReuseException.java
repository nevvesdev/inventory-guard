package br.com.nevvesdev.inventory.domain.exception;

public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String key) {
        super("A chave de idempotência já foi usada com outro conteúdo: " + key);
    }
}