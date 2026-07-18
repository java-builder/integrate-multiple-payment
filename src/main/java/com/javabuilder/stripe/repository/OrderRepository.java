package com.javabuilder.stripe.repository;

import com.javabuilder.stripe.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.product"})
    Optional<Order> findOrderWithOrderDetailsByOrderCode(String orderCode);
}
