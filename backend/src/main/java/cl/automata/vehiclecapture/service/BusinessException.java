package cl.automata.vehiclecapture.service;

/** Error de regla de negocio con el código HTTP que corresponde. */
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
