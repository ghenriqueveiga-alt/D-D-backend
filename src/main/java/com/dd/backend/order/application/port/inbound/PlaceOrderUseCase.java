package com.dd.backend.order.application.port.inbound;

import com.dd.backend.order.domain.model.Order;

/** Porta de entrada: registro de um pedido de venda. */
public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);
}
