package cl.automata.vehiclecapture.config;

import org.springframework.stereotype.Component;

/**
 * Stub de usuario actual. Aún no hay autenticación (Fase 1 real: Spring Security + JWT).
 * Todo el código ya pasa por aquí para que la trazabilidad quede lista cuando exista login.
 */
@Component
public class CurrentUser {
    public String name() {
        return "demo";
    }
}
