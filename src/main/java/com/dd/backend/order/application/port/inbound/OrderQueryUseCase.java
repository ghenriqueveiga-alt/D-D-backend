package com.dd.backend.order.application.port.inbound;

import com.dd.backend.order.domain.model.Order;
import java.util.List;

/** Porta de entrada: consultas de pedidos. */
public interface OrderQueryUseCase {

    Order get(Long orderId);

    List<Order> list();

    List<Order> listByCustomer(Long customerId);
}
