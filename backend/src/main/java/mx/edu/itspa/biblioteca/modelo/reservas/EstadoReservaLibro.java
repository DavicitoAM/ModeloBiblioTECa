package mx.edu.itspa.biblioteca.modelo.reservas;

/** M04, PDF p. impresa 29: estados de la reserva bibliografica. Los valores nombran estados; no implementan transiciones. */
public enum EstadoReservaLibro {
    PENDIENTE, EN_ESPERA, LISTA_PARA_RECOGER, ATENDIDA, CANCELADA, VENCIDA
}
