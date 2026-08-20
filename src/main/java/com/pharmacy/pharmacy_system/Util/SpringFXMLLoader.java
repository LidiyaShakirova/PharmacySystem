package com.pharmacy.pharmacy_system.Util;

import javafx.util.Callback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Утилита для связи Spring и JavaFX.
 * Позволяет FXMLLoader создавать контроллеры как Spring-бины.
 */
@Component
public class SpringFXMLLoader {

    private static ApplicationContext context;

    /**
     * Устанавливает Spring-контекст (вызывается при старте приложения).
     */
    public static void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }

    /**
     * Возвращает фабрику контроллеров для FXMLLoader.
     * Фабрика извлекает бин нужного типа из Spring-контекста.
     */
    public static Callback<Class<?>, Object> getControllerFactory() {
        return param -> {
            if (context == null) {
                throw new IllegalStateException("Spring-контекст не инициализирован");
            }
            return context.getBean(param);
        };
    }
}