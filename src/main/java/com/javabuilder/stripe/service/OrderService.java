package com.javabuilder.stripe.service;

import com.javabuilder.stripe.common.OrderStatus;
import com.javabuilder.stripe.dto.request.CreateOrderRequest;
import com.javabuilder.stripe.dto.request.OrderItemRequest;
import com.javabuilder.stripe.dto.response.OrderResponse;
import com.javabuilder.stripe.entity.Order;
import com.javabuilder.stripe.entity.OrderDetail;
import com.javabuilder.stripe.entity.Product;
import com.javabuilder.stripe.exception.AppException;
import com.javabuilder.stripe.exception.ErrorCode;
import com.javabuilder.stripe.mapper.OrderMapper;
import com.javabuilder.stripe.repository.OrderRepository;
import com.javabuilder.stripe.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-SERVICE")
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Generate order code
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. Create Order
        Order order = Order.builder()
                .orderCode(orderCode)
                .orderStatus(OrderStatus.PENDING)
                .currency(request.currency())
                .totalPrice(BigDecimal.ZERO)
                .orderDetails(new ArrayList<>())
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        // 3. Process items, check stock, and associate details
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getQuantity() < itemRequest.quantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            // Deduct stock
            product.setQuantity(product.getQuantity() - itemRequest.quantity());
            productRepository.save(product);

            // Create OrderDetail
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .price(product.getPrice())
                    .quantity(itemRequest.quantity())
                    .build();

            order.getOrderDetails().add(orderDetail);

            // Accumulate total price
            BigDecimal itemCost = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalPrice = totalPrice.add(itemCost);
        }
        order.setTotalPrice(totalPrice);

        // 4. Save order (cascades saving to orderDetails)
        order = orderRepository.save(order);

        log.info("Order created successfully with code: {}", orderCode);
        return orderMapper.toOrderResponse(order);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        return orderMapper.toOrderResponse(order);
    }

    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + orderCode));
        return orderMapper.toOrderResponse(order);
    }

}
