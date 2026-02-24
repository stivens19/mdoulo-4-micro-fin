package com.tecsup.app.micro.order.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra una orden
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(Long orderId) {
        super("Orden con ID " + orderId + " no encontrada");
    }
    
    public OrderNotFoundException(String orderNumber) {
        super("Orden con número " + orderNumber + " no encontrada");
    }
}
