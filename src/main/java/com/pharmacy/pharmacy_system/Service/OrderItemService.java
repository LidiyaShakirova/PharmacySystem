package com.pharmacy.pharmacy_system.Service;


import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.OrderItem;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.OrderItemRepository;
import com.pharmacy.pharmacy_system.Util.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
    public class OrderItemService {

        private final OrderItemRepository orderItemRepository;

        private User getCurrentUser() {
            return UserSession.getInstance().getCurrentUser();
        }

        private void checkCanModify(Order order) {
            User current = getCurrentUser();

            if (!"ADMIN".equals(current.getRole()) && !order.getUser().getId().equals(current.getId())) {
                throw new SecurityException("Нет прав на изменение этой заявки");
            }
            if (!"DRAFT".equals(order.getStatus())) {
                throw new IllegalStateException("Можно изменять только черновики заявок");
            }
        }
        public List<OrderItem> findByOrderId(Long orderId) {
            return orderItemRepository.findByOrderId(orderId);
        }

        public OrderItem findById(Long id) {
            return orderItemRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Позиция с id " + id + " не найдена"));
        }

        @Transactional
        public OrderItem updateQuantity(Long orderItemId, Integer newQuantity) {
            if (newQuantity == null || newQuantity <= 0) {
                throw new IllegalArgumentException("Количество должно быть положительным");
            }

            OrderItem item = findById(orderItemId);
            checkCanModify(item.getOrder()); // проверка прав и статуса

            item.setQuantity(newQuantity);
            OrderItem updated = orderItemRepository.save(item);
            log.info("Пользователь {} изменил количество в позиции {} на {}",
                    getCurrentUser().getUsername(), orderItemId, newQuantity);
            return updated;
        }

        @Transactional
        public void deleteOrderItem(Long orderItemId) {
            OrderItem item = findById(orderItemId);
            checkCanModify(item.getOrder());

            orderItemRepository.delete(item);
            log.info("Пользователь {} удалил позицию {} из заявки {}",
                    getCurrentUser().getUsername(), orderItemId, item.getOrder().getId());

    }
}
