package com.dd.backend.order.application.port.inbound;

import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.PaymentMethod;

/** Porta de entrada: ciclo de vida do pedido (pagamento, envio, entrega, cancelamento). */
public interface OrderLifecycleUseCase {

    Order pay(Long orderId, PaymentMethod method);

    Order ship(Long orderId);

    Order deliver(Long orderId);

    Order cancel(Long orderId);
}
