package com.tecsup.app.micro.order.application.usecase;

import com.tecsup.app.micro.order.domain.exception.OrderNotFoundException;
import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: Obtener orden por ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrderByIdUseCase {
    
    private final OrderRepository orderRepository;
    
    public Order execute(Long id) {
        log.debug("Ejecutando GetOrderByIdUseCase para id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}

