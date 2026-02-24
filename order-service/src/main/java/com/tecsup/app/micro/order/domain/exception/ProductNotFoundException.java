package com.tecsup.app.micro.order.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un producto en el Product Service
 */
public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(Long productId) {
        super("Producto con id " + productId + " no encontrado");
    }
}
