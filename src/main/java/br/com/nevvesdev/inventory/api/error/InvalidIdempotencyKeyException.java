package br.com.nevvesdev.inventory.api.error;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("A chave de idempotência deve ter de 8 a 80 caracteres: letras, números, '.', '_', ':' ou '-'");
    }
}