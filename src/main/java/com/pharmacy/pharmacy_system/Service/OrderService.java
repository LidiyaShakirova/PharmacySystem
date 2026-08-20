package com.pharmacy.pharmacy_system.Service;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.OrderItem;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.OrderItemRepository;
import com.pharmacy.pharmacy_system.Repository.OrderRepository;
import com.pharmacy.pharmacy_system.Util.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DrugService drugService;
    private final AppSettingService appSettingService;
    private final StockMovementService stockMovementService;

    private User getCurrentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private void checkPharmacist() {
        User current = getCurrentUser();
        if (current == null || !"PHARMACIST".equals(current.getRole())) {
            throw new SecurityException("Доступ запрещён. Только фармацевт может выполнять это действие.");
        }
    }

    private void checkCanModify(Order order) {
        User current = getCurrentUser();
        if (!"PHARMACIST".equals(current.getRole())) {
            throw new SecurityException("Доступ запрещён");
        }
        if (!order.getUser().getId().equals(current.getId())) {
            throw new SecurityException("Вы можете редактировать только свои заявки");
        }
        if (!"Черновик".equals(order.getStatus())) {
            throw new IllegalStateException("Можно редактировать только черновики");
        }
    }

    public Set<Long> getDrugIdsInActiveOrders() {
        List<Order> activeOrders = orderRepository.findByStatusIn(List.of("Черновик", "Согласована"));
        Set<Long> drugIds = new HashSet<>();

        for (Order order : activeOrders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                drugIds.add(item.getDrug().getId());
            }
        }
        return drugIds;
    }

    public Set<Long> getDrugIdsInCurrentUserDrafts() {
        User currentUser = getCurrentUser();
        List<Order> drafts = orderRepository.findByUserIdAndStatus(currentUser.getId(), "Черновик");
        Set<Long> drugIds = new HashSet<>();

        for (Order draft : drafts) {
            List<OrderItem> items = orderItemRepository.findByOrderId(draft.getId());
            for (OrderItem item : items) {
                drugIds.add(item.getDrug().getId());
            }
        }
        return drugIds;
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> findMyOrders() {
        User current = getCurrentUser();
        if ("ADMIN".equals(current.getRole())) {
            return findAll();
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(current.getId());
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Заявка с id " + id + " не найдена"));
    }

    public List<Order> findByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public static class OrderItemDto {
        private Long drugId;
        private Integer quantity;

        public OrderItemDto(Long drugId, Integer quantity) {
            this.drugId = drugId;
            this.quantity = quantity;
        }

        public Long getDrugId() {
            return drugId;
        }

        public void setDrugId(Long drugId) {
            this.drugId = drugId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    @Transactional
    public Order createManualOrder(List<OrderItemDto> items) {
        checkPharmacist();

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Заявка должна содержать хотя бы одну позицию");
        }

        Set<Long> drugIdsInActiveOrders = getDrugIdsInActiveOrders();
        for (OrderItemDto dto : items) {
            if (drugIdsInActiveOrders.contains(dto.getDrugId())) {
                Drug drug = drugService.findById(dto.getDrugId());
                throw new IllegalArgumentException("Препарат '" + drug.getName() + "' уже находится в другой активной заявке (Черновик или Согласована)");
            }
        }

        Order order = new Order();
        order.setUser(getCurrentUser());
        order.setStatus("Черновик");
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDto dto : items) {
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Количество должно быть положительным");
            }
            Drug drug = drugService.findById(dto.getDrugId());
            OrderItem item = new OrderItem();
            item.setDrug(drug);
            item.setQuantity(dto.getQuantity());
            item.setOrder(order);
            orderItems.add(item);
        }
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        log.info("Создана заявка №{} ({} позиций) фармацевтом {}", saved.getId(), orderItems.size(), saved.getUser().getUsername());
        return saved;
    }
    @Transactional
    public Order generateAutoOrder() {

        Set<Long> drugIdsInActiveOrders = getDrugIdsInActiveOrders();

        Set<Long> drugIdsInMyDrafts = getDrugIdsInCurrentUserDrafts();

        List<Drug> allDrugs = drugService.findAll();

        int targetWeeks = appSettingService.getTargetWeeks();
        int maxItems = appSettingService.getOrderLimit();

        List<OrderItemDto> itemsToOrder = new ArrayList<>();

        for (Drug drug : allDrugs) {
            if (drugIdsInActiveOrders.contains(drug.getId())) {
                log.debug("Препарат '{}' пропущен: уже в активной заявке", drug.getName());
                continue;
            }
            if (drugIdsInMyDrafts.contains(drug.getId())) {
                log.debug("Препарат '{}' пропущен: уже в вашем черновике", drug.getName());
                continue;
            }

            Integer minStock = drug.getCategory().getMinStock();
            if (minStock == null) minStock = 0;

            double target = drug.getWeeklySales() * targetWeeks;

            if (drug.getCurrentStock() < target || drug.getCurrentStock() < minStock) {
                int need = (int) Math.ceil(Math.max(target, minStock) - drug.getCurrentStock());
                itemsToOrder.add(new OrderItemDto(drug.getId(), need));
            }
        }

        if (itemsToOrder.isEmpty()) {
            log.info("Нет препаратов для автоматического заказа");
            return null;
        }

        itemsToOrder.sort(Comparator.comparingInt((OrderItemDto dto) -> {
            Drug d = drugService.findById(dto.getDrugId());
            return d.getCategory().getPriority();
        }).thenComparingInt(dto -> -dto.getQuantity()));
        if (itemsToOrder.size() > maxItems) {
            itemsToOrder = itemsToOrder.subList(0, maxItems);
        }
        Order order = createManualOrder(itemsToOrder);
        log.info("Автоматически создана заявка №{}", order.getId());
        return order;
    }

    @Transactional
    public Order updateStatus(Long orderId, String newStatus) {
        checkPharmacist();

        Order order = findById(orderId);
        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new SecurityException("Вы можете изменять статус только своих заявок");
        }
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Заявка №{} изменён статус на {}", orderId, newStatus);
        return updated;
    }

    @Transactional
    public Order updateDraftOrder(Long orderId, List<OrderItemDto> newItems) {
        checkPharmacist();

        Order order = findById(orderId);
        checkCanModify(order);

        if (newItems == null || newItems.isEmpty()) {
            throw new IllegalArgumentException("Заявка должна содержать хотя бы одну позицию");
        }

        Set<Long> drugIdsInOtherOrders = getDrugIdsInActiveOrders();
        for (OrderItem item : order.getItems()) {
            drugIdsInOtherOrders.remove(item.getDrug().getId());
        }

        for (OrderItemDto dto : newItems) {
            if (drugIdsInOtherOrders.contains(dto.getDrugId())) {
                Drug drug = drugService.findById(dto.getDrugId());
                throw new IllegalArgumentException("Препарат '" + drug.getName() + "' уже находится в другой активной заявке");
            }
        }
        orderItemRepository.deleteByOrderId(orderId);

        List<OrderItem> updatedItems = new ArrayList<>();
        for (OrderItemDto dto : newItems) {
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Количество должно быть положительным");
            }
            Drug drug = drugService.findById(dto.getDrugId());
            OrderItem item = new OrderItem();
            item.setDrug(drug);
            item.setQuantity(dto.getQuantity());
            item.setOrder(order);
            updatedItems.add(item);
        }

        order.setItems(updatedItems);
        Order saved = orderRepository.save(order);
        log.info("Обновлена заявка №{} фармацевтом {}", orderId, getCurrentUser().getUsername());
        return saved;
    }
    @Transactional
    public void deleteOrder(Long orderId) {
        checkPharmacist();

        Order order = findById(orderId);
        checkCanModify(order);

        orderRepository.delete(order);
        log.info("Заявка №{} удалена фармацевтом {}", orderId, getCurrentUser().getUsername());
    }

    public static class ReceiveItemDto {
        private Long drugId;
        private Integer receivedQuantity;

        public ReceiveItemDto() {
        }
        public Long getDrugId() {
            return drugId;
        }
        public void setDrugId(Long drugId) {
            this.drugId = drugId;
        }
        public Integer getReceivedQuantity() {
            return receivedQuantity;
        }

        public void setReceivedQuantity(Integer receivedQuantity) {
            this.receivedQuantity = receivedQuantity;
        }
    }
    @Transactional
    public void receiveOrder(Long orderId, List<ReceiveItemDto> receivedItems) {
        Order order = findById(orderId);

        if (!"Согласована".equals(order.getStatus())) {
            throw new IllegalStateException("Заявка должна быть в статусе: СОГЛАСОВАНА");
        }

        for (ReceiveItemDto item : receivedItems) {
            Drug drug = drugService.findById(item.getDrugId());

            stockMovementService.registerIncome(drug.getName(), item.getReceivedQuantity());
            OrderItem orderItem = orderItemRepository.findByOrderIdAndDrugId(orderId, drug.getId()).orElseThrow(() -> new IllegalArgumentException("Позиция не найдена"));

            int newReceived = (orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : 0) + item.getReceivedQuantity();
            orderItem.setReceivedQuantity(newReceived);
            orderItemRepository.save(orderItem);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        boolean allReceived = items.stream().allMatch(item -> item.getReceivedQuantity() != null && item.getReceivedQuantity() >= item.getQuantity());

        if (allReceived) {
            order.setStatus("Закрыта");
            orderRepository.save(order);
            log.info("Заявка №{} автоматически закрыта - все препараты поступили", orderId);
        }
    }
}