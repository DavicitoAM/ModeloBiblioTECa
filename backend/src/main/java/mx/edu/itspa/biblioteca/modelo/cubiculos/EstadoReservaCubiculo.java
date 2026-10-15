package mx.edu.itspa.biblioteca.modelo.cubiculos;

/** M05, PDF p. impresa 36: estados de la reserva del espacio. Los valores nombran estados; no implementan transiciones. */
public enum EstadoReservaCubiculo {
    CONFIRMADA, EN_USO, FINALIZADA, CANCELADA, VENCIDA, NO_PRESENTADA
}
