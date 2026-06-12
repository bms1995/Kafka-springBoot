package com.example.orderservice.api;

import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public String createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = orderService.createOrder(request);
        return "Order created and queued for Kafka with ID: " + orderId;
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/{orderId}/events")
    public List<OrderEventResponse> getOrderEvents(@PathVariable String orderId) {
        return orderService.getOrderEvents(orderId);
    }
}
